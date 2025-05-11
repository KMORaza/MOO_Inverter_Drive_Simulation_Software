package inverter_drive.simulation.software;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.stream.IntStream;

public class Optimizer {
    private final InverterPowerStage inverter;
    private final InductionMotor motor;
    private final FaultSimulator faultSimulator;
    private final SensorModel sensors;
    private final VfController controller;
    private final DataLogger dataLogger;
    private final Random random = new Random();
    private final int populationSize = 50;
    private final int generations = 100;
    private final double mutationRate = 0.1;
    private final double crossoverRate = 0.9;
    /// Parameter bounds
    private final double[] pwmFreqBounds = {2000, 20000}; // Hz
    private final double[] modIndexBounds = {0.1, 1.0};
    private final double[] fanSpeedBounds = {0.0, 1.0};
    private final double[] coolantFlowBounds = {0.0, 10.0}; // L/min

    public Optimizer(InverterPowerStage inverter, InductionMotor motor, FaultSimulator faultSimulator,
                     SensorModel sensors, VfController controller, DataLogger dataLogger) {
        this.inverter = inverter;
        this.motor = motor;
        this.faultSimulator = faultSimulator;
        this.sensors = sensors;
        this.controller = controller;
        this.dataLogger = dataLogger;
    }

    public static class Individual {
        double[] parameters; // [pwmFreq, modIndex, fanSpeed, coolantFlow]
        double[] objectives; // [powerLoss, maxTemp, faultImpact]
        int rank;
        double crowdingDistance;
        Individual(double[] parameters) {
            this.parameters = parameters;
            this.objectives = new double[3];
        }
    }

    public Individual[] optimize() {
        Individual[] population = initializePopulation();
        for (int gen = 0; gen < generations; gen++) {
            evaluatePopulation(population);
            Individual[] offspring = generateOffspring(population);
            Individual[] combined = new Individual[populationSize * 2];
            System.arraycopy(population, 0, combined, 0, populationSize);
            System.arraycopy(offspring, 0, combined, populationSize, populationSize);
            assignRanksAndCrowding(combined);
            population = selectNextPopulation(combined);
        }
        evaluatePopulation(population);
        assignRanksAndCrowding(population);
        return Arrays.stream(population)
                .filter(ind -> ind.rank == 1)
                .sorted(Comparator.comparingDouble(ind -> ind.objectives[0])) // Sort by power loss
                .toArray(Individual[]::new);
    }

    private Individual[] initializePopulation() {
        Individual[] population = new Individual[populationSize];
        for (int i = 0; i < populationSize; i++) {
            double[] params = new double[]{
                    pwmFreqBounds[0] + random.nextDouble() * (pwmFreqBounds[1] - pwmFreqBounds[0]),
                    modIndexBounds[0] + random.nextDouble() * (modIndexBounds[1] - modIndexBounds[0]),
                    fanSpeedBounds[0] + random.nextDouble() * (fanSpeedBounds[1] - fanSpeedBounds[0]),
                    coolantFlowBounds[0] + random.nextDouble() * (coolantFlowBounds[1] - coolantFlowBounds[0])
            };
            population[i] = new Individual(params);
        }
        return population;
    }

    private void evaluatePopulation(Individual[] population) {
        for (Individual ind : population) {
            double pwmFreq = ind.parameters[0];
            double modIndex = ind.parameters[1];
            double fanSpeed = ind.parameters[2];
            double coolantFlow = ind.parameters[3];
            inverter.setPwmFrequency(pwmFreq);
            inverter.setModulationIndex(modIndex);
            inverter.setCooling(fanSpeed, coolantFlow);
            motor.setParameters(
                    motor.getRatedVoltage(), motor.getRatedPower(), (int) motor.getPolePairs(),
                    motor.getResistance(), motor.getInductance(), "Constant",
                    0.1, 0.01, 0.05, 0.01, 0.005, 5000,
                    fanSpeed, coolantFlow
            );
            double simulationTime = 0.0;
            double[] phaseVoltages = new double[3];
            double[] phaseCurrents = new double[3];
            double powerLoss = 0.0;
            double maxTemp = 25.0;
            double faultImpact = 0.0;
            int steps = (int) (1.0 / Config.SIMULATION_TIME_STEP);
            faultSimulator.injectFault("Overcurrent");
            for (int i = 0; i < steps; i++) {
                double[] pwmSignals = controller.updateControl(
                        "V/f", 100.0, 50.0, 1.0, 10.0, 1.0, simulationTime
                );
                phaseVoltages = inverter.generatePhaseVoltages(pwmSignals, "SVPWM");
                phaseVoltages = faultSimulator.applyFaults(
                        phaseVoltages, true, motor.getTemperature(), inverter.getTemperature(),
                        150.0, "Warning"
                );
                phaseCurrents = sensors.measureCurrents(phaseVoltages, motor);
                motor.updateState(phaseVoltages, phaseCurrents, "Constant", Config.SIMULATION_TIME_STEP);
                /// Calculate power loss (switching + conduction)
                double switchingLoss = pwmFreq * 0.0001 * inverter.getDcLinkVoltage();
                double conductionLoss = Arrays.stream(phaseCurrents)
                        .map(current -> current * current * motor.getResistance())
                        .sum();
                powerLoss += (switchingLoss + conductionLoss) * Config.SIMULATION_TIME_STEP;
                /// Track maximum temperature
                maxTemp = Math.max(maxTemp, Math.max(motor.getTemperature(), inverter.getTemperature()));
                /// Measure fault impact (e.g., deviation from expected speed)
                faultImpact += Math.abs(motor.getSpeed() - 100.0) * Config.SIMULATION_TIME_STEP;
                simulationTime += Config.SIMULATION_TIME_STEP;
            }
            /// Normalize objectives
            ind.objectives[0] = powerLoss / steps; // Average power loss
            ind.objectives[1] = maxTemp; // Maximum temperature
            ind.objectives[2] = faultImpact / steps; // Average fault impact
            faultSimulator.clearFault();
        }
    }

    private Individual[] generateOffspring(Individual[] population) {
        Individual[] offspring = new Individual[populationSize];
        for (int i = 0; i < populationSize; i += 2) {
            Individual parent1 = tournamentSelection(population);
            Individual parent2 = tournamentSelection(population);
            Individual[] children = crossover(parent1, parent2);
            offspring[i] = mutate(children[0]);
            if (i + 1 < populationSize) {
                offspring[i + 1] = mutate(children[1]);
            }
        }
        return offspring;
    }

    private Individual tournamentSelection(Individual[] population) {
        Individual candidate1 = population[random.nextInt(populationSize)];
        Individual candidate2 = population[random.nextInt(populationSize)];
        return candidate1.rank < candidate2.rank ||
                (candidate1.rank == candidate2.rank && candidate1.crowdingDistance > candidate2.crowdingDistance)
                ? candidate1 : candidate2;
    }

    private Individual[] crossover(Individual parent1, Individual parent2) {
        if (random.nextDouble() > crossoverRate) {
            return new Individual[]{new Individual(parent1.parameters.clone()),
                    new Individual(parent2.parameters.clone())};
        }
        double[] child1Params = new double[4];
        double[] child2Params = new double[4];
        for (int i = 0; i < 4; i++) {
            double alpha = random.nextDouble();
            child1Params[i] = alpha * parent1.parameters[i] + (1 - alpha) * parent2.parameters[i];
            child2Params[i] = alpha * parent2.parameters[i] + (1 - alpha) * parent1.parameters[i];
            /// Ensure bounds
            child1Params[i] = Math.max(getBounds(i)[0], Math.min(getBounds(i)[1], child1Params[i]));
            child2Params[i] = Math.max(getBounds(i)[0], Math.min(getBounds(i)[1], child2Params[i]));
        }
        return new Individual[]{new Individual(child1Params), new Individual(child2Params)};
    }

    private Individual mutate(Individual individual) {
        double[] params = individual.parameters.clone();
        for (int i = 0; i < params.length; i++) {
            if (random.nextDouble() < mutationRate) {
                double[] bounds = getBounds(i);
                params[i] += random.nextGaussian() * (bounds[1] - bounds[0]) * 0.1;
                params[i] = Math.max(bounds[0], Math.min(bounds[1], params[i]));
            }
        }
        return new Individual(params);
    }

    private double[] getBounds(int index) {
        switch (index) {
            case 0: return pwmFreqBounds;
            case 1: return modIndexBounds;
            case 2: return fanSpeedBounds;
            case 3: return coolantFlowBounds;
            default: throw new IllegalArgumentException("Invalid parameter index");
        }
    }

    private void assignRanksAndCrowding(Individual[] population) {
        // Non-dominated sorting
        int[] dominationCount = new int[population.length];
        int[][] dominatedBy = new int[population.length][population.length];
        int[] dominatedCount = new int[population.length];
        int frontIndex = 0;
        int[] rank = new int[population.length];
        boolean[] assigned = new boolean[population.length];

        for (int i = 0; i < population.length; i++) {
            for (int j = 0; j < population.length; j++) {
                if (i == j) continue;
                if (dominates(population[i], population[j])) {
                    dominatedBy[i][dominatedCount[i]++] = j;
                } else if (dominates(population[j], population[i])) {
                    dominationCount[i]++;
                }
            }
            if (dominationCount[i] == 0) {
                rank[i] = 1;
                assigned[i] = true;
            }
        }
        while (IntStream.range(0, population.length).anyMatch(i -> !assigned[i])) {
            frontIndex++;
            for (int i = 0; i < population.length; i++) {
                if (rank[i] != frontIndex) continue;
                for (int j = 0; j < dominatedCount[i]; j++) {
                    int dominated = dominatedBy[i][j];
                    dominationCount[dominated]--;
                    if (dominationCount[dominated] == 0) {
                        rank[dominated] = frontIndex + 1;
                        assigned[dominated] = true;
                    }
                }
            }
        }
        for (int i = 0; i < population.length; i++) {
            population[i].rank = rank[i];
        }
        /// Crowding distance
        for (Individual ind : population) {
            ind.crowdingDistance = 0.0;
        }
        for (int obj = 0; obj < 3; obj++) {
            int finalObj = obj;
            Arrays.sort(population, Comparator.comparingDouble(ind -> ind.objectives[finalObj]));
            population[0].crowdingDistance = Double.POSITIVE_INFINITY;
            population[population.length - 1].crowdingDistance = Double.POSITIVE_INFINITY;
            double objRange = population[population.length - 1].objectives[obj] - population[0].objectives[obj];
            if (objRange == 0) continue;
            for (int i = 1; i < population.length - 1; i++) {
                population[i].crowdingDistance += (population[i + 1].objectives[obj] - population[i - 1].objectives[obj]) / objRange;
            }
        }
    }

    private boolean dominates(Individual ind1, Individual ind2) {
        boolean betterInAny = false;
        for (int i = 0; i < ind1.objectives.length; i++) {
            if (ind1.objectives[i] > ind2.objectives[i]) {
                return false;
            }
            if (ind1.objectives[i] < ind2.objectives[i]) {
                betterInAny = true;
            }
        }
        return betterInAny;
    }

    private Individual[] selectNextPopulation(Individual[] combined) {
        Arrays.sort(combined, (a, b) -> {
            if (a.rank != b.rank) return Integer.compare(a.rank, b.rank);
            return Double.compare(b.crowdingDistance, a.crowdingDistance);
        });
        Individual[] nextPopulation = new Individual[populationSize];
        System.arraycopy(combined, 0, nextPopulation, 0, populationSize);
        return nextPopulation;
    }
}
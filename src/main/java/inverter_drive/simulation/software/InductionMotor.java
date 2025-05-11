package inverter_drive.simulation.software;

public class InductionMotor {
    private double speed = 0.0;
    private double torque = 0.0;
    private double rotorFlux = 1.0;
    private double ratedVoltage = 230.0;
    private double ratedPower = 5.0;
    private double polePairs = 2;
    private double resistance = 0.5;
    private double inductance = 0.01;
    private String loadType = "Constant";
    private double loadInertia = 0.1;
    private double damping = 0.01;
    private double shaftInertia = 0.05;
    private double friction = 0.01;
    private double tempCoefficient = 0.005;
    private double couplingStiffness = 5000;
    private double temperature = 25.0; // °C
    private double fanSpeed = 0.5; // 0–1
    private double coolantFlow = 5.0; // L/min
    private double id = 0.0; // Direct-axis current
    private double iq = 0.0; // Quadrature-axis current

    public void setParameters(double ratedVoltage, double ratedPower, int polePairs,
                              double resistance, double inductance, String loadType,
                              double loadInertia, double damping, double shaftInertia,
                              double friction, double tempCoefficient, double couplingStiffness,
                              double fanSpeed, double coolantFlow) {
        this.ratedVoltage = ratedVoltage;
        this.ratedPower = ratedPower;
        this.polePairs = polePairs;
        this.resistance = resistance;
        this.inductance = inductance;
        this.loadType = loadType;
        this.loadInertia = loadInertia;
        this.damping = damping;
        this.shaftInertia = shaftInertia;
        this.friction = friction;
        this.tempCoefficient = tempCoefficient;
        this.couplingStiffness = couplingStiffness;
        this.fanSpeed = fanSpeed;
        this.coolantFlow = coolantFlow;
    }

    public void updateState(double[] phaseVoltages, double[] phaseCurrents, String loadType, double timeStep) {
        updateTemperature(phaseCurrents);
        double effectiveResistance = resistance * (1 + tempCoefficient * (temperature - Config.AMBIENT_TEMPERATURE));
        double vq = (2.0 / 3.0) * (phaseVoltages[0] - 0.5 * (phaseVoltages[1] + phaseVoltages[2]));
        double vd = (1.0 / Math.sqrt(3)) * (phaseVoltages[1] - phaseVoltages[2]);
        double iq = (2.0 / 3.0) * (phaseCurrents[0] - 0.5 * (phaseCurrents[1] + phaseCurrents[2]));
        double id = (1.0 / Math.sqrt(3)) * (phaseCurrents[1] - phaseCurrents[2]);
        this.iq = iq;
        this.id = id;
        torque = 1.5 * polePairs * rotorFlux * iq;
        double slip = (ratedVoltage / (2 * Math.PI * 50.0) - speed / polePairs) / (ratedVoltage / (2 * Math.PI * 50.0));
        rotorFlux += timeStep * (-rotorFlux / inductance + id);
        double loadTorque = 10.0;
        if (loadType.equals("Fan/Pump")) {
            loadTorque = 0.1 * speed * speed;
        } else if (loadType.equals("Inertia")) {
            loadTorque = 0.0;
        }
        double totalInertia = loadInertia + shaftInertia;
        double couplingEffect = couplingStiffness * speed * timeStep;
        double acceleration = (torque - loadTorque - (damping + friction) * speed - couplingEffect) / totalInertia;
        speed += acceleration * timeStep;
        if (speed < 0) speed = 0;
    }

    private void updateTemperature(double[] phaseCurrents) {
        /// Heat generation from I²R losses
        double iSquaredR = (phaseCurrents[0] * phaseCurrents[0] +
                phaseCurrents[1] * phaseCurrents[1] +
                phaseCurrents[2] * phaseCurrents[2]) * resistance;
        double heatGeneration = iSquaredR * Config.MOTOR_THERMAL_RESISTANCE;
        /// Convection: Q_conv = h * A * (T - T_amb)
        double h = Config.MOTOR_CONVECTION_BASE + Config.MOTOR_CONVECTION_FAN_COEFF * fanSpeed
                + Config.MOTOR_CONVECTION_COOLANT_COEFF * coolantFlow;
        double T_K = temperature + 273.15; // Convert to Kelvin
        double T_amb_K = Config.AMBIENT_TEMPERATURE + 273.15;
        double Q_conv = h * Config.MOTOR_SURFACE_AREA * (temperature - Config.AMBIENT_TEMPERATURE);
        /// Radiation: Q_rad = ε * σ * A * (T^4 - T_amb^4)
        double Q_rad = Config.MOTOR_EMISSIVITY * Config.STEFAN_BOLTZMANN * Config.MOTOR_SURFACE_AREA
                * (Math.pow(T_K, 4) - Math.pow(T_amb_K, 4));
        /// Total heat balance: dT/dt = (Q_gen - Q_conv - Q_rad) / C
        double coolingEffect = (Q_conv + Q_rad) / Config.MOTOR_THERMAL_CAPACITANCE;
        temperature += (heatGeneration - coolingEffect) * Config.SIMULATION_TIME_STEP;
        if (temperature < Config.AMBIENT_TEMPERATURE) temperature = Config.AMBIENT_TEMPERATURE;
    }

    public double getSpeed() {
        return speed;
    }

    public double getTorque() {
        return torque;
    }

    public double getResistance() {
        return resistance;
    }

    public double getInductance() {
        return inductance;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getRotorFlux() {
        return rotorFlux;
    }

    public double getId() {
        return id;
    }

    public double getIq() {
        return iq;
    }

    public double getRatedVoltage() {
        return ratedVoltage;
    }

    public double getRatedPower() {
        return ratedPower;
    }

    public double getPolePairs() {
        return polePairs;
    }
}
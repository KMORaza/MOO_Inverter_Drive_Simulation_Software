## Inverter Drive Simulation Software

_Diese Desktop-Software simuliert einen elektrischen Wechselrichterantrieb und ist geschrieben in JavaFX. Diese Software ist eine verbesserte Version der Software, die ich früher geschrieben habe. Ich habe die frühere Version verbessert, indem ich eine Mehrziel-Optimierungstechnik eingebaut habe. Ich habe den Nicht-dominanten Genetischen Sortieralgorithmus II (NSGA-II) verwendet._

_This desktop software simulates an electrical inverter drive and is written in JavaFX. This software is an enhanced version of the software which I wrote earlier. I enhanced the earlier version by incorporating multi-objective optimization technique. I used Non-dominated Sorting Genetic Algorithm II (NSGA-II)._

The NSGA-II-based multi-objective optimization successfully balances power loss, temperature, and fault resilience

### Major components
* Configuration - Defines constants for simulation parameters, such as time step, thermal properties, convection coefficients, and fault simulation settings. These constants are used across the system to ensure consistency.
* Data logging - Logs simulation data (time, voltages, currents, speed, torque, control mode, and faults) to a CSV file.
* Fault simulation - Simulates faults like overcurrent, undervoltage, phase loss, overheat, and IGBT failure. It modifies phase voltages based on fault conditions and supports auto-reset after a 2-second delay.
* Induction motor model - Models an induction motor with parameters like rated voltage, power, resistance, and thermal characteristics. It updates motor state (speed, torque, temperature) based on input voltages, currents, and load type, incorporating thermal dynamics and cooling effects.
* Power stage - Simulates the inverter's power stage, generating phase voltages from PWM signals using SPWM or SVPWM techniques. It accounts for DC-link voltage, PWM frequency, dead time, modulation index, harmonic injection, overmodulation, and thermal behavior.
* Sensor model - Models current sensors with Gaussian noise and supports fault simulation (complete or partial failure) to mimic real-world sensor inaccuracies.
* V/f control system
  * V/f (Voltage/Frequency): Maintains a constant voltage-to-frequency ratio with PI control for speed regulation.
  * FOC (Field-Oriented Control): Uses PI controllers for torque and flux, transforming d-q voltages to three-phase signals.
  * DTC (Direct Torque Control): Applies hysteresis-based control using a switching table to select voltage vectors.
* Waveform visualization - Visualizes phase voltages, currents, and motor speed on a JavaFX canvas, with dynamic scaling and color-coded waveforms for real-time monitoring.
* Optimization - Implements Non-dominated Sorting Genetic Algorithm II (NSGA-II), a multi-objective genetic algorithm to optimize inverter and motor parameters.

### Multi-Objective Optimization 
* Combines switching losses (proportional to PWM frequency and DC-link voltage) and conduction losses (I²R losses in the motor).
* Tracks the highest temperature of the motor or inverter to ensure thermal stability.
* Measures the deviation of motor speed from the reference (100 rad/s) under an overcurrent fault, reflecting system robustness.
* NSGA-II optimization logic
  * Population: 50 individuals, each representing a set of the four parameters.
  * Crossover: Blended crossover with a 90% probability, combining parent parameters.
  * Mutation: Gaussian mutation with a 10% probability, perturbing parameters within bounds.
  * Selection: Tournament selection based on rank and crowding distance.
  * Non-Dominated Sorting: Assigns ranks to individuals based on dominance (an individual dominates another if it is better in at least one objective and not worse in any).
  * Crowding Distance: Ensures diversity by favoring individuals in less crowded regions of the Pareto front.
  * Initialization: Randomly generates a population within parameter bounds.
  * Evaluation: Simulates each individual for 1 second (10,000 steps at 0.0001s time step) with an overcurrent fault. Computes average power loss, maximum temperature, and average fault impact.
  * Evolution: Generates offspring via crossover and mutation, combines with the parent population, and selects the top 50 individuals based on rank and crowding distance.
  * Outcome: Returns the Pareto front (rank 1 individuals), sorted by power loss.
 
---

![](https://github.com/KMORaza/MOO_Inverter_Drive_Simulation_Software/blob/main/src/main/screenshot.png)

---

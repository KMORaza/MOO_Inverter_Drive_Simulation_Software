package inverter_drive.simulation.software;

public class InverterPowerStage {
    private double dcLinkVoltage;
    private double pwmFrequency;
    private double deadTime;
    private double modulationIndex;
    private boolean harmonicInjection;
    private boolean overmodulation;
    private double temperature = 25.0; // °C
    private double fanSpeed = 0.5; // 0–1
    private double coolantFlow = 5.0; // L/min

    public InverterPowerStage(double dcLinkVoltage, double pwmFrequency, double deadTime,
                              double modulationIndex, boolean harmonicInjection, boolean overmodulation) {
        this.dcLinkVoltage = dcLinkVoltage;
        this.pwmFrequency = pwmFrequency;
        this.deadTime = deadTime;
        this.modulationIndex = modulationIndex;
        this.harmonicInjection = harmonicInjection;
        this.overmodulation = overmodulation;
    }

    public double[] generatePhaseVoltages(double[] pwmSignals, String pwmType) {
        double[] phaseVoltages = new double[3];
        double deadTimeFactor = 1.0 - deadTime * pwmFrequency;
        double modFactor = modulationIndex * (overmodulation ? 1.15 : 1.0);
        if (pwmType.equals("SVPWM")) { /// Space Vector PWM
            double[] vRef = new double[3];
            for (int i = 0; i < 3; i++) {
                vRef[i] = 2.0 * pwmSignals[i] - 1.0;
            }
            double vAlpha = (2.0 / 3.0) * (vRef[0] - 0.5 * (vRef[1] + vRef[2]));
            double vBeta = (1.0 / Math.sqrt(3)) * (vRef[1] - vRef[2]);
            double vMag = Math.sqrt(vAlpha * vAlpha + vBeta * vBeta);
            double theta = Math.atan2(vBeta, vAlpha);
            int sector = (int) Math.floor(theta / (Math.PI / 3.0)) % 6;
            if (sector < 0) sector += 6;
            double sectorAngle = theta - sector * Math.PI / 3.0;
            double m = vMag * Math.sqrt(3) / dcLinkVoltage;
            double T = 1.0 / pwmFrequency;
            double T1 = m * T * Math.sin(Math.PI / 3.0 - sectorAngle);
            double T2 = m * T * Math.sin(sectorAngle);
            double T0 = T - T1 - T2;
            double[] duties = new double[3];
            switch (sector) {
                case 0:
                    duties[0] = (T1 + T2 + T0 / 2) / T;
                    duties[1] = (T2 + T0 / 2) / T;
                    duties[2] = T0 / (2 * T);
                    break;
                case 1:
                    duties[0] = (T1 + T0 / 2) / T;
                    duties[1] = (T1 + T2 + T0 / 2) / T;
                    duties[2] = T0 / (2 * T);
                    break;
                case 2:
                    duties[0] = T0 / (2 * T);
                    duties[1] = (T1 + T2 + T0 / 2) / T;
                    duties[2] = (T2 + T0 / 2) / T;
                    break;
                case 3:
                    duties[0] = T0 / (2 * T);
                    duties[1] = (T1 + T0 / 2) / T;
                    duties[2] = (T1 + T2 + T0 / 2) / T;
                    break;
                case 4:
                    duties[0] = (T2 + T0 / 2) / T;
                    duties[1] = T0 / (2 * T);
                    duties[2] = (T1 + T2 + T0 / 2) / T;
                    break;
                case 5:
                    duties[0] = (T1 + T2 + T0 / 2) / T;
                    duties[1] = T0 / (2 * T);
                    duties[2] = (T1 + T0 / 2) / T;
                    break;
            }
            for (int i = 0; i < 3; i++) {
                double signal = duties[i];
                if (harmonicInjection) {
                    signal += 0.1 * Math.sin(3 * Math.PI * pwmFrequency * System.currentTimeMillis() / 1000.0);
                }
                phaseVoltages[i] = signal * dcLinkVoltage * deadTimeFactor * modFactor;
            }
        } else {
            for (int i = 0; i < 3; i++) {
                double signal = pwmSignals[i];
                if (harmonicInjection) {
                    signal += 0.1 * Math.sin(3 * Math.PI * pwmFrequency * System.currentTimeMillis() / 1000.0);
                }
                phaseVoltages[i] = signal * dcLinkVoltage * deadTimeFactor * modFactor;
            }
        }

        updateTemperature(phaseVoltages);
        return phaseVoltages;
    }

    private void updateTemperature(double[] phaseVoltages) {
        /// Heat generation from switching losses
        double switchingLosses = pwmFrequency * 0.0001 * dcLinkVoltage;
        double heatGeneration = switchingLosses * Config.INVERTER_THERMAL_RESISTANCE;
        /// Convection: Q_conv = h * A * (T - T_amb)
        double h = Config.INVERTER_CONVECTION_BASE + Config.INVERTER_CONVECTION_FAN_COEFF * fanSpeed
                + Config.INVERTER_CONVECTION_COOLANT_COEFF * coolantFlow;
        double T_K = temperature + 273.15;
        double T_amb_K = Config.AMBIENT_TEMPERATURE + 273.15;
        double Q_conv = h * Config.INVERTER_SURFACE_AREA * (temperature - Config.AMBIENT_TEMPERATURE);
        /// Radiation: Q_rad = ε * σ * A * (T^4 - T_amb^4)
        double Q_rad = Config.INVERTER_EMISSIVITY * Config.STEFAN_BOLTZMANN * Config.INVERTER_SURFACE_AREA
                * (Math.pow(T_K, 4) - Math.pow(T_amb_K, 4));
        /// Total heat balance: dT/dt = (Q_gen - Q_conv - Q_rad) / C
        double coolingEffect = (Q_conv + Q_rad) / Config.INVERTER_THERMAL_CAPACITANCE;
        temperature += (heatGeneration - coolingEffect) * Config.SIMULATION_TIME_STEP;
        if (temperature < Config.AMBIENT_TEMPERATURE) temperature = Config.AMBIENT_TEMPERATURE;
    }

    public void setDcLinkVoltage(double voltage) {
        this.dcLinkVoltage = voltage;
    }

    public void setPwmFrequency(double frequency) {
        this.pwmFrequency = frequency;
    }

    public void setDeadTime(double time) {
        this.deadTime = time;
    }

    public void setModulationIndex(double index) {
        this.modulationIndex = index;
    }

    public void setHarmonicInjection(boolean enabled) {
        this.harmonicInjection = enabled;
    }

    public void setOvermodulation(boolean enabled) {
        this.overmodulation = enabled;
    }

    public void setCooling(double fanSpeed, double coolantFlow) {
        this.fanSpeed = fanSpeed;
        this.coolantFlow = coolantFlow;
    }

    public double getDcLinkVoltage() {
        return dcLinkVoltage;
    }

    public double getTemperature() {
        return temperature;
    }
}
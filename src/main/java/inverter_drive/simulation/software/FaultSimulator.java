package inverter_drive.simulation.software;

public class FaultSimulator {
    private final InverterPowerStage inverter;
    private final SensorModel sensors;
    private String currentFault = "None";
    private double faultTime = 0.0;

    public FaultSimulator(InverterPowerStage inverter, SensorModel sensors) {
        this.inverter = inverter;
        this.sensors = sensors;
    }

    public double[] applyFaults(double[] phaseVoltages, boolean autoReset,
                                double motorTemp, double inverterTemp,
                                double maxTemp, String protectionMode) {
        if (motorTemp > maxTemp || inverterTemp > maxTemp) {
            if (protectionMode.equals("Shutdown")) {
                currentFault = "Overheat";
                return new double[]{0, 0, 0}; // Stop inverter
            } else if (protectionMode.equals("Warning") && !currentFault.equals("Overheat")) {
                currentFault = "Overheat";
                faultTime = System.currentTimeMillis();
            }
        }
        if (autoReset && !currentFault.equals("None") && (System.currentTimeMillis() - faultTime) > 2000) {
            clearFault();
        }
        double[] modifiedVoltages = phaseVoltages.clone();
        switch (currentFault) {
            case "Overcurrent":
                for (int i = 0; i < 3; i++) modifiedVoltages[i] *= Config.OVERCURRENT_VOLTAGE_SCALE;
                break;
            case "Undervoltage":
                for (int i = 0; i < 3; i++) modifiedVoltages[i] *= Config.UNDERVOLTAGE_VOLTAGE_SCALE;
                break;
            case "Phase Loss":
                modifiedVoltages[0] = 0; // Loss of phase A
                break;
            case "Overheat":
                if (protectionMode.equals("Shutdown")) {
                    for (int i = 0; i < 3; i++) modifiedVoltages[i] = 0;
                }
                break;
            case "IGBTFailure":
                // Simulate intermittent failure on phase A
                if (Math.random() < Config.IGBT_FAILURE_DUTY_CYCLE) {
                    modifiedVoltages[0] = 0;
                }
                break;
        }
        return modifiedVoltages;
    }

    public void injectFault(String faultType) {
        if (!faultType.equals("None")) {
            currentFault = faultType;
            faultTime = System.currentTimeMillis();
        }
    }

    public void clearFault() {
        currentFault = "None";
    }

    public String getCurrentFault() {
        return currentFault;
    }
}
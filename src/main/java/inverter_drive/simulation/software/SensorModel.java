package inverter_drive.simulation.software;
import java.util.Random;

public class SensorModel {
    private boolean currentSensorFault = false;
    private double partialFailureScale = 1.0;
    private final Random random = new Random();
    public double[] measureCurrents(double[] phaseVoltages, InductionMotor motor) {
        double[] currents = new double[3];
        if (!currentSensorFault) {
            for (int i = 0; i < 3; i++) {
                currents[i] = phaseVoltages[i] / (motor.getResistance() + motor.getInductance() * 0.1);
                /// Add Gaussian noise
                currents[i] += random.nextGaussian() * Config.SENSOR_NOISE_STDDEV * currents[i];
                /// Apply partial failure
                currents[i] *= partialFailureScale;
            }
        }
        return currents;
    }
    public void setCurrentSensorFault(boolean fault) {
        this.currentSensorFault = fault;
    }
    public void setPartialFailureScale(double scale) {
        this.partialFailureScale = Math.max(0.0, Math.min(1.0, scale));
    }
}
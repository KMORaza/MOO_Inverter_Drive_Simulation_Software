package inverter_drive.simulation.software;

public class VfController {
    private final InductionMotor motor;
    private double vPerHz;
    private double maxVoltage = 230;
    private double currentSpeed = 0.0;
    private double kp = 0.1;
    private double ki = 0.01;
    private double integralError = 0.0;
    /// FOC variables
    private double torqueErrorIntegral = 0.0;
    private double fluxErrorIntegral = 0.0;
    private final double kpTorque = 0.5;
    private final double kiTorque = 0.05;
    private final double kpFlux = 0.3;
    private final double kiFlux = 0.03;

    public VfController(InductionMotor motor) {
        this.motor = motor;
        this.vPerHz = maxVoltage / 50.0;
    }

    public void setGains(double kp, double ki) {
        this.kp = kp;
        this.ki = ki;
    }

    public double[] updateControl(String mode, double speedRef, double torqueRef, double fluxRef,
                                  double accelRate, double direction, double time) {
        if (mode.equals("DTC")) {
            /// Direct Torque Control
            double torqueError = torqueRef - motor.getTorque();
            double fluxError = fluxRef - motor.getRotorFlux();
            /// Hysteresis bands
            boolean torqueBand = Math.abs(torqueError) > 0.05 * torqueRef;
            boolean fluxBand = Math.abs(fluxError) > 0.05 * fluxRef;
            int torqueState = torqueError > 0 ? 1 : -1;
            if (!torqueBand) torqueState = 0;
            int fluxState = fluxError > 0 ? 1 : -1;
            if (!fluxBand) fluxState = 0;
            /// Estimate stator flux angle
            double id = motor.getId(); // Direct-axis current
            double iq = motor.getIq(); // Quadrature-axis current
            double psiD = motor.getRotorFlux() + motor.getInductance() * id;
            double psiQ = motor.getInductance() * iq;
            double fluxAngle = Math.atan2(psiQ, psiD);
            /// Determine sector (1 to 6)
            int sector = (int) Math.floor((fluxAngle + Math.PI) / (Math.PI / 3.0)) % 6 + 1;
            if (sector < 1) sector += 6;
            /// Switching table for DTC
            int[][] switchingTable = {
                    /// {flux, torque} -> vector
                    {1, 1, 2}, {1, 0, 7}, {1, -1, 6},
                    {0, 1, 3}, {0, 0, 0}, {0, -1, 5},
                    {-1, 1, 4}, {-1, 0, 8}, {-1, -1, 4}
            };
            int tableIndex = (fluxState + 1) * 3 + (torqueState + 1);
            int vector = switchingTable[tableIndex][2];
            /// Voltage vector to PWM signals
            double[] va = new double[3];
            switch (vector) {
                case 1: va = new double[]{1, 0, 0}; break; // V1 (100)
                case 2: va = new double[]{1, 1, 0}; break; // V2 (110)
                case 3: va = new double[]{0, 1, 0}; break; // V3 (010)
                case 4: va = new double[]{0, 1, 1}; break; // V4 (011)
                case 5: va = new double[]{0, 0, 1}; break; // V5 (001)
                case 6: va = new double[]{1, 0, 1}; break; // V6 (101)
                case 7: va = new double[]{1, 1, 1}; break; // V7 (111)
                case 8: va = new double[]{0, 0, 0}; break; // V0 (000)
                default: va = new double[]{0, 0, 0};
            }
            /// Convert to PWM signals [0,1]
            double[] pwmSignals = new double[3];
            for (int i = 0; i < 3; i++) {
                pwmSignals[i] = 0.5 * (1 + va[i] * maxVoltage / (motor.getRatedVoltage() / Math.sqrt(3)));
            }
            return pwmSignals;
        } else if (mode.equals("FOC")) {
            /// Field-Oriented Control
            double speedError = speedRef - motor.getSpeed();
            double torqueRefAdjusted = kp * speedError + ki * (integralError += speedError * Config.SIMULATION_TIME_STEP);
            double torqueError = torqueRefAdjusted - motor.getTorque();
            double fluxError = fluxRef - motor.getRotorFlux();
            /// PI controllers for torque and flux
            double vq = kpTorque * torqueError + kiTorque * (torqueErrorIntegral += torqueError * Config.SIMULATION_TIME_STEP);
            double vd = kpFlux * fluxError + kiFlux * (fluxErrorIntegral += fluxError * Config.SIMULATION_TIME_STEP);
            /// Convert d-q voltages to three-phase (inverse Park-Clarke)
            double theta = 2 * Math.PI * speedRef * time * direction;
            double va = vd * Math.cos(theta) - vq * Math.sin(theta);
            double vb = vd * Math.cos(theta - 2 * Math.PI / 3) - vq * Math.sin(theta - 2 * Math.PI / 3);
            double vc = vd * Math.cos(theta + 2 * Math.PI / 3) - vq * Math.sin(theta + 2 * Math.PI / 3);
            /// Normalize to PWM signals
            double max = Math.max(Math.abs(va), Math.max(Math.abs(vb), Math.abs(vc)));
            if (max > 0) {
                va /= max;
                vb /= max;
                vc /= max;
            }
            return new double[]{0.5 * (1 + va), 0.5 * (1 + vb), 0.5 * (1 + vc)};
        } else {
            /// V/f control
            double speedError = speedRef - motor.getSpeed();
            integralError += speedError * Config.SIMULATION_TIME_STEP;
            double freq = kp * speedError + ki * integralError;
            double maxFreqChange = accelRate * Config.SIMULATION_TIME_STEP / (2 * Math.PI);
            freq = Math.max(Math.min(freq, currentSpeed + maxFreqChange), currentSpeed - maxFreqChange);
            currentSpeed = freq;
            double voltage = freq * vPerHz;
            double omega = 2 * Math.PI * freq * direction;
            double[] pwmSignals = new double[3];
            pwmSignals[0] = 0.5 * (1 + Math.sin(omega * time));
            pwmSignals[1] = 0.5 * (1 + Math.sin(omega * time - 2 * Math.PI / 3));
            pwmSignals[2] = 0.5 * (1 + Math.sin(omega * time + 2 * Math.PI / 3));
            return pwmSignals;
        }
    }
}
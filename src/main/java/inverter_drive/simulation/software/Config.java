package inverter_drive.simulation.software;

public class Config {
    public static final double SIMULATION_TIME_STEP = 0.0001; // seconds
    public static final double MOTOR_THERMAL_RESISTANCE = 0.1; // °C/W
    public static final double MOTOR_THERMAL_CAPACITANCE = 5000.0; // J/°C
    public static final double INVERTER_THERMAL_RESISTANCE = 0.05; // °C/W
    public static final double INVERTER_THERMAL_CAPACITANCE = 2000.0; // J/°C
    public static final double AMBIENT_TEMPERATURE = 25.0; // °C
    public static final double STEFAN_BOLTZMANN = 5.670367e-8; // W/m²·K⁴
    public static final double INVERTER_SURFACE_AREA = 0.5; // m²
    public static final double INVERTER_EMISSIVITY = 0.8; // Painted surface
    public static final double INVERTER_CONVECTION_BASE = 10.0; // W/m²·K (natural convection)
    public static final double INVERTER_CONVECTION_FAN_COEFF = 40.0; // W/m²·K per unit fan speed
    public static final double INVERTER_CONVECTION_COOLANT_COEFF = 20.0; // W/m²·K per L/min coolant flow
    public static final double MOTOR_SURFACE_AREA = 1.0; // m²
    public static final double MOTOR_EMISSIVITY = 0.85; // Painted motor surface
    public static final double MOTOR_CONVECTION_BASE = 15.0; // W/m²·K (natural convection)
    public static final double MOTOR_CONVECTION_FAN_COEFF = 35.0; // W/m²·K per unit fan speed
    public static final double MOTOR_CONVECTION_COOLANT_COEFF = 25.0; // W/m²·K per L/min coolant flow
    public static final double SENSOR_NOISE_STDDEV = 0.01; // Standard deviation for Gaussian noise
    public static final double OVERCURRENT_VOLTAGE_SCALE = 1.5; // Voltage scaling for overcurrent fault
    public static final double UNDERVOLTAGE_VOLTAGE_SCALE = 0.5; // Voltage scaling for undervoltage fault
    public static final double IGBT_FAILURE_DUTY_CYCLE = 0.3; // Duty cycle for IGBT failure simulation
}
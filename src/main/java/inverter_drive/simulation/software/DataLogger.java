package inverter_drive.simulation.software;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class DataLogger {
    private final Queue<String> logBuffer;
    private final int maxBufferSize = 1000;

    public DataLogger() {
        logBuffer = new LinkedList<>();
        logBuffer.add("Time,Va,Vb,Vc,Ia,Ib,Ic,Speed,Torque,ControlMode,Fault\n");
    }

    public synchronized void logData(double time, double[] voltages, double[] currents, double speed, double torque, String controlMode, String fault) {
        String line = String.format("%.3f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%s\n",
                time, voltages[0], voltages[1], voltages[2],
                currents[0], currents[1], currents[2], speed, torque, controlMode, fault);
        logBuffer.add(line);
        if (logBuffer.size() >= maxBufferSize) {
            exportToCSV();
        }
    }

    public synchronized void exportToCSV() {
        try (FileWriter writer = new FileWriter("simulation_data.csv")) {
            for (String line : logBuffer) {
                writer.write(line);
            }
            logBuffer.clear();
            logBuffer.add("Time,Va,Vb,Vc,Ia,Ib,Ic,Speed,Torque,ControlMode,Fault\n");
        } catch (IOException e) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("File Error");
                alert.setHeaderText("Failed to write CSV file");
                alert.setContentText("Error: " + e.getMessage());
                alert.showAndWait();
            });
        }
    }
}
package inverter_drive.simulation.software;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class WaveformVisualizer {
    private final Canvas canvas;
    private final double[][] voltageData = new double[3][1000]; // Va, Vb, Vc
    private final double[][] currentData = new double[3][1000]; // Ia, Ib, Ic
    private final double[] speedData = new double[1000];
    private int dataIndex = 0;
    private int lastDrawnIndex = -1;
    private boolean resetCanvas = false;
    private double voltageScale = 0.1;
    private double currentScale = 0.5;
    private double speedScale = 0.01;
    public WaveformVisualizer() {
        canvas = new Canvas(680, 550);
    }
    public Canvas getCanvas() {
        return canvas;
    }
    public void setScales(double maxVoltage, double maxCurrent, double maxSpeed) {
        voltageScale = maxVoltage > 0 ? 100.0 / maxVoltage : 0.1;
        currentScale = maxCurrent > 0 ? 50.0 / maxCurrent : 0.5;
        speedScale = maxSpeed > 0 ? 50.0 / maxSpeed : 0.01;
    }
    public void resetCanvas() {
        resetCanvas = true;
    }
    private void drawLabels(GraphicsContext gc, double height) {
        gc.setFont(Font.font("Consolas", 14));
        /// Voltage labels (Va, Vb, Vc)
        gc.setFill(Color.CYAN);
        gc.fillText("Va", 5, height / 4 - 20);
        gc.setFill(Color.YELLOW);
        gc.fillText("Vb", 5, height / 4);
        gc.setFill(Color.RED);
        gc.fillText("Vc", 5, height / 4 + 20);
        /// Current labels (Ia, Ib, Ic)
        gc.setFill(Color.MAGENTA);
        gc.fillText("Ia", 5, height / 2 - 20);
        gc.setFill(Color.LIGHTSKYBLUE);
        gc.fillText("Ib", 5, height / 2);
        gc.setFill(Color.ORANGE);
        gc.fillText("Ic", 5, height / 2 + 20);
        /// Speed label
        gc.setFill(Color.LIME);
        gc.fillText("Speed", 5, 3 * height / 4);
    }
    public synchronized void updateWaveforms(double[] voltages, double[] currents, double speed, double time) {
        for (int i = 0; i < 3; i++) {
            voltageData[i][dataIndex % 1000] = voltages[i];
            currentData[i][dataIndex % 1000] = currents[i];
        }
        speedData[dataIndex % 1000] = speed;
        dataIndex++;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double width = canvas.getWidth();
        double height = canvas.getHeight();
        int maxPoints = Math.min(dataIndex, 1000);
        if (resetCanvas || dataIndex == 1 || dataIndex % 1000 == 0) {
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, width, height);
            drawLabels(gc, height);
            lastDrawnIndex = -1;
            resetCanvas = false;
        }
        int startIndex = lastDrawnIndex + 1;
        if (startIndex < 0) startIndex = 0;
        /// Plot voltages (Va, Vb, Vc)
        Color[] voltageColors = {Color.CYAN, Color.YELLOW, Color.RED};
        for (int phase = 0; phase < 3; phase++) {
            gc.setStroke(voltageColors[phase]);
            gc.setLineWidth(1.5);
            gc.beginPath();
            for (int i = startIndex; i < maxPoints; i++) {
                double x = i * width / 1000;
                double y = height / 4 - voltageData[phase][i] * voltageScale;
                if (i == startIndex) {
                    gc.moveTo(x, y);
                } else {
                    double prevX = (i - 1) * width / 1000;
                    double prevY = height / 4 - voltageData[phase][i - 1] * voltageScale;
                    gc.quadraticCurveTo(prevX, prevY, (prevX + x) / 2, (prevY + y) / 2);
                    gc.lineTo(x, y);
                }
            }
            gc.stroke();
        }
        /// Plot currents (Ia, Ib, Ic)
        Color[] currentColors = {Color.MAGENTA, Color.LIGHTSKYBLUE, Color.ORANGE};
        for (int phase = 0; phase < 3; phase++) {
            gc.setStroke(currentColors[phase]);
            gc.setLineWidth(1.5);
            gc.beginPath();
            for (int i = startIndex; i < maxPoints; i++) {
                double x = i * width / 1000;
                double y = height / 2 - currentData[phase][i] * currentScale;
                if (i == startIndex) {
                    gc.moveTo(x, y);
                } else {
                    double prevX = (i - 1) * width / 1000;
                    double prevY = height / 2 - currentData[phase][i - 1] * currentScale;
                    gc.quadraticCurveTo(prevX, prevY, (prevX + x) / 2, (prevY + y) / 2);
                    gc.lineTo(x, y);
                }
            }
            gc.stroke();
        }
        /// Plot speed
        gc.setStroke(Color.LIME);
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = startIndex; i < maxPoints; i++) {
            double x = i * width / 1000;
            double y = 3 * height / 4 - speedData[i] * speedScale;
            if (i == startIndex) {
                gc.moveTo(x, y);
            } else {
                double prevX = (i - 1) * width / 1000;
                double prevY = 3 * height / 4 - speedData[i - 1] * speedScale;
                gc.quadraticCurveTo(prevX, prevY, (prevX + x) / 2, (prevY + y) / 2);
                gc.lineTo(x, y);
            }
        }
        gc.stroke();

        /// Grid
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(0.3);
        for (int i = 1; i < 13; i++) {
            double y = i * height / 13;
            gc.strokeLine(0, y, width, y);
        }
        for (int i = 1; i < 15; i++) {
            double x = i * width / 15;
            gc.strokeLine(x, 0, x, height);
        }
        lastDrawnIndex = maxPoints - 1;
    }
}
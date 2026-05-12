package pro.revive.utils.UtilesMed;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Animation ECG flottante dessinee sur un Canvas JavaFX.
 * Peut utiliser un Canvas fourni de l'exterieur (depuis FXML) ou en creer un.
 *
 * Usage avec Canvas FXML :
 *   EcgAnimation ecg = new EcgAnimation(myFxmlCanvas);
 *   ecg.start();
 *
 * Usage autonome :
 *   EcgAnimation ecg = new EcgAnimation(600, 60);
 *   ecg.start();
 *   pane.getChildren().add(ecg.getCanvas());
 */
public class EcgAnimation {

    private final Canvas        canvas;
    private final GraphicsContext gc;
    private final AnimationTimer  timer;

    private double offset = 0;
    private static final double SPEED = 1.8;

    private static final Color COLOR_TEAL = Color.web("#2ec4a0", 0.85);
    private static final Color COLOR_BLUE = Color.web("#1a9bbf", 0.55);
    private static final Color COLOR_GLOW = Color.web("#2ec4a0", 0.18);

    /** Constructeur avec Canvas externe (depuis FXML). */
    public EcgAnimation(Canvas externalCanvas) {
        this.canvas = externalCanvas;
        this.gc     = externalCanvas.getGraphicsContext2D();
        this.timer  = buildTimer();
    }

    /** Constructeur autonome — cree son propre Canvas. */
    public EcgAnimation(double width, double height) {
        this.canvas = new Canvas(width, height);
        this.canvas.setMouseTransparent(true);
        this.gc     = canvas.getGraphicsContext2D();
        this.timer  = buildTimer();
    }

    private AnimationTimer buildTimer() {
        return new AnimationTimer() {
            @Override
            public void handle(long now) {
                double w = canvas.getWidth();
                offset = (offset + SPEED) % (w * 2);
                draw();
            }
        };
    }

    public Canvas getCanvas() { return canvas; }
    public void start()       { timer.start(); }
    public void stop()        { timer.stop(); }

    // ── Dessin ────────────────────────────────────────────────────────────

    private void draw() {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        gc.clearRect(0, 0, w, h);

        // Ligne ECG principale (teal, haute)
        drawEcgLine(COLOR_TEAL, COLOR_GLOW, h * 0.38, 1.8, 0, w, h);

        // Ligne ECG secondaire (bleu, basse, plus petite)
        drawEcgLine(COLOR_BLUE, Color.TRANSPARENT, h * 0.72, 1.1, w * 0.6, w, h);
    }

    private void drawEcgLine(Color color, Color glow, double centerY,
                              double amplitude, double phaseShift,
                              double w, double h) {
        double period = 180.0;

        if (!glow.equals(Color.TRANSPARENT)) {
            gc.setStroke(glow);
            gc.setLineWidth(6);
            gc.beginPath();
            tracerEcg(centerY, amplitude, period, phaseShift, w, h);
            gc.stroke();
        }

        gc.setStroke(color);
        gc.setLineWidth(1.8);
        gc.beginPath();
        tracerEcg(centerY, amplitude, period, phaseShift, w, h);
        gc.stroke();
    }

    private void tracerEcg(double centerY, double amplitude,
                            double period, double phaseShift,
                            double w, double h) {
        boolean first = true;
        for (double x = -period; x < w + period; x += 0.8) {
            double pos = ((x + offset + phaseShift) % period + period) % period;
            double t   = pos / period;
            double y   = centerY + ecgSignal(t) * amplitude * (h * 0.22);
            if (first) { gc.moveTo(x, y); first = false; }
            else          gc.lineTo(x, y);
        }
    }

    /**
     * Signal ECG normalise : onde P, complexe QRS, onde T.
     * t in [0,1] = un cycle.
     */
    private double ecgSignal(double t) {
        double y = 0;
        y += gaussian(t, 0.10, 0.025) * 0.25;   // onde P
        y -= gaussian(t, 0.22, 0.012) * 0.18;   // onde Q
        y += gaussian(t, 0.26, 0.018) * 1.00;   // onde R (pic)
        y -= gaussian(t, 0.31, 0.014) * 0.30;   // onde S
        y += gaussian(t, 0.50, 0.055) * 0.35;   // onde T
        return y;
    }

    private double gaussian(double t, double mu, double sigma) {
        double d = t - mu;
        return Math.exp(-(d * d) / (2 * sigma * sigma));

    }
}

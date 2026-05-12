package pro.revive.controllers.TriageController;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import pro.revive.App;
import pro.revive.Navigator;
import pro.revive.services.TriageServices.VisualAssistanceService;
import pro.revive.utils.TriageUtils.AppExecutor;

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller for the Visual Assistance screen (Module 2 — Triage).
 *
 * Responsibilities (UI only):
 *   - Webcam lifecycle (start / stop / feed thread)
 *   - Motion detection loop (delegates detection logic to VisualAssistanceService)
 *   - Displaying AI commentary returned by VisualAssistanceService
 *   - Navigation
 *
 * All AI logic, API keys, image encoding and JSON parsing live in
 * VisualAssistanceService — this controller never touches them directly.
 */
public class VisualAssistanceController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Label     lblCommentary;
    @FXML private Label     lblStatus;
    @FXML private Label     lblNoCamera;
    @FXML private Button    btnToggle;
    @FXML private Label     lblUserName;

    // ── Service ───────────────────────────────────────────────────────────
    private final VisualAssistanceService service = new VisualAssistanceService();

    // ── Webcam state ──────────────────────────────────────────────────────
    private Webcam webcam;
    private ScheduledExecutorService cameraExecutor;
    private ScheduledExecutorService motionExecutor;
    private final AtomicBoolean running   = new AtomicBoolean(false);
    private final AtomicBoolean analysing = new AtomicBoolean(false);
    private volatile BufferedImage lastFrame;
    private volatile BufferedImage prevMotionFrame;
    private volatile long lastAnalysisTime = 0;

    private static final long COOLDOWN_MS = 5000; // min ms between AI calls

    // ── Init ──────────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        setIdleState();

        // Stop camera when user navigates away (scene root replaced)
        cameraView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && running.get()) stopCamera();
        });

        // Stop camera when window is closed
        App.primaryStage.setOnCloseRequest(e -> stopCamera());

        // Stop camera if JVM shuts down unexpectedly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            if (webcam != null && webcam.isOpen()) webcam.close();
        }));
    }

    // ── Camera toggle ─────────────────────────────────────────────────────

    @FXML
    public void toggleCamera() {
        if (running.get()) stopCamera();
        else               startCamera();
    }

    private void startCamera() {
        AppExecutor.run(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    Platform.runLater(() -> lblCommentary.setText("❌ Aucune caméra détectée sur ce poste."));
                    return;
                }
                if (webcam.isOpen()) {
                    webcam.close();
                    Thread.sleep(400);
                }
                webcam.open();
                running.set(true);

                Platform.runLater(() -> {
                    btnToggle.setText("⏹  Arrêter l'analyse");
                    btnToggle.setStyle(
                        "-fx-background-color: #DC2626; -fx-text-fill: white; " +
                        "-fx-font-size: 14px; -fx-font-weight: bold; " +
                        "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-cursor: hand;"
                    );
                    lblStatus.setText("● Caméra active");
                    lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #22C55E; -fx-font-weight: bold;");
                    lblNoCamera.setVisible(false);
                    lblCommentary.setText("Caméra active. Analyse automatique au prochain mouvement détecté.");
                });

                // ── Camera feed — ~15 fps ─────────────────────────────────
                cameraExecutor = Executors.newSingleThreadScheduledExecutor();
                cameraExecutor.scheduleAtFixedRate(() -> {
                    if (!running.get() || webcam == null) return;
                    try {
                        BufferedImage img = webcam.getImage();
                        if (img != null) {
                            lastFrame = img;
                            javafx.scene.image.Image fxImg = toFxImage(img);
                            Platform.runLater(() -> cameraView.setImage(fxImg));
                        }
                    } catch (Exception ignored) {}
                }, 0, 66, TimeUnit.MILLISECONDS);

                // ── Motion loop — checks every 800 ms ─────────────────────
                motionExecutor = Executors.newSingleThreadScheduledExecutor();
                motionExecutor.scheduleAtFixedRate(() -> {
                    if (!running.get()) return;
                    BufferedImage current = lastFrame;
                    if (current == null) return;

                    long now        = System.currentTimeMillis();
                    boolean ready   = (now - lastAnalysisTime) >= COOLDOWN_MS && !analysing.get();
                    BufferedImage prev = prevMotionFrame;

                    if (ready && prev != null && service.motionDetected(prev, current)) {
                        lastAnalysisTime = now;
                        analysing.set(true);
                        Platform.runLater(() -> lblCommentary.setText("🔍 Mouvement détecté — Analyse IA en cours..."));

                        // Run the AI call on a separate thread so it doesn't block the motion loop
                        final BufferedImage frameToAnalyse = current;
                        AppExecutor.run(() -> {
                            String result = service.analyzeFrame(frameToAnalyse);
                            Platform.runLater(() -> lblCommentary.setText(result));
                            analysing.set(false);
                        });
                    }
                    prevMotionFrame = current;
                }, 800, 800, TimeUnit.MILLISECONDS);

            } catch (Exception e) {
                Platform.runLater(() ->
                    lblCommentary.setText("❌ Erreur d'initialisation : " + e.getMessage())
                );
            }
        });
    }

    private void stopCamera() {
        running.set(false);
        shutdownExecutor(cameraExecutor);
        shutdownExecutor(motionExecutor);
        cameraExecutor = null;
        motionExecutor = null;
        AppExecutor.run(() -> {
            try {
                Thread.sleep(250);
                if (webcam != null && webcam.isOpen()) webcam.close();
                webcam = null;
            } catch (Exception ignored) {}
            Platform.runLater(this::setIdleState);
        });
    }

    private void setIdleState() {
        cameraView.setImage(null);
        btnToggle.setText("▶  Démarrer l'analyse");
        btnToggle.setStyle(
            "-fx-background-color: #2563EB; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: bold; " +
            "-fx-padding: 12px 20px; -fx-background-radius: 10px; -fx-cursor: hand;"
        );
        lblStatus.setText("● Caméra inactive");
        lblStatus.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        if (lblNoCamera != null) lblNoCamera.setVisible(true);
        lblCommentary.setText("Appuyez sur 'Démarrer' pour activer l'analyse en temps réel.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Converts a BufferedImage to a JavaFX Image without SwingFXUtils
     * (which requires the javafx.swing module, not available in all configs).
     */
    private javafx.scene.image.Image toFxImage(BufferedImage bf) {
        if (bf == null) return null;
        int w = bf.getWidth(), h = bf.getHeight();
        int[] buf = new int[w * h];
        bf.getRGB(0, 0, w, h, buf, 0, w);
        WritableImage wi = new WritableImage(w, h);
        PixelWriter pw = wi.getPixelWriter();
        pw.setPixels(0, 0, w, h, javafx.scene.image.PixelFormat.getIntArgbInstance(), buf, 0, w);
        return wi;
    }

    private void shutdownExecutor(ScheduledExecutorService exec) {
        if (exec != null && !exec.isShutdown()) {
            exec.shutdownNow();
            try { exec.awaitTermination(500, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {}
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @FXML public void goDashboard()        { stopCamera(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList()       { stopCamera(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()        { stopCamera(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()        { stopCamera(); Navigator.goTo("Salle_List"); }
    @FXML public void goVisualAssistance() { /* already here */ }
    @FXML public void goSurveillance()     { stopCamera(); Navigator.goTo("Surveillance"); }
    @FXML public void deconnexion()        { stopCamera(); Navigator.logout(); }
}

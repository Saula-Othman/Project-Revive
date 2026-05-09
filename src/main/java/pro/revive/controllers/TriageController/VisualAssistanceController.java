package pro.revive.controllers.TriageController;

import com.github.sarxos.webcam.Webcam;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import pro.revive.App;
import pro.revive.Navigator;
import pro.revive.utils.AppExecutor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.ResourceBundle;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class VisualAssistanceController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Label     lblCommentary;
    @FXML private Label     lblStatus;
    @FXML private Label     lblNoCamera;
    @FXML private Button    btnToggle;
    @FXML private Label     lblUserName;

    private Webcam webcam;
    private ScheduledExecutorService cameraExecutor;
    private ScheduledExecutorService motionExecutor;
    private final AtomicBoolean running   = new AtomicBoolean(false);
    private final AtomicBoolean analysing = new AtomicBoolean(false);
    private volatile BufferedImage lastFrame;
    private volatile BufferedImage prevMotionFrame;
    private volatile long lastAnalysisTime = 0;

    private static final long   COOLDOWN_MS       = 5000; // min ms between AI calls
    private static final double MOTION_THRESHOLD   = 12.0; // avg pixel diff to trigger

    // ── Groq AI config ────────────────────────────────────────────
    private static final String GROQ_API_KEY = loadGroqKey();
    private static final String GROQ_URL     = "url";
    private static final String MODEL        = "model";

    private static String loadGroqKey() {
        try (java.io.InputStream in = VisualAssistanceController.class
                .getResourceAsStream("/db.properties")) {
            java.util.Properties p = new java.util.Properties();
            p.load(in);
            return p.getProperty("groq.api.key", "");
        } catch (Exception e) {
            return "";
        }
    }

    private static final String PROMPT_JSON =
        "\"Analyse le haut du corps de ce patient en 2 lignes maximum.\\n" +
        "Ligne 1 — Mouvement : ce que le patient fait (ex: tient son bras droit, prot\\u00e8ge son \\u00e9paule gauche).\\n" +
        "Ligne 2 — Sympt\\u00f4me probable : la blessure ou douleur suspect\\u00e9e (ex: fracture, luxation, entorse).\\n" +
        "Sois court, direct, en fran\\u00e7ais. Pas d'explication. Termine par le niveau : \\u26a0 Critique / \\u2139 Mod\\u00e9r\\u00e9 / \\u2713 L\\u00e9ger.\"";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // ── Init ─────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        setIdleState();

        // Stop camera when user navigates away (scene root replaced)
        cameraView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && running.get()) stopCamera();
        });

        // Stop camera when window is closed (X button)
        App.primaryStage.setOnCloseRequest(e -> stopCamera());

        // Stop camera if JVM shuts down unexpectedly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            if (webcam != null && webcam.isOpen()) webcam.close();
        }));
    }

    // ── Camera toggle ─────────────────────────────────────────────
    @FXML
    public void toggleCamera() {
        if (running.get()) {
            stopCamera();
        } else {
            startCamera();
        }
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
                    lblCommentary.setText("Caméra active. Appuyez sur 'Analyser' quand le patient est en position.");
                });

                // Camera feed thread — ~15 fps
                cameraExecutor = Executors.newSingleThreadScheduledExecutor();
                cameraExecutor.scheduleAtFixedRate(() -> {
                    if (!running.get() || webcam == null) return;
                    try {
                        BufferedImage img = webcam.getImage();
                        if (img != null) {
                            lastFrame = img;
                            javafx.scene.image.Image fxImg = SwingFXUtils.toFXImage(img, null);
                            Platform.runLater(() -> cameraView.setImage(fxImg));
                        }
                    } catch (Exception ignored) {}
                }, 0, 66, TimeUnit.MILLISECONDS);

                // Motion detection thread — checks every 800ms, triggers AI when movement detected
                motionExecutor = Executors.newSingleThreadScheduledExecutor();
                motionExecutor.scheduleAtFixedRate(() -> {
                    if (!running.get()) return;
                    BufferedImage current = lastFrame;
                    if (current == null) return;

                    long now = System.currentTimeMillis();
                    boolean cooldownOk = (now - lastAnalysisTime) >= COOLDOWN_MS;
                    boolean notBusy    = !analysing.get();

                    if (cooldownOk && notBusy) {
                        BufferedImage prev = prevMotionFrame;
                        if (prev != null && motionDetected(prev, current)) {
                            lastAnalysisTime = now;
                            analysing.set(true);
                            Platform.runLater(() -> lblCommentary.setText("🔍 Mouvement détecté — Analyse IA en cours..."));
                            analyzeWithGroq(current);
                        }
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

    // ── Groq AI analysis ─────────────────────────────────────────
    private void analyzeWithGroq(BufferedImage frame) {
        try {
            // 1. Encode frame as base64 JPEG (scaled to 640px wide for speed)
            String base64Image = encodeToBase64(frame);

            // 2. Build JSON body (OpenAI-compatible vision format)
            String jsonBody = "{"
                + "\"model\":\"" + MODEL + "\","
                + "\"max_tokens\":350,"
                + "\"messages\":[{"
                +   "\"role\":\"user\","
                +   "\"content\":["
                +     "{\"type\":\"text\",\"text\":" + PROMPT_JSON + "},"
                +     "{\"type\":\"image_url\",\"image_url\":"
                +       "{\"url\":\"data:image/jpeg;base64," + base64Image + "\"}}"
                +   "]"
                + "}]"
                + "}";

            // 3. POST to Groq
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() == 200) {
                String content = extractContent(body);
                Platform.runLater(() -> lblCommentary.setText(content));
            } else {
                String errSnippet = body.length() > 200 ? body.substring(0, 200) : body;
                Platform.runLater(() -> lblCommentary.setText(
                    "❌ Erreur API " + response.statusCode() + " :\n" + errSnippet));
            }

        } catch (Exception e) {
            Platform.runLater(() -> lblCommentary.setText("❌ Erreur réseau : " + e.getMessage()));
        } finally {
            analysing.set(false); // ready for next movement
        }
    }

    /** Returns true if the two frames differ enough to count as real movement */
    private boolean motionDetected(BufferedImage prev, BufferedImage curr) {
        try {
            int w    = Math.min(prev.getWidth(),  curr.getWidth());
            int h    = Math.min(prev.getHeight(), curr.getHeight());
            int step = 8; // sample every 8th pixel — fast enough for real-time
            long diff = 0;
            int  samples = 0;
            for (int y = 0; y < h; y += step) {
                for (int x = 0; x < w; x += step) {
                    int p = prev.getRGB(x, y);
                    int c = curr.getRGB(x, y);
                    int dr = Math.abs(((p >> 16) & 0xFF) - ((c >> 16) & 0xFF));
                    int dg = Math.abs(((p >>  8) & 0xFF) - ((c >>  8) & 0xFF));
                    int db = Math.abs(( p        & 0xFF) - ( c        & 0xFF));
                    diff += (dr + dg + db) / 3;
                    samples++;
                }
            }
            double avgDiff = (double) diff / samples;
            return avgDiff > MOTION_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    /** Encode BufferedImage → JPEG → Base64 string (scaled to max 640px wide) */
    private String encodeToBase64(BufferedImage img) throws Exception {
        int targetW = Math.min(img.getWidth(), 640);
        int targetH = (int)(img.getHeight() * ((double) targetW / img.getWidth()));
        BufferedImage scaled = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        scaled.getGraphics().drawImage(
            img.getScaledInstance(targetW, targetH, java.awt.Image.SCALE_SMOOTH), 0, 0, null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(scaled, "jpeg", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /** Extract choices[0].message.content from Groq JSON response */
    private String extractContent(String json) {
        try {
            int msgIdx     = json.indexOf("\"message\"");
            if (msgIdx == -1) return json;
            int contentIdx = json.indexOf("\"content\"", msgIdx);
            if (contentIdx == -1) return json;
            int colon      = json.indexOf(":", contentIdx) + 1;
            while (colon < json.length() && Character.isWhitespace(json.charAt(colon))) colon++;
            if (json.charAt(colon) != '"') return json;

            StringBuilder sb = new StringBuilder();
            int i = colon + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    switch (next) {
                        case 'n':  sb.append('\n'); i += 2; break;
                        case 't':  sb.append('\t'); i += 2; break;
                        case '"':  sb.append('"');  i += 2; break;
                        case '\\': sb.append('\\'); i += 2; break;
                        default:   sb.append(next); i += 2; break;
                    }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return json;
        }
    }

    private void shutdownExecutor(ScheduledExecutorService exec) {
        if (exec != null && !exec.isShutdown()) {
            exec.shutdownNow();
            try { exec.awaitTermination(500, TimeUnit.MILLISECONDS); } catch (InterruptedException ignored) {} //NOSONAR
        }
    }

    // ── Navigation ───────────────────────────────────────────────
    @FXML public void goDashboard()        { stopCamera(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList()       { stopCamera(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()        { stopCamera(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()        { stopCamera(); Navigator.goTo("Salle_List"); }
    @FXML public void goVisualAssistance() { /* already on this page */ }
    @FXML public void goSurveillance()     { stopCamera(); Navigator.goTo("Surveillance"); }
    @FXML public void deconnexion()        { stopCamera(); Navigator.goTo("DashboardTriage"); }
}

package pro.revive.utils.UtilesMateriel;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.sound.sampled.*;

/**
 * Service de notifications visuelles et sonores pour REVIVE.
 */
public class NotificationService {

    public enum Type { SUCCESS, WARNING, ERROR, INFO }

    /**
     * Affiche une notification toast en bas à droite de la fenêtre.
     */
    public static void show(Stage owner, String message, Type type) {
        Platform.runLater(() -> {
            Popup popup = new Popup();

            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPrefWidth(340);
            box.setMinHeight(56);

            String icon, bg, border;
            switch (type) {
                case SUCCESS -> { icon = "✅"; bg = "#f0fdf4"; border = "#22c55e"; }
                case WARNING -> { icon = "⚠️"; bg = "#fffbeb"; border = "#f59e0b"; }
                case ERROR   -> { icon = "❌"; bg = "#fef2f2"; border = "#ef4444"; }
                default      -> { icon = "ℹ️"; bg = "#eff6ff"; border = "#3b82f6"; }
            }

            box.setStyle(String.format(
                "-fx-background-color: %s;" +
                "-fx-border-color: %s;" +
                "-fx-border-width: 0 0 0 4;" +
                "-fx-border-radius: 0 10 10 0;" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 14 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 16, 0, 0, 6);",
                bg, border
            ));

            Label lblIcon = new Label(icon);
            lblIcon.setStyle("-fx-font-size: 18px;");

            Label lblMsg = new Label(message);
            lblMsg.setStyle("-fx-text-fill: #1e293b; -fx-font-size: 13px; -fx-font-weight: bold; -fx-wrap-text: true;");
            lblMsg.setMaxWidth(270);
            lblMsg.setWrapText(true);

            box.getChildren().addAll(lblIcon, lblMsg);
            popup.getContent().add(box);
            popup.setAutoHide(true);

            // Position en bas à droite
            double x = owner.getX() + owner.getWidth() - 370;
            double y = owner.getY() + owner.getHeight() - 120;
            popup.show(owner, x, y);

            // Disparaît après 4 secondes
            Timeline hide = new Timeline(new KeyFrame(Duration.seconds(4), e -> popup.hide()));
            hide.play();
        });
    }

    /**
     * Joue un son d'alerte d'urgence (bip synthétique).
     */
    public static void playAlertSound() {
        new Thread(() -> {
            try {
                // Générer un bip synthétique (440 Hz, 0.5s)
                int sampleRate = 44100;
                int duration = 500; // ms
                int numSamples = sampleRate * duration / 1000;
                byte[] buffer = new byte[numSamples * 2];

                for (int i = 0; i < numSamples; i++) {
                    double angle = 2.0 * Math.PI * 880 * i / sampleRate; // 880 Hz
                    short sample = (short) (Math.sin(angle) * 16000);
                    buffer[2 * i]     = (byte) (sample & 0xFF);
                    buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
                }

                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();

                // Deuxième bip après 200ms
                Thread.sleep(200);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();

            } catch (Exception e) {
                System.err.println("[Sound] Erreur son: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Joue un son de succès (bip court aigu).
     */
    public static void playSuccessSound() {
        new Thread(() -> {
            try {
                int sampleRate = 44100;
                int numSamples = sampleRate * 300 / 1000;
                byte[] buffer = new byte[numSamples * 2];

                for (int i = 0; i < numSamples; i++) {
                    double angle = 2.0 * Math.PI * 1200 * i / sampleRate;
                    double envelope = Math.min(1.0, (double)(numSamples - i) / (numSamples * 0.3));
                    short sample = (short) (Math.sin(angle) * 12000 * envelope);
                    buffer[2 * i]     = (byte) (sample & 0xFF);
                    buffer[2 * i + 1] = (byte) ((sample >> 8) & 0xFF);
                }

                AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                System.err.println("[Sound] Erreur son: " + e.getMessage());
            }
        }).start();
    }
}

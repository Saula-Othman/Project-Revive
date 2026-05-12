package pro.revive.services.ServicesUser;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Random;

public class CaptchaService {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Random RANDOM = new Random();

    private String currentCode = "";

    /** Génère un nouveau code et le dessine sur le Canvas fourni. */
    public void generate(Canvas canvas) {
        currentCode = generateCode(6);
        draw(canvas, currentCode);
    }

    /** Retourne true si la saisie correspond au code (insensible à la casse). */
    public boolean validate(String input) {
        return currentCode.equalsIgnoreCase(input == null ? "" : input.trim());
    }

    public String getCurrentCode() { return currentCode; }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private void draw(Canvas canvas, String code) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // Fond dégradé
        gc.setFill(Color.web("#EEF2FF"));
        gc.fillRect(0, 0, w, h);

        // Lignes de bruit
        for (int i = 0; i < 8; i++) {
            gc.setStroke(Color.color(
                    RANDOM.nextDouble() * 0.5,
                    RANDOM.nextDouble() * 0.5,
                    RANDOM.nextDouble() * 0.8,
                    0.4));
            gc.setLineWidth(1.5);
            gc.strokeLine(
                    RANDOM.nextDouble() * w, RANDOM.nextDouble() * h,
                    RANDOM.nextDouble() * w, RANDOM.nextDouble() * h);
        }

        // Points de bruit
        for (int i = 0; i < 40; i++) {
            gc.setFill(Color.color(
                    RANDOM.nextDouble(),
                    RANDOM.nextDouble(),
                    RANDOM.nextDouble(),
                    0.3));
            double size = 2 + RANDOM.nextDouble() * 3;
            gc.fillOval(RANDOM.nextDouble() * w, RANDOM.nextDouble() * h, size, size);
        }

        // Dessiner chaque caractère avec rotation et couleur aléatoires
        double charWidth = w / (code.length() + 1);
        for (int i = 0; i < code.length(); i++) {
            gc.save();
            double x = charWidth * (i + 0.8);
            double y = h / 2 + 8;
            double angle = (RANDOM.nextDouble() - 0.5) * 30; // -15° à +15°

            gc.translate(x, y);
            gc.rotate(angle);

            // Couleur sombre pour lisibilité
            gc.setFill(Color.color(
                    RANDOM.nextDouble() * 0.3,
                    RANDOM.nextDouble() * 0.3,
                    0.4 + RANDOM.nextDouble() * 0.4));

            int fontSize = 22 + RANDOM.nextInt(6);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, fontSize));
            gc.fillText(String.valueOf(code.charAt(i)), 0, 0);
            gc.restore();
        }

        // Bordure
        gc.setStroke(Color.web("#1A56DB", 0.3));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(1, 1, w - 2, h - 2, 10, 10);
    }
}

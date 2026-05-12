package pro.revive.services.TriageServices;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Service IA — Analyse visuelle de patients via Groq Vision API.
 *
 * Responsabilités :
 *   - Appel HTTP à Groq (clé API, URL, modèle)
 *   - Encodage image → base64 JPEG
 *   - Détection de mouvement par différence de pixels
 *   - Parsing de la réponse JSON
 *
 * Le controller (VisualAssistanceController) ne fait qu'appeler
 * analyzeFrame() et motionDetected() — toute la logique est ici.
 */
public class VisualAssistanceService {

    // ── Groq config ───────────────────────────────────────────────────────
    private static final String GROQ_API_KEY = "KEY";
    private static final String GROQ_URL     = "URL";
    private static final String MODEL        = "meta-llama/llama-4-scout-17b-16e-instruct";

    // ── Motion detection threshold ────────────────────────────────────────
    /** Average per-pixel difference (0–255) required to count as real movement. */
    public static final double MOTION_THRESHOLD = 12.0;

    // ── Prompt (escaped for direct JSON embedding) ────────────────────────
    private static final String PROMPT_JSON =
        "\"Analyse le haut du corps de ce patient en 2 lignes maximum.\\n" +
        "Ligne 1 — Mouvement : ce que le patient fait (ex: tient son bras droit, prot\\u00e8ge son \\u00e9paule gauche).\\n" +
        "Ligne 2 — Sympt\\u00f4me probable : la blessure ou douleur suspect\\u00e9e (ex: fracture, luxation, entorse).\\n" +
        "Sois court, direct, en fran\\u00e7ais. Pas d'explication. Termine par le niveau : \\u26a0 Critique / \\u2139 Mod\\u00e9r\\u00e9 / \\u2713 L\\u00e9ger.\"";

    // ── Shared HTTP client (thread-safe, reused across calls) ─────────────
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    // ─────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Sends a camera frame to Groq Vision and returns the AI commentary.
     * Always returns a non-null String — errors are returned as readable messages.
     *
     * @param frame  The current webcam frame to analyse
     * @return       AI commentary in French, or an error message
     */
    public String analyzeFrame(BufferedImage frame) {
        try {
            String base64Image = encodeToBase64(frame);

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

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type",  "application/json")
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            if (response.statusCode() == 200) {
                return extractContent(body);
            } else {
                String snippet = body.length() > 200 ? body.substring(0, 200) : body;
                return "❌ Erreur API " + response.statusCode() + " :\n" + snippet;
            }

        } catch (Exception e) {
            return "❌ Erreur réseau : " + e.getMessage();
        }
    }

    /**
     * Returns true if the two frames differ enough to count as real movement.
     * Samples every 8th pixel for performance.
     *
     * @param prev  Previous frame
     * @param curr  Current frame
     * @return      true if movement detected
     */
    public boolean motionDetected(BufferedImage prev, BufferedImage curr) {
        try {
            int w    = Math.min(prev.getWidth(),  curr.getWidth());
            int h    = Math.min(prev.getHeight(), curr.getHeight());
            int step = 8; // sample every 8th pixel — fast enough for real-time
            long diff    = 0;
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

    // ─────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Scales the image to max 640 px wide and encodes it as a base64 JPEG string.
     * Smaller images → smaller payloads → faster API responses.
     */
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

    /**
     * Extracts choices[0].message.content from a Groq JSON response.
     * Manual parsing — avoids pulling in a JSON library dependency.
     */
    private String extractContent(String json) {
        try {
            int msgIdx = json.indexOf("\"message\"");
            if (msgIdx == -1) return json;
            int contentIdx = json.indexOf("\"content\"", msgIdx);
            if (contentIdx == -1) return json;
            int colon = json.indexOf(":", contentIdx) + 1;
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
}

package pro.revive.services.ServicesLabo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service d'intégration avec l'API Google Gemini.
 * Gère la communication HTTP avec l'API et la persistance de la clé API.
 */
public class GeminiService {

    // ── Endpoint Gemini 2.0 Flash
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    // ── Fichier de config partagé avec RadioAIService
    private static final File CONFIG_FILE = new File(
            System.getProperty("user.home") + File.separator + ".revive"
            + File.separator + "config.properties");

    private static final String CLE_API = "gemini.api.key";

    // ── Clé API en mémoire
    private static String apiKey = chargerCleDepuisFichier();

    // ── Prompt système médical
    private static final String SYSTEM_PROMPT =
            "Tu es un assistant médical expert intégré dans le système REVIVE de gestion des urgences médicales. " +
            "Tu aides les biologistes et médecins à interpréter les résultats d'examens médicaux. " +
            "Tu réponds toujours en français, de manière claire, professionnelle et concise. " +
            "Tu ne prescris jamais de médicaments ni ne poses de diagnostic définitif — " +
            "tu fournis des informations d'aide à la décision médicale. " +
            "Si une question dépasse tes compétences, tu le signales clairement. " +
            "Tu utilises des emojis médicaux pour rendre tes réponses plus lisibles (✅ 🔴 ⚠️ 💊 🔬 📋 etc.).";

    // ─────────────────────────────────────────────────────────────────────────
    // GESTION CLÉ API
    // ─────────────────────────────────────────────────────────────────────────

    private static String chargerCleDepuisFichier() {
        if (!CONFIG_FILE.exists()) return "";
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            java.util.Properties props = new java.util.Properties();
            props.load(in);
            return props.getProperty(CLE_API, "").trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static void setCleApi(String cle) {
        if (cle == null) return;
        apiKey = cle.trim();
        sauvegarderCle(apiKey);
    }

    public static String getCleApi() {
        return apiKey;
    }

    public static boolean hasCleApi() {
        return apiKey != null && !apiKey.isBlank() && apiKey.startsWith("AIza");
    }

    private static void sauvegarderCle(String cle) {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            java.util.Properties props = new java.util.Properties();
            if (CONFIG_FILE.exists()) {
                try (InputStream in = new FileInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }
            props.setProperty(CLE_API, cle);
            try (OutputStream out = new FileOutputStream(CONFIG_FILE)) {
                props.store(out, "REVIVE — Configuration IA");
            }
        } catch (Exception e) {
            System.err.println("⚠ Impossible de sauvegarder la clé Gemini : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION INTERNET
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean isInternetAvailable() {
        try {
            URL url = new URL("https://www.google.com");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("HEAD");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code == 200;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // APPEL API GEMINI
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie une question à Gemini avec le contexte médical du résultat.
     *
     * @param question      La question posée par l'utilisateur
     * @param contexte      Le contexte médical (compte rendu, état, patient)
     * @param historique    L'historique de la conversation (pour la mémoire)
     * @return              La réponse de Gemini ou un message d'erreur
     */
    public static String poserQuestion(String question, String contexte, String historique) {
        if (!hasCleApi()) {
            return "⚠️ Clé API Gemini non configurée.\n\nVeuillez entrer votre clé API dans le champ prévu.";
        }

        try {
            // ── Construction du prompt complet
            String promptComplet = SYSTEM_PROMPT + "\n\n" +
                    "=== CONTEXTE DU PATIENT ===\n" + contexte + "\n\n" +
                    (historique != null && !historique.isBlank()
                            ? "=== HISTORIQUE DE LA CONVERSATION ===\n" + historique + "\n\n"
                            : "") +
                    "=== QUESTION ACTUELLE ===\n" + question;

            // ── Corps JSON de la requête
            String jsonBody = "{"
                    + "\"contents\": [{"
                    + "  \"parts\": [{"
                    + "    \"text\": " + jsonEscape(promptComplet)
                    + "  }]"
                    + "}],"
                    + "\"generationConfig\": {"
                    + "  \"temperature\": 0.4,"
                    + "  \"maxOutputTokens\": 1024,"
                    + "  \"topP\": 0.8"
                    + "}"
                    + "}";

            // ── Connexion HTTP
            URL url = new URL(GEMINI_URL + apiKey);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            // ── Envoi
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            InputStream is = (code == 200) ? conn.getInputStream() : conn.getErrorStream();
            String response = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();

            // ── Retry automatique si quota dépassé (429)
            if (code == 429) {
                try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
                // Réessayer une fois
                URL url2 = new URL(GEMINI_URL + apiKey);
                HttpURLConnection conn2 = (HttpURLConnection) url2.openConnection();
                conn2.setRequestMethod("POST");
                conn2.setDoOutput(true);
                conn2.setConnectTimeout(15000);
                conn2.setReadTimeout(30000);
                conn2.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                try (OutputStream os = conn2.getOutputStream()) {
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                }
                int code2 = conn2.getResponseCode();
                InputStream is2 = (code2 == 200) ? conn2.getInputStream() : conn2.getErrorStream();
                response = new String(is2.readAllBytes(), StandardCharsets.UTF_8);
                conn2.disconnect();
                code = code2;
            }

            if (code != 200) {
                return gererErreurAPI(code, response);
            }

            return extraireTexteReponse(response);

        } catch (java.net.ConnectException e) {
            return "📡 Pas de connexion internet.\n\nL'assistant fonctionne en mode hors ligne.";
        } catch (java.net.SocketTimeoutException e) {
            return "⏱️ Délai d'attente dépassé.\n\nVeuillez réessayer dans quelques instants.";
        } catch (Exception e) {
            return "❌ Erreur de communication avec Gemini : " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING RÉPONSE JSON
    // ─────────────────────────────────────────────────────────────────────────

    private static String extraireTexteReponse(String json) {
        try {
            // Structure : {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
            int idx = json.indexOf("\"text\":");
            if (idx < 0) return "❌ Réponse inattendue du serveur.";

            int debut = json.indexOf("\"", idx + 7) + 1;
            int fin   = trouverFinChaine(json, debut);
            if (debut <= 0 || fin <= debut) return "❌ Impossible de lire la réponse.";

            String texte = json.substring(debut, fin);
            // Décoder les séquences d'échappement JSON
            return texte
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\/", "/");
        } catch (Exception e) {
            return "❌ Erreur de parsing : " + e.getMessage();
        }
    }

    /** Trouve la fin d'une chaîne JSON en gérant les caractères échappés */
    private static int trouverFinChaine(String json, int debut) {
        for (int i = debut; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\') { i++; continue; } // sauter le caractère échappé
            if (c == '"')  return i;
        }
        return -1;
    }

    private static String gererErreurAPI(int code, String body) {
        return switch (code) {
            case 400 -> "❌ Requête invalide (400).\n\nVérifiez votre clé API.";
            case 401, 403 -> "🔑 Clé API invalide ou expirée ("+code+").\n\nVeuillez vérifier votre clé Gemini.";
            case 429 -> "⏳ Quota dépassé (429).\n\nLe plan gratuit Gemini autorise 15 requêtes/minute.\nPatientez quelques secondes — une nouvelle tentative automatique a été effectuée.\nSi l'erreur persiste, attendez 1 minute avant de réessayer.";
            case 500, 503 -> "🔧 Serveur Gemini indisponible ("+code+").\n\nRéessayez dans quelques instants.";
            default -> "❌ Erreur API ("+code+") : " + body.substring(0, Math.min(200, body.length()));
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    /** Échappe une chaîne pour l'inclure dans un JSON */
    private static String jsonEscape(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append("\"");
        return sb.toString();
    }
}

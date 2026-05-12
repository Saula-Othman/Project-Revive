package pro.revive.services.ServicesLabo;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.json.JSONObject;

import java.io.File;

/**
 * ══════════════════════════════════════════════════════════════════
 *  REVIVE — Service d'Analyse Radiologique IA (DenseNet121)
 *
 *  Communique avec une API Flask locale (localhost:5000).
 *  Pas de ngrok — fonctionne entièrement en local.
 *
 *  Modèle : DenseNet121 — Détection de pneumonie
 *  Dataset : Chest X-Ray Pneumonia (Kaggle) — Accuracy ~87%
 * ══════════════════════════════════════════════════════════════════
 */
public class RadioAIService {

    // ── Serveur Flask local — fixe, pas de ngrok
    private static final String SERVER_URL  = "http://localhost:5000";
    private static final String PREDICT_URL = SERVER_URL + "/predict";
    private static final String HEALTH_URL  = SERVER_URL + "/health";

    // ─────────────────────────────────────────────────────────────────────────
    // RÉSULTAT DE L'ANALYSE
    // ─────────────────────────────────────────────────────────────────────────

    public static class AnalyseResult {
        public final String  etat;          // "Propre" ou "Grave"
        public final String  label;         // "NORMAL" ou "PNEUMONIA"
        public final double  probabilite;   // 0.0 – 1.0 (score brut du modèle)
        public final double  confiance;     // 0.0 – 100.0 (%)
        public final String  message;       // message d'interprétation
        public final boolean success;
        public final String  erreur;

        /** Résultat réussi */
        public AnalyseResult(String etat, String label,
                             double probabilite, double confiance, String message) {
            this.etat        = etat;
            this.label       = label;
            this.probabilite = probabilite;
            this.confiance   = confiance;
            this.message     = message;
            this.success     = true;
            this.erreur      = null;
        }

        /** Résultat en erreur */
        public AnalyseResult(String erreur) {
            this.etat        = null;
            this.label       = null;
            this.probabilite = 0;
            this.confiance   = 0;
            this.message     = null;
            this.success     = false;
            this.erreur      = erreur;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPATIBILITÉ — méthodes conservées pour le controller existant
    // ─────────────────────────────────────────────────────────────────────────

    public static String getServerUrl()              { return SERVER_URL; }
    public static void   setServerUrl(String url)    { /* URL fixe localhost */ }
    public static String getConfigFilePath()         { return "localhost:5000 (local)"; }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFICATION DISPONIBILITÉ DU SERVEUR
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean isServerAvailable() {
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(3))
                        .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(3))
                        .build())
                .build()) {

            HttpGet request = new HttpGet(HEALTH_URL);
            return client.execute(request, response -> {
                int code = response.getCode();
                return code == 200;
            });
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE D'UNE IMAGE RADIOLOGIQUE
    // ─────────────────────────────────────────────────────────────────────────

    public static AnalyseResult analyserImage(File imageFile) {
        // ── Validation du fichier
        if (imageFile == null || !imageFile.exists()) {
            return new AnalyseResult("Fichier image introuvable.");
        }

        String nom = imageFile.getName().toLowerCase();
        if (!nom.endsWith(".jpg") && !nom.endsWith(".jpeg") && !nom.endsWith(".png")) {
            return new AnalyseResult("Format non supporté. Utilisez JPG ou PNG.");
        }

        // ── Envoi via Apache HttpClient5
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(10))
                        .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(30))
                        .build())
                .build()) {

            HttpPost request = new HttpPost(PREDICT_URL);

            // ── Corps multipart — champ "file" attendu par Flask
            var entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", imageFile,
                            ContentType.IMAGE_JPEG, imageFile.getName())
                    .build();
            request.setEntity(entity);

            return client.execute(request, response -> {
                int code = response.getCode();
                String body = EntityUtils.toString(response.getEntity(), "UTF-8");

                if (code != 200) {
                    return new AnalyseResult(
                            "Erreur serveur (" + code + "). " +
                            "Vérifiez que le serveur Flask est démarré sur localhost:5000.");
                }

                return parseResponse(body);
            });

        } catch (org.apache.hc.client5.http.HttpHostConnectException e) {
            return new AnalyseResult(
                    "Serveur IA inaccessible.\n\n" +
                    "Démarrez le serveur Flask avec :\n" +
                    "  python app.py\n\n" +
                    "Le serveur doit tourner sur localhost:5000.");
        } catch (java.net.SocketTimeoutException e) {
            return new AnalyseResult(
                    "Délai d'attente dépassé.\n" +
                    "Le modèle met trop de temps à répondre. Réessayez.");
        } catch (Exception e) {
            // Connexion refusée ou autre erreur réseau
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (msg.toLowerCase().contains("connect") || msg.toLowerCase().contains("refused")) {
                return new AnalyseResult(
                        "Serveur IA inaccessible.\n\n" +
                        "Démarrez le serveur Flask avec :\n" +
                        "  python pneumonia_server.py\n\n" +
                        "Le serveur doit tourner sur localhost:5000.");
            }
            return new AnalyseResult("Erreur de communication : " + msg);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PARSING JSON — utilise org.json
    // ─────────────────────────────────────────────────────────────────────────

    private static AnalyseResult parseResponse(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            // Vérifier si c'est une erreur
            if (obj.has("error")) {
                return new AnalyseResult("Erreur IA : " + obj.getString("error"));
            }

            // Champs retournés par Flask DenseNet121
            // { "prediction": "NORMAL" | "PNEUMONIA", "score": 0.xxxx }
            String prediction = obj.getString("prediction");
            double score      = obj.getDouble("score");

            // Conversion vers le format REVIVE
            boolean estNormal = "NORMAL".equalsIgnoreCase(prediction);
            String  etat      = estNormal ? "Propre" : "Grave";
            String  label     = estNormal ? "NORMAL" : "PNEUMONIA";

            // Confiance : probabilité de la classe prédite
            double confiance = estNormal ? (1.0 - score) * 100.0 : score * 100.0;

            // Message d'interprétation clinique
            String message = genererMessageClinique(estNormal, confiance, score);

            return new AnalyseResult(etat, label, score, confiance, message);

        } catch (Exception e) {
            return new AnalyseResult(
                    "Réponse invalide du serveur.\n" +
                    "Réponse reçue : " + json.substring(0, Math.min(200, json.length())));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MESSAGE CLINIQUE INTELLIGENT
    // ─────────────────────────────────────────────────────────────────────────

    private static String genererMessageClinique(boolean estNormal, double confiance, double score) {
        if (estNormal) {
            if (confiance >= 90) {
                return String.format(
                        "✅ Radiographie NORMALE — Aucune anomalie pulmonaire détectée.\n" +
                        "Confiance : %.1f%% — Résultat fiable.\n" +
                        "Aucun signe compatible avec une pneumonie.", confiance);
            } else {
                return String.format(
                        "🟡 Radiographie probablement NORMALE (%.1f%% de confiance).\n" +
                        "Résultat à confirmer par un radiologue.\n" +
                        "Aucun signe évident de pneumonie détecté.", confiance);
            }
        } else {
            if (confiance >= 90) {
                return String.format(
                        "🔴 PNEUMONIE DÉTECTÉE — Anomalie pulmonaire significative.\n" +
                        "Confiance : %.1f%% — Résultat très fiable.\n" +
                        "Consultation médicale urgente recommandée.", confiance);
            } else if (confiance >= 70) {
                return String.format(
                        "⚠️ Signes suspects de PNEUMONIE (%.1f%% de confiance).\n" +
                        "Anomalie pulmonaire probable — Avis médical requis.\n" +
                        "Examens complémentaires conseillés.", confiance);
            } else {
                return String.format(
                        "🟡 Anomalie possible détectée (%.1f%% de confiance).\n" +
                        "Résultat incertain — Interprétation médicale nécessaire.\n" +
                        "Consultation d'un radiologue recommandée.", confiance);
            }
        }
    }
}

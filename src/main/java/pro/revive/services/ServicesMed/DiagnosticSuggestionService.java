package pro.revive.services.ServicesMed;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Service IA — Suggestion de diagnostic via Groq API (gratuit).
 * Modele : llama3-8b-8192
 * Cle gratuite sur : https://console.groq.com
 *
 * Fallback : si Groq echoue, utilise une base locale de diagnostics.
 */
public class DiagnosticSuggestionService {

    // ── Groq API ──────────────────────────────────────────────────────────
    private static final String GROQ_URL   = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_MODEL = "llama-3.3-70b-versatile";  // Meilleur modele gratuit
    private static final int    TIMEOUT    = 20;

    private static final String SYSTEM_PROMPT =
        "Tu es un assistant medical pour urgentistes francophones. "
      + "A partir du motif de consultation et des symptomes fournis, propose un diagnostic probable en 2-3 lignes maximum. "
      + "Format : diagnostic principal, puis eventuellement 1-2 diagnostics differentiels. "
      + "Prends en compte le motif initial d'admission pour affiner ton analyse. "
      + "Reponds uniquement en francais. Sois concis et clinique.";

    // ── Clé Groq ──────────────────────────────────────────────────────────
    private static final String GROQ_API_KEY = "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";

    // ── Membres ───────────────────────────────────────────────────────────
    private final OkHttpClient client;
    private final Gson         gson;
    private final String       groqKey;

    public DiagnosticSuggestionService() {
        this.client  = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build();
        this.gson    = new Gson();
        this.groqKey = GROQ_API_KEY;
    }

    // ── API publique ──────────────────────────────────────────────────────

    /**
     * Suggere un diagnostic a partir des symptomes.
     * Essaie Groq d'abord, puis fallback local si indisponible.
     */
    public String suggerer(String symptomes) throws IOException {
        return suggerer(symptomes, null);
    }

    /**
     * Suggere un diagnostic a partir des symptomes ET du motif de consultation.
     * Le motif enrichit le contexte pour une suggestion plus precise.
     */
    public String suggerer(String symptomes, String motifConsultation) throws IOException {
        if (symptomes == null || symptomes.trim().isEmpty()) {
            throw new IllegalArgumentException("Les symptomes ne peuvent pas etre vides.");
        }

        String contexte = symptomes.trim();
        if (motifConsultation != null && !motifConsultation.trim().isEmpty()) {
            contexte = "Motif de consultation : " + motifConsultation.trim() + "\n"
                     + "Symptomes observes : " + symptomes.trim();
        }

        // Essai Groq
        if (groqKey != null && !groqKey.startsWith("VOTRE_")) {
            try {
                return appellerGroq(contexte);
            } catch (IOException e) {
                System.err.println("[DiagnosticIA] Groq indisponible : " + e.getMessage());
                // Fallback local
                return fallbackLocal(contexte);
            }
        }

        // Pas de cle configuree — fallback local
        return fallbackLocal(contexte);
    }

    // ── Appel Groq ────────────────────────────────────────────────────────

    private String appellerGroq(String symptomes) throws IOException {
        String corps = construireRequete(symptomes, GROQ_MODEL);

        Request request = new Request.Builder()
            .url(GROQ_URL)
            .addHeader("Authorization", "Bearer " + groqKey)
            .addHeader("Content-Type",  "application/json")
            .post(RequestBody.create(corps, MediaType.get("application/json; charset=utf-8")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                String msg = extraireMessageErreur(body);
                throw new IOException("Groq API erreur " + response.code() + " : " + msg);
            }
            return extraireContenu(body);
        }
    }

    // ── Construction requete JSON ─────────────────────────────────────────

    private String construireRequete(String symptomes, String model) {
        JsonObject root = new JsonObject();
        root.addProperty("model",       model);
        root.addProperty("max_tokens",  350);
        root.addProperty("temperature", 0.3);

        JsonArray messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role",    "system");
        sys.addProperty("content", SYSTEM_PROMPT);
        messages.add(sys);

        JsonObject user = new JsonObject();
        user.addProperty("role",    "user");
        user.addProperty("content", symptomes);
        messages.add(user);

        root.add("messages", messages);
        return gson.toJson(root);
    }

    // ── Parsing reponse ───────────────────────────────────────────────────

    private String extraireContenu(String json) throws IOException {
        try {
            JsonObject root    = gson.fromJson(json, JsonObject.class);
            JsonArray  choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty())
                throw new IOException("Reponse vide.");
            return choices.get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString().trim();
        } catch (Exception e) {
            throw new IOException("Parsing impossible : " + e.getMessage());
        }
    }

    private String extraireMessageErreur(String json) {
        try {
            JsonObject root  = gson.fromJson(json, JsonObject.class);
            JsonObject error = root.getAsJsonObject("error");
            if (error != null && error.has("message"))
                return error.get("message").getAsString();
        } catch (Exception ignored) {}
        return json.length() > 150 ? json.substring(0, 150) : json;
    }

    // ── Fallback local ────────────────────────────────────────────────────

    /**
     * Base de diagnostics locaux — fonctionne sans internet.
     * Analyse les mots-cles des symptomes et retourne une suggestion.
     */
    private String fallbackLocal(String symptomes) {
        String s = symptomes.toLowerCase();

        if (contient(s, "fievre", "toux", "dyspnee", "expectoration"))
            return "Diagnostic probable : Pneumonie communautaire.\n"
                 + "Differentiels : Bronchite aigue, Pleuresie infectieuse.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "douleur thoracique", "irradiation", "sueurs", "nausee"))
            return "Diagnostic probable : Syndrome coronarien aigu (SCA) a exclure en urgence.\n"
                 + "Differentiels : Dissection aortique, Embolie pulmonaire.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "cephalee", "raideur", "nuque", "photophobie", "fievre"))
            return "Diagnostic probable : Meningite bacterienne (urgence absolue).\n"
                 + "Differentiels : Meningite virale, Hemorragie sous-arachnoïdienne.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "douleur abdominale", "nausee", "vomissement", "fievre"))
            return "Diagnostic probable : Appendicite aigue ou gastro-enterite.\n"
                 + "Differentiels : Peritonite, Occlusion intestinale.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "dyspnee", "sibilants", "wheezing", "asthme"))
            return "Diagnostic probable : Crise d'asthme aigue.\n"
                 + "Differentiels : BPCO decompensee, OAP.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "hypoglycemie", "glycemie", "diabete", "confusion", "sueurs"))
            return "Diagnostic probable : Hypoglycemie severe.\n"
                 + "Differentiels : AVC, Intoxication.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "traumatisme", "chute", "fracture", "douleur", "gonflement"))
            return "Diagnostic probable : Traumatisme osseux ou articulaire.\n"
                 + "Differentiels : Entorse, Fracture, Contusion.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        if (contient(s, "fievre", "toux"))
            return "Diagnostic probable : Infection respiratoire aigue (virale ou bacterienne).\n"
                 + "Differentiels : Grippe, COVID-19, Pneumonie.\n"
                 + "(Suggestion locale — configurez Groq pour l'IA)";

        return "Symptomes insuffisants pour une suggestion automatique.\n"
             + "Veuillez configurer Groq API pour des suggestions IA completes.\n"
             + "Cle gratuite sur : https://console.groq.com";
    }

    private boolean contient(String texte, String... mots) {
        for (String mot : mots) {
            if (texte.contains(mot)) return true;
        }
        return false;
    }

}

package pro.revive.services.ServicesMed;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service d'interprétation des commandes vocales via Groq API.
 *
 * Transforme un texte dicté en commande structurée JSON :
 *   { "action": "remplir_champ", "params": { "champ": "diagnostic", "valeur": "pneumonie" } }
 *
 * Actions supportées :
 *   - remplir_champ   : remplir symptomes, diagnostic, orientation
 *   - ajouter_ordonnance : ajouter un médicament à l'ordonnance
 *   - cloturer        : clôturer la consultation
 *   - annuler         : annuler / effacer un champ
 *   - inconnu         : commande non reconnue
 */
public class VoiceCommandService {

    private static final String GROQ_URL     = "URL";
    private static final String GROQ_MODEL   = "Model";
    private static final String GROQ_API_KEY = "KEY";
    private static final int    TIMEOUT      = 15;

    // Prompt système — force une réponse JSON stricte
    private static final String SYSTEM_PROMPT =
        "Tu es un assistant médical pour urgentistes. " +
        "L'utilisateur te donne une commande vocale en français. " +
        "Tu dois analyser cette commande et retourner UNIQUEMENT un objet JSON valide, sans texte avant ni après. " +
        "Le JSON doit avoir exactement cette structure : " +
        "{\"action\": \"...\", \"params\": {...}} " +
        "\n\nActions possibles et leurs params :" +
        "\n- remplir_champ : {\"champ\": \"symptomes\"|\"diagnostic\"|\"orientation\", \"valeur\": \"...\"}" +
        "\n- ajouter_ordonnance : {\"medicament\": \"...\", \"dosage\": \"...\", \"frequence\": \"...\", \"duree\": \"...\"}" +
        "\n- cloturer : {} " +
        "\n- annuler : {\"champ\": \"symptomes\"|\"diagnostic\"|\"orientation\"|\"tout\"}" +
        "\n- inconnu : {\"message\": \"raison\"}" +
        "\n\nOrientations valides : Sortie, Hospitalisation, Transfert" +
        "\n\nExemples :" +
        "\nCommande: 'enregistre le diagnostic pneumopathie et oriente en hospitalisation'" +
        "\nRéponse: {\"action\":\"remplir_champ\",\"params\":{\"champ\":\"diagnostic\",\"valeur\":\"Pneumopathie\",\"orientation\":\"Hospitalisation\"}}" +
        "\nCommande: 'ajoute amoxicilline 500mg trois fois par jour pendant 7 jours'" +
        "\nRéponse: {\"action\":\"ajouter_ordonnance\",\"params\":{\"medicament\":\"Amoxicilline\",\"dosage\":\"500mg\",\"frequence\":\"3 fois/jour\",\"duree\":\"7 jours\"}}" +
        "\nCommande: 'cloture la consultation'" +
        "\nRéponse: {\"action\":\"cloturer\",\"params\":{}}" +
        "\nCommande: 'efface les symptomes'" +
        "\nRéponse: {\"action\":\"annuler\",\"params\":{\"champ\":\"symptomes\"}}";

    // ── Classe Command ────────────────────────────────────────────────────

    public static class Command {
        public final String              action;
        public final Map<String, Object> params;

        public Command(String action, Map<String, Object> params) {
            this.action = action;
            this.params = params != null ? params : new HashMap<>();
        }

        public String getParam(String key) {
            Object v = params.get(key);
            return v != null ? v.toString().trim() : "";
        }

        @Override
        public String toString() {
            return "Command{action='" + action + "', params=" + params + "}";
        }
    }

    // ── Membres ───────────────────────────────────────────────────────────

    private final OkHttpClient client;
    private final Gson         gson;
    private final String       groqKey;

    public VoiceCommandService() {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .build();
        this.gson    = new Gson();
        this.groqKey = GROQ_API_KEY;
    }

    // ── API publique ──────────────────────────────────────────────────────

    /**
     * Analyse un texte vocal et retourne une commande structurée.
     * Toujours retourne un Command non-null (action="inconnu" si échec).
     */
    public Command parseCommand(String voiceText) {
        if (voiceText == null || voiceText.trim().isEmpty()) {
            return new Command("inconnu", Map.of("message", "Texte vide"));
        }

        String text = voiceText.trim();
        System.out.println("[VoiceCmd] Texte reçu : " + text);

        // Essai via Groq
        if (groqKey != null && !groqKey.isBlank() && !groqKey.startsWith("VOTRE_")) {
            try {
                String json = appellerGroq(text);
                System.out.println("[VoiceCmd] Réponse Groq : " + json);
                Command cmd = parseJson(json);
                if (!"inconnu".equals(cmd.action)) return cmd;
                // Si Groq dit inconnu, essayer le fallback local
            } catch (Exception e) {
                System.err.println("[VoiceCmd] Groq erreur : " + e.getMessage());
            }
        }

        // Fallback local amélioré
        Command local = fallbackLocal(text.toLowerCase());
        System.out.println("[VoiceCmd] Fallback local : " + local);
        return local;
    }

    // ── Appel Groq ────────────────────────────────────────────────────────

    private String appellerGroq(String voiceText) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("model",       GROQ_MODEL);
        root.addProperty("max_tokens",  300);
        root.addProperty("temperature", 0.1); // très déterministe pour du JSON

        JsonArray messages = new JsonArray();

        JsonObject sys = new JsonObject();
        sys.addProperty("role",    "system");
        sys.addProperty("content", SYSTEM_PROMPT);
        messages.add(sys);

        JsonObject user = new JsonObject();
        user.addProperty("role",    "user");
        user.addProperty("content", "Commande vocale : " + voiceText);
        messages.add(user);

        root.add("messages", messages);

        Request request = new Request.Builder()
            .url(GROQ_URL)
            .addHeader("Authorization", "Bearer " + groqKey)
            .addHeader("Content-Type",  "application/json")
            .post(RequestBody.create(gson.toJson(root),
                MediaType.get("application/json; charset=utf-8")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful())
                throw new IOException("HTTP " + response.code());

            // Extraire le contenu de la réponse
            JsonObject resp    = gson.fromJson(body, JsonObject.class);
            JsonArray  choices = resp.getAsJsonArray("choices");
            return choices.get(0)
                .getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString().trim();
        }
    }

    // ── Parsing JSON ──────────────────────────────────────────────────────

    private Command parseJson(String jsonStr) {
        try {
            // Nettoyer le JSON (parfois entouré de ```json ... ```)
            String clean = jsonStr
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

            JsonObject obj    = gson.fromJson(clean, JsonObject.class);
            String     action = obj.has("action") ? obj.get("action").getAsString() : "inconnu";

            Map<String, Object> params = new HashMap<>();
            if (obj.has("params") && obj.get("params").isJsonObject()) {
                JsonObject p = obj.getAsJsonObject("params");
                for (Map.Entry<String, JsonElement> e : p.entrySet()) {
                    params.put(e.getKey(), e.getValue().getAsString());
                }
            }
            System.out.println("[VoiceCmd] Commande parsée : " + action + " " + params);
            return new Command(action, params);

        } catch (Exception e) {
            System.err.println("[VoiceCmd] Parsing JSON échoué : " + e.getMessage() + " | JSON: " + jsonStr);
            return new Command("inconnu", Map.of("message", "Parsing échoué : " + jsonStr));
        }
    }

    // ── Fallback local par mots-clés ──────────────────────────────────────

    private Command fallbackLocal(String text) {
        System.out.println("[VoiceCmd] Fallback analyse : '" + text + "'");

        // ── Clôturer ──────────────────────────────────────────────────────
        if (contient(text, "clotur", "terminer", "fermer", "finir", "fin consul")) {
            return new Command("cloturer", new HashMap<>());
        }

        // ── Ordonnance / médicament ───────────────────────────────────────
        if (contient(text, "ordonnance", "prescri", "ajoute", "médicament", "medicament",
                     "mg", "comprimé", "comprimes", "gélule", "gelule", "sirop",
                     "amoxicilline", "paracétamol", "paracetamol", "ibuprofène", "ibuprofen",
                     "doliprane", "augmentin", "azithromycine", "metformine", "oméprazole")) {

            Map<String, Object> p = new HashMap<>();

            // Extraire le médicament — mot après "ajoute" ou "prescris" ou premier mot inconnu
            String med = extraireMedicament(text);
            p.put("medicament", med);

            // Extraire le dosage (ex: 500mg, 1g, 250 mg)
            String dosage = extraireRegex(text, "\\d+\\s*(?:mg|g|ml|mcg|µg|ui)");
            p.put("dosage", dosage);

            // Extraire la fréquence
            String freq = extraireFrequence(text);
            p.put("frequence", freq);

            // Extraire la durée
            String duree = extraireDuree(text);
            p.put("duree", duree);

            System.out.println("[VoiceCmd] Ordonnance détectée : " + p);
            return new Command("ajouter_ordonnance", p);
        }

        // ── Orientation ───────────────────────────────────────────────────
        if (contient(text, "hospitali", "hospitalise", "hospitalisation")) {
            return new Command("remplir_champ",
                Map.of("champ", "orientation", "valeur", "Hospitalisation"));
        }
        if (contient(text, "transfert", "transferer", "transférer")) {
            return new Command("remplir_champ",
                Map.of("champ", "orientation", "valeur", "Transfert"));
        }
        if (contient(text, "sortie", "rentrer", "retour domicile", "domicile")) {
            return new Command("remplir_champ",
                Map.of("champ", "orientation", "valeur", "Sortie"));
        }

        // ── Diagnostic ────────────────────────────────────────────────────
        if (contient(text, "diagnostic", "diagnostique", "diagnostiquer",
                     "enregistre le", "note le", "écris le")) {
            String valeur = extraireApres(text,
                "diagnostic est", "diagnostic :", "diagnostic",
                "diagnostique est", "enregistre le", "note le", "écris le");
            // Supprimer "et oriente..." de la valeur
            if (valeur.contains(" et ")) valeur = valeur.substring(0, valeur.indexOf(" et ")).trim();
            if (valeur.contains(" oriente")) valeur = valeur.substring(0, valeur.indexOf(" oriente")).trim();

            Map<String, Object> p = new HashMap<>();
            p.put("champ", "diagnostic");
            p.put("valeur", capitaliser(valeur));

            // Détecter orientation combinée
            if (contient(text, "hospitali")) p.put("orientation", "Hospitalisation");
            else if (contient(text, "transfert")) p.put("orientation", "Transfert");
            else if (contient(text, "sortie")) p.put("orientation", "Sortie");

            return new Command("remplir_champ", p);
        }

        // ── Symptômes ─────────────────────────────────────────────────────
        if (contient(text, "symptome", "symptôme", "patient a", "patient présente",
                     "patient souffre", "plainte", "se plaint")) {
            String valeur = extraireApres(text,
                "symptomes sont", "symptômes sont", "symptomes :",
                "symptômes :", "symptome", "symptôme",
                "patient a", "patient présente", "patient souffre", "se plaint de");
            return new Command("remplir_champ",
                Map.of("champ", "symptomes", "valeur", capitaliser(valeur)));
        }

        // ── Annuler / effacer ─────────────────────────────────────────────
        if (contient(text, "efface", "annule", "supprime", "vide", "efface tout")) {
            String champ = contient(text, "diagnostic") ? "diagnostic"
                         : contient(text, "symptome", "symptôme") ? "symptomes"
                         : contient(text, "orientation") ? "orientation"
                         : "tout";
            return new Command("annuler", Map.of("champ", champ));
        }

        return new Command("inconnu", Map.of("message", "Non reconnu : " + text));
    }

    // ── Extracteurs ───────────────────────────────────────────────────────

    private String extraireMedicament(String text) {
        // Chercher après les mots déclencheurs
        String[] triggers = {"ajoute ", "prescris ", "ordonnance ", "médicament ", "medicament "};
        for (String t : triggers) {
            int idx = text.indexOf(t);
            if (idx >= 0) {
                String reste = text.substring(idx + t.length()).trim();
                // Prendre le premier mot (nom du médicament)
                String[] mots = reste.split("\\s+");
                if (mots.length > 0 && !mots[0].isEmpty()) return capitaliser(mots[0]);
            }
        }
        // Chercher un nom de médicament connu
        String[] medsConnus = {"amoxicilline", "paracétamol", "paracetamol", "ibuprofène",
            "ibuprofen", "doliprane", "augmentin", "azithromycine", "metformine",
            "oméprazole", "omeprazole", "aspirine", "codeine", "tramadol",
            "ciprofloxacine", "amoxicilline", "ceftriaxone", "metronidazole"};
        for (String med : medsConnus) {
            if (text.contains(med)) return capitaliser(med);
        }
        return "Médicament";
    }

    private String extraireFrequence(String text) {
        if (contient(text, "une fois", "1 fois")) return "1 fois/jour";
        if (contient(text, "deux fois", "2 fois")) return "2 fois/jour";
        if (contient(text, "trois fois", "3 fois")) return "3 fois/jour";
        if (contient(text, "quatre fois", "4 fois")) return "4 fois/jour";
        if (contient(text, "matin et soir", "matin soir")) return "Matin et soir";
        if (contient(text, "matin midi soir", "trois prises")) return "Matin, midi et soir";
        if (contient(text, "toutes les 8", "toutes les huit")) return "Toutes les 8h";
        if (contient(text, "toutes les 12", "toutes les douze")) return "Toutes les 12h";
        if (contient(text, "toutes les 6")) return "Toutes les 6h";
        if (contient(text, "par jour", "quotidien")) {
            String num = extraireRegex(text, "\\d+(?=\\s*fois)");
            return num.isEmpty() ? "1 fois/jour" : num + " fois/jour";
        }
        return "";
    }

    private String extraireDuree(String text) {
        // "pendant X jours/semaines"
        String jours    = extraireRegex(text, "\\d+\\s*(?:jours?|j\\b)");
        String semaines = extraireRegex(text, "\\d+\\s*semaines?");
        String mois     = extraireRegex(text, "\\d+\\s*mois");
        if (!jours.isEmpty())    return jours;
        if (!semaines.isEmpty()) return semaines;
        if (!mois.isEmpty())     return mois;
        return "";
    }

    private String extraireRegex(String text, String pattern) {
        java.util.regex.Matcher m = java.util.regex.Pattern
            .compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(text);
        return m.find() ? m.group().trim() : "";
    }

    private String extraireApres(String text, String... mots) {
        for (String mot : mots) {
            int idx = text.indexOf(mot);
            if (idx >= 0) {
                String reste = text.substring(idx + mot.length()).trim();
                reste = reste.replaceFirst("^(est|:|le |la |les |un |une |du |de )", "").trim();
                if (!reste.isEmpty()) return capitaliser(reste);
            }
        }
        return "";
    }

    private boolean contient(String text, String... mots) {
        for (String mot : mots) if (text.contains(mot)) return true;
        return false;
    }

    private String capitaliser(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

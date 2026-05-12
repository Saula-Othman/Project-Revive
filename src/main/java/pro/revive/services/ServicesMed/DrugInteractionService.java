package pro.revive.services.ServicesMed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service de vérification des interactions médicamenteuses — Module 3.
 * - checkInteractions() : appel RxNorm → retourne List<Interaction>
 * - InteractionResult   : résultat structuré pour les tests unitaires
 */
public class DrugInteractionService {

    private static final String RXCUI_URL    = "https://rxnav.nlm.nih.gov/REST/rxcui.json?name=";
    private static final String INTERACT_URL = "https://rxnav.nlm.nih.gov/REST/interaction/list.json?rxcuis=";
    private static final int    TIMEOUT_MS   = 5000;

    // ── Interaction record ───────────────────────────────────────────────────

    public record Interaction(String drug1, String drug2, String description, String severity) {}

    // ── checkInteractions ────────────────────────────────────────────────────

    public List<Interaction> checkInteractions(String newDrug, List<String> existingDrugs) {
        List<Interaction> interactions = new ArrayList<>();
        if (newDrug == null || existingDrugs == null || existingDrugs.isEmpty())
            return interactions;
        try {
            String rxcuiNew = getRxCui(newDrug);
            if (rxcuiNew == null) return interactions;
            List<String> rxcuis = new ArrayList<>();
            rxcuis.add(rxcuiNew);
            for (String drug : existingDrugs) {
                String rxcui = getRxCui(drug);
                if (rxcui != null) rxcuis.add(rxcui);
            }
            if (rxcuis.size() < 2) return interactions;
            String json = httpGet(INTERACT_URL + String.join("+", rxcuis));
            interactions = parseInteractions(json, newDrug);
        } catch (Exception e) {
            System.err.println("[DrugInteractionService] Erreur: " + e.getMessage());
        }
        return interactions;
    }

    private String getRxCui(String drugName) {
        try {
            String encoded = URLEncoder.encode(drugName, StandardCharsets.UTF_8);
            String json    = httpGet(RXCUI_URL + encoded);
            if (json == null || json.isBlank()) return null;
            int idx = json.indexOf("\"rxnormId\":\"");
            if (idx < 0) return null;
            int start = idx + 12;
            int end   = json.indexOf("\"", start);
            if (end < 0) return null;
            String id = json.substring(start, end).trim();
            return id.isEmpty() || id.equals("null") ? null : id;
        } catch (Exception e) { return null; }
    }

    private List<Interaction> parseInteractions(String json, String newDrug) {
        List<Interaction> results = new ArrayList<>();
        if (json == null || json.isBlank()) return results;
        try {
            int pos = 0;
            while (true) {
                int descIdx = json.indexOf("\"description\":\"", pos);
                if (descIdx < 0) break;
                int start = descIdx + 15;
                int end   = json.indexOf("\"", start);
                if (end < 0) break;
                String desc     = json.substring(start, end).trim();
                String severity = extractNearby(json, descIdx, "\"severity\":\"");
                if (!desc.isEmpty())
                    results.add(new Interaction(newDrug, "medicament existant", desc, severity));
                pos = end + 1;
            }
        } catch (Exception e) {
            System.err.println("[DrugInteractionService] Parse: " + e.getMessage());
        }
        return results;
    }

    private String extractNearby(String json, int fromIdx, String key) {
        int searchEnd = Math.min(fromIdx + 500, json.length());
        String slice  = json.substring(fromIdx, searchEnd);
        int idx = slice.indexOf(key);
        if (idx < 0) return "inconnue";
        int start = idx + key.length();
        int end   = slice.indexOf("\"", start);
        return end < 0 ? "inconnue" : slice.substring(start, end).trim();
    }

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        if (conn.getResponseCode() != 200) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();
        return sb.toString();
    }

    // ── InteractionResult (tests unitaires) ──────────────────────────────────

    public static class InteractionResult {

        public final boolean      hasInteraction;
        public final boolean      erreurReseau;
        public final String       medicament;
        public final List<String> interactionsAvec;
        public final String       message;

        private InteractionResult(boolean hasInteraction, boolean erreurReseau,
                                  String medicament, List<String> interactionsAvec,
                                  String message) {
            this.hasInteraction   = hasInteraction;
            this.erreurReseau     = erreurReseau;
            this.medicament       = medicament;
            this.interactionsAvec = interactionsAvec != null
                                    ? Collections.unmodifiableList(interactionsAvec)
                                    : Collections.emptyList();
            this.message          = message;
        }

        public static InteractionResult aucune() {
            return new InteractionResult(false, false, null,
                    Collections.emptyList(), "Aucune interaction medicamenteuse detectee.");
        }

        public static InteractionResult erreurReseau() {
            return new InteractionResult(false, true, null,
                    Collections.emptyList(),
                    "Service de verification indisponible — veuillez reessayer.");
        }

        public static InteractionResult avecInteractions(String medicament,
                                                          List<String> interactions) {
            String msg = "Interactions detectees pour " + medicament + " avec : "
                       + String.join(", ", interactions);
            return new InteractionResult(true, false, medicament, interactions, msg);
        }
    }
}

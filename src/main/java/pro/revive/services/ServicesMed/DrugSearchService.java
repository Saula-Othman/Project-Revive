package pro.revive.services.ServicesMed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature 2 — Drug Autocomplete via RxNorm (gratuit, sans cle API).
 *
 * Strategie (3 tentatives) :
 *   1. approximateTerm  — meilleur pour noms partiels (amox -> amoxicillin)
 *   2. spellingsuggestions — suggestions orthographiques
 *   3. drugs.json       — recherche directe par nom exact
 */
public class DrugSearchService {

    // Endpoint 1 : approximateTerm (le plus fiable pour saisie partielle)
    private static final String APPROX_URL =
        "https://rxnav.nlm.nih.gov/REST/approximateTerm.json?term=";

    // Endpoint 2 : suggestions orthographiques
    private static final String SUGGEST_URL =
        "https://rxnav.nlm.nih.gov/REST/spellingsuggestions.json?name=";

    // Endpoint 3 : recherche directe
    private static final String DRUGS_URL =
        "https://rxnav.nlm.nih.gov/REST/drugs.json?name=";

    private static final int TIMEOUT_MS  = 5000;
    private static final int MAX_RESULTS = 10;

    public record DrugEntry(String name, String rxcui) {
        @Override public String toString() { return name; }
    }

    /**
     * Recherche des medicaments correspondant au terme saisi.
     * Essaie les 3 endpoints dans l'ordre jusqu'a obtenir des resultats.
     */
    public List<DrugEntry> search(String query) {
        if (query == null || query.trim().length() < 2) return new ArrayList<>();

        String q = query.trim();

        try {
            // Tentative 1 : approximateTerm (meilleur pour "amox", "para", etc.)
            List<DrugEntry> results = searchApproximate(q);
            if (!results.isEmpty()) return results;

            // Tentative 2 : suggestions orthographiques
            results = searchSuggestions(q);
            if (!results.isEmpty()) return results;

            // Tentative 3 : recherche directe
            results = searchDrugs(q);
            return results;

        } catch (Exception e) {
            System.err.println("[DrugSearchService] Erreur: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Endpoint 1 : approximateTerm ─────────────────────────────────────

    private List<DrugEntry> searchApproximate(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url     = APPROX_URL + encoded + "&maxEntries=" + MAX_RESULTS;
        String json    = httpGet(url);
        return parseApproximate(json);
    }

    /**
     * Parse :
     * {"approximateGroup":{"inputTerm":"amox","candidate":[
     *   {"rxcui":"723","score":"100","rank":"1","name":"amoxicillin"},
     *   ...
     * ]}}
     */
    private List<DrugEntry> parseApproximate(String json) {
        List<DrugEntry> results = new ArrayList<>();
        if (json == null || json.isBlank()) return results;
        try {
            // Chercher les blocs "rxcui":"..." et "name":"..."
            int pos = 0;
            while (results.size() < MAX_RESULTS) {
                int rxcuiIdx = json.indexOf("\"rxcui\":\"", pos);
                if (rxcuiIdx < 0) break;
                int rxcuiStart = rxcuiIdx + 9;
                int rxcuiEnd   = json.indexOf("\"", rxcuiStart);
                String rxcui   = json.substring(rxcuiStart, rxcuiEnd).trim();

                // Chercher le "name" dans le meme bloc candidat
                int nameIdx = json.indexOf("\"name\":\"", rxcuiEnd);
                if (nameIdx < 0) break;
                int nameStart = nameIdx + 8;
                int nameEnd   = json.indexOf("\"", nameStart);
                String name   = json.substring(nameStart, nameEnd).trim();

                if (!name.isEmpty() && !name.equals("null")) {
                    // Eviter les doublons
                    boolean exists = results.stream().anyMatch(d -> d.name().equalsIgnoreCase(name));
                    if (!exists) {
                        results.add(new DrugEntry(capitalize(name), rxcui));
                    }
                }
                pos = nameEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("[DrugSearchService] Parse approximate: " + e.getMessage());
        }
        return results;
    }

    // ── Endpoint 2 : spellingsuggestions ─────────────────────────────────

    private List<DrugEntry> searchSuggestions(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json    = httpGet(SUGGEST_URL + encoded);
        return parseSuggestions(json);
    }

    /**
     * Parse : {"suggestionGroup":{"name":null,"suggestionList":{"suggestion":["Drug1","Drug2"]}}}
     */
    private List<DrugEntry> parseSuggestions(String json) {
        List<DrugEntry> results = new ArrayList<>();
        if (json == null || json.isBlank()) return results;
        try {
            int idx = json.indexOf("\"suggestion\":[");
            if (idx < 0) return results;
            int start = json.indexOf('[', idx);
            int end   = json.indexOf(']', start);
            if (start < 0 || end < 0) return results;
            String arr = json.substring(start + 1, end);
            for (String item : arr.split(",")) {
                String name = item.trim().replace("\"", "");
                if (!name.isEmpty()) {
                    results.add(new DrugEntry(capitalize(name), null));
                    if (results.size() >= MAX_RESULTS) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[DrugSearchService] Parse suggestions: " + e.getMessage());
        }
        return results;
    }

    // ── Endpoint 3 : drugs.json ───────────────────────────────────────────

    private List<DrugEntry> searchDrugs(String query) throws Exception {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json    = httpGet(DRUGS_URL + encoded);
        return parseDrugs(json);
    }

    private List<DrugEntry> parseDrugs(String json) {
        List<DrugEntry> results = new ArrayList<>();
        if (json == null || json.isBlank()) return results;
        try {
            int pos = 0;
            while (results.size() < MAX_RESULTS) {
                int nameIdx = json.indexOf("\"name\":\"", pos);
                if (nameIdx < 0) break;
                int valStart = nameIdx + 8;
                int valEnd   = json.indexOf("\"", valStart);
                if (valEnd < 0) break;
                String name = json.substring(valStart, valEnd).trim();
                if (!name.isEmpty() && !name.equals("null")) {
                    results.add(new DrugEntry(capitalize(name), null));
                }
                pos = valEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("[DrugSearchService] Parse drugs: " + e.getMessage());
        }
        return results;
    }

    // ── HTTP helper ───────────────────────────────────────────────────────

    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(TIMEOUT_MS);
        conn.setReadTimeout(TIMEOUT_MS);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "REVIVE-Medical/1.0");
        int code = conn.getResponseCode();
        if (code != 200) {
            System.err.println("[DrugSearchService] HTTP " + code + " for: " + urlStr);
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        conn.disconnect();
        return sb.toString();
    }

    // ── Utilitaire ────────────────────────────────────────────────────────

    /** Met la premiere lettre en majuscule. */
    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }
}

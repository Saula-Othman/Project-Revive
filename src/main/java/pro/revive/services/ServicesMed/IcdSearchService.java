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
 * Feature 1 — ICD-10 Autocomplete
 * API gratuite NLM : https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search
 * Aucune cle API requise.
 *
 * Format reponse JSON :
 * [total, ["CODE1","CODE2",...], null, [["CODE1","Desc1"],["CODE2","Desc2"],...]]
 */
public class IcdSearchService {

    private static final String BASE_URL =
        "https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search";
    private static final int TIMEOUT_MS = 4000;
    private static final int MAX_RESULTS = 10;

    public record IcdEntry(String code, String description) {
        @Override
        public String toString() {
            return code + " — " + description;
        }
    }

    /**
     * Recherche des codes ICD-10 correspondant au terme saisi.
     * @param query terme de recherche (min 2 caracteres)
     * @return liste de IcdEntry, vide si erreur ou aucun resultat
     */
    public List<IcdEntry> search(String query) {
        List<IcdEntry> results = new ArrayList<>();
        if (query == null || query.trim().length() < 2) return results;

        try {
            String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
            String urlStr  = BASE_URL + "?terms=" + encoded + "&maxList=" + MAX_RESULTS;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status != 200) return results;

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            conn.disconnect();

            results = parseResponse(sb.toString());

        } catch (Exception e) {
            System.err.println("[IcdSearchService] Erreur: " + e.getMessage());
        }
        return results;
    }

    /**
     * Parse minimal du JSON sans librairie externe.
     * Format attendu : [N, ["C1","C2",...], null, [["C1","D1"],["C2","D2"],...]]
     */
    private List<IcdEntry> parseResponse(String json) {
        List<IcdEntry> results = new ArrayList<>();
        try {
            // Trouver le 4eme element : tableau de paires [code, description]
            // On cherche le dernier tableau de tableaux
            int lastOpen = json.lastIndexOf("[[");
            if (lastOpen < 0) return results;

            String pairsSection = json.substring(lastOpen + 1);
            // Extraire chaque paire ["CODE","Description"]
            int pos = 0;
            while (pos < pairsSection.length()) {
                int start = pairsSection.indexOf('[', pos);
                if (start < 0) break;
                int end = pairsSection.indexOf(']', start);
                if (end < 0) break;

                String pair = pairsSection.substring(start + 1, end);
                // pair = "\"J18.9\",\"Pneumonia, unspecified\""
                String[] parts = pair.split(",", 2);
                if (parts.length == 2) {
                    String code = parts[0].trim().replace("\"", "");
                    String desc = parts[1].trim().replace("\"", "");
                    if (!code.isEmpty() && !desc.isEmpty()) {
                        results.add(new IcdEntry(code, desc));
                    }
                }
                pos = end + 1;
                if (results.size() >= MAX_RESULTS) break;
            }
        } catch (Exception e) {
            System.err.println("[IcdSearchService] Parse error: " + e.getMessage());
        }
        return results;
    }
}

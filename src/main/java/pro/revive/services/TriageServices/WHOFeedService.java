package pro.revive.services.TriageServices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches real WHO Disease Outbreak News from the official WHO API.
 * Endpoint: https://www.who.int/api/news/diseaseoutbreaknews?$top=10&$orderby=PublicationDate desc
 */
public class WHOFeedService {

    private static final String API_URL =
            "https://www.who.int/api/news/diseaseoutbreaknews?$top=10&$orderby=PublicationDate%20desc";

    // Cache — refresh at most once every 10 minutes
    private static List<WHOAlert> cache = null;
    private static long lastFetchMs = 0;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

    // ── Inner class ──────────────────────────────────────────────

    public static class WHOAlert {
        public String    titre;
        public String    region;
        public String    description;
        public String    syndromeType;
        public boolean   isRecent;
        public LocalDate date;

        public WHOAlert(String titre, String region, String description,
                        String syndromeType, boolean isRecent, LocalDate date) {
            this.titre        = titre;
            this.region       = region;
            this.description  = description;
            this.syndromeType = syndromeType;
            this.isRecent     = isRecent;
            this.date         = date;
        }

        public boolean matchesSyndrome(String localSyndrome) {
            if (localSyndrome == null || syndromeType == null) return false;
            return syndromeType.equalsIgnoreCase(localSyndrome);
        }

        public String getSyndromeIcon() {
            if (syndromeType == null) return "●";
            switch (syndromeType) {
                case "Respiratoire":     return "◉";
                case "Digestif":         return "◆";
                case "Neurologique":     return "★";
                case "Cutane":           return "○";
                case "Cardiovasculaire": return "♥";
                case "Trauma":           return "✚";
                default:                 return "●";
            }
        }

        public String getSyndromeColor() {
            if (syndromeType == null) return "#64748B";
            switch (syndromeType) {
                case "Respiratoire":     return "#3B82F6";
                case "Digestif":         return "#F59E0B";
                case "Neurologique":     return "#8B5CF6";
                case "Cutane":           return "#EC4899";
                case "Cardiovasculaire": return "#EF4444";
                case "Trauma":           return "#64748B";
                default:                 return "#10B981";
            }
        }

        public String getDateFormatee() {
            if (date == null) return "N/A";
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Fetches the latest WHO disease outbreak alerts.
     * Returns an empty list on network failure so the UI degrades gracefully.
     */
    public List<WHOAlert> fetchAlerts() {
        long now = System.currentTimeMillis();
        if (cache != null && (now - lastFetchMs) < CACHE_TTL_MS) {
            return cache;
        }
        List<WHOAlert> alerts = new ArrayList<>();
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "REVIVE-Triage/1.0");

            int status = conn.getResponseCode();
            if (status != 200) {
                System.out.println("WHOFeedService: HTTP " + status);
                return cache != null ? cache : alerts;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
            }
            conn.disconnect();

            alerts = parseJson(sb.toString());
            cache = alerts;
            lastFetchMs = now;

        } catch (Exception e) {
            System.out.println("WHOFeedService: " + e.getMessage());
            if (cache != null) return cache; // return stale cache on error
        }
        return alerts;
    }

    // ── JSON parser (no external library — manual parsing) ───────

    private List<WHOAlert> parseJson(String json) {
        List<WHOAlert> alerts = new ArrayList<>();
        // Extract the "value" array
        int valueStart = json.indexOf("\"value\":[");
        if (valueStart == -1) return alerts;
        json = json.substring(valueStart + 9);

        // Split into individual objects by finding top-level { }
        List<String> objects = splitJsonObjects(json);

        LocalDate today = LocalDate.now();

        for (String obj : objects) {
            try {
                String titre       = extractString(obj, "Title");
                String overview    = extractString(obj, "Overview");
                String pubDateStr  = extractString(obj, "PublicationDate");

                if (titre == null || titre.isEmpty()) continue;

                // Clean HTML from overview
                String description = overview != null
                        ? overview.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim()
                        : "";
                if (description.length() > 300)
                    description = description.substring(0, 297) + "...";

                // Parse date
                LocalDate date = null;
                if (pubDateStr != null && pubDateStr.length() >= 10) {
                    try {
                        date = LocalDate.parse(pubDateStr.substring(0, 10));
                    } catch (Exception ignored) {}
                }

                // Extract region from title (text after " – " or " - ")
                String region = "Monde";
                if (titre.contains(" \u2013 ")) {
                    region = titre.substring(titre.lastIndexOf(" \u2013 ") + 3).trim();
                } else if (titre.contains(" - ")) {
                    region = titre.substring(titre.lastIndexOf(" - ") + 3).trim();
                }

                // Classify syndrome from title + description
                String syndrome = classifySyndrome(titre + " " + description);

                // Recent = published within last 30 days
                boolean recent = date != null && date.isAfter(today.minusDays(30));

                alerts.add(new WHOAlert(titre, region, description, syndrome, recent, date));

            } catch (Exception e) {
                // Skip malformed entry
            }
        }
        return alerts;
    }

    /**
     * Splits a JSON array string into individual top-level object strings.
     */
    private List<String> splitJsonObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objects.add(arrayContent.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    /**
     * Extracts a string value for a given key from a JSON object string.
     */
    private String extractString(String obj, String key) {
        String search = "\"" + key + "\":\"";
        int idx = obj.indexOf(search);
        if (idx == -1) return null;
        int start = idx + search.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < obj.length(); i++) {
            char c = obj.charAt(i);
            if (c == '"' && (i == 0 || obj.charAt(i - 1) != '\\')) break;
            if (c == '\\' && i + 1 < obj.length()) {
                char next = obj.charAt(i + 1);
                if (next == '"') { sb.append('"'); i++; continue; }
                if (next == 'n') { sb.append('\n'); i++; continue; }
                if (next == 't') { sb.append('\t'); i++; continue; }
                if (next == '\\') { sb.append('\\'); i++; continue; }
                if (next == 'u' && i + 5 < obj.length()) {
                    try {
                        int code = Integer.parseInt(obj.substring(i + 2, i + 6), 16);
                        sb.append((char) code);
                        i += 5;
                        continue;
                    } catch (Exception ignored) {}
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Classifies a syndrome category from text keywords.
     */
    private String classifySyndrome(String text) {
        if (text == null) return "Autre";
        String t = text.toLowerCase();
        if (t.contains("influenza") || t.contains("respiratory") || t.contains("pneumonia")
                || t.contains("covid") || t.contains("sars") || t.contains("mers")
                || t.contains("respirat") || t.contains("pulmon"))
            return "Respiratoire";
        if (t.contains("cholera") || t.contains("diarrhea") || t.contains("gastro")
                || t.contains("hepatitis") || t.contains("typhoid") || t.contains("digestif"))
            return "Digestif";
        if (t.contains("meningitis") || t.contains("encephalitis") || t.contains("neurolog")
                || t.contains("polio") || t.contains("rabies"))
            return "Neurologique";
        if (t.contains("ebola") || t.contains("marburg") || t.contains("hemorrhagic")
                || t.contains("dengue") || t.contains("yellow fever") || t.contains("monkeypox")
                || t.contains("mpox") || t.contains("skin") || t.contains("cutane"))
            return "Cutane";
        if (t.contains("cardiac") || t.contains("cardiovascular"))
            return "Cardiovasculaire";
        return "Autre";
    }
}

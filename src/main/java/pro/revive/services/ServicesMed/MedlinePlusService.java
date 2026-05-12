package pro.revive.services.ServicesMed;

import pro.revive.entities.EntitiesMed.MedlinePlusResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Service pour recuperer les informations de sante depuis MedlinePlus API.
 * API gratuite, sans cle, sans inscription.
 * Endpoint : https://connect.medlineplus.gov/service
 */
public class MedlinePlusService {

    private static final String API_URL = 
        "https://connect.medlineplus.gov/service?mainSearchCriteria.v.cs=2.16.840.1.113883.6.103&mainSearchCriteria.v.c=%s&knowledgeResponseType=application/json";
    
    private static final int TIMEOUT_SECONDS = 5;
    private static final int MAX_SUMMARY_LENGTH = 300;

    private final HttpClient httpClient;

    public MedlinePlusService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build();
    }

    /**
     * Recupere les informations de sante pour un code ICD-10.
     * Execution asynchrone via CompletableFuture.
     * 
     * @param icdCode Code ICD-10 (ex: "J18.9")
     * @return CompletableFuture contenant MedlinePlusResult
     */
    public CompletableFuture<MedlinePlusResult> fetchHealthInfoAsync(String icdCode) {
        return CompletableFuture.supplyAsync(() -> fetchHealthInfo(icdCode));
    }

    /**
     * Recupere les informations de sante pour un code ICD-10 (synchrone).
     * 
     * @param icdCode Code ICD-10 (ex: "J18.9")
     * @return MedlinePlusResult avec titre, resume et URL
     */
    public MedlinePlusResult fetchHealthInfo(String icdCode) {
        if (icdCode == null || icdCode.trim().isEmpty()) {
            return createDefaultResult(icdCode);
        }

        try {
            String url = String.format(API_URL, icdCode.trim());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, 
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResponse(response.body(), icdCode);
            } else {
                System.err.println("[MedlinePlus] HTTP " + response.statusCode() 
                        + " pour ICD: " + icdCode);
                return createDefaultResult(icdCode);
            }

        } catch (Exception e) {
            System.err.println("[MedlinePlus] Erreur pour ICD " + icdCode + ": " 
                    + e.getMessage());
            return createDefaultResult(icdCode);
        }
    }

    /**
     * Parse la reponse JSON de MedlinePlus.
     */
    private MedlinePlusResult parseResponse(String jsonBody, String icdCode) {
        try {
            JSONObject root = new JSONObject(jsonBody);
            
            // Verifier si feed existe et contient des entrees
            if (!root.has("feed")) {
                return createDefaultResult(icdCode);
            }

            JSONObject feed = root.getJSONObject("feed");
            if (!feed.has("entry")) {
                return createDefaultResult(icdCode);
            }

            JSONArray entries = feed.getJSONArray("entry");
            if (entries.length() == 0) {
                return createDefaultResult(icdCode);
            }

            // Recuperer la premiere entree
            JSONObject firstEntry = entries.getJSONObject(0);

            // Titre
            String title = icdCode;
            if (firstEntry.has("title")) {
                JSONObject titleObj = firstEntry.getJSONObject("title");
                if (titleObj.has("_value")) {
                    title = titleObj.getString("_value");
                }
            }

            // Resume
            String summary = "";
            if (firstEntry.has("summary")) {
                JSONObject summaryObj = firstEntry.getJSONObject("summary");
                if (summaryObj.has("_value")) {
                    summary = summaryObj.getString("_value");
                    // Limiter la longueur
                    if (summary.length() > MAX_SUMMARY_LENGTH) {
                        summary = summary.substring(0, MAX_SUMMARY_LENGTH) + "...";
                    }
                }
            }

            // URL
            String url = "";
            if (firstEntry.has("link")) {
                JSONArray links = firstEntry.getJSONArray("link");
                if (links.length() > 0) {
                    JSONObject firstLink = links.getJSONObject(0);
                    if (firstLink.has("href")) {
                        url = firstLink.getString("href");
                    }
                }
            }

            MedlinePlusResult result = new MedlinePlusResult(title, summary, url);
            System.out.println("[MedlinePlus] Info recuperee pour " + icdCode 
                    + ": " + title);
            return result;

        } catch (Exception e) {
            System.err.println("[MedlinePlus] Erreur de parsing pour " + icdCode 
                    + ": " + e.getMessage());
            return createDefaultResult(icdCode);
        }
    }

    /**
     * Cree un resultat par defaut si l'API echoue.
     */
    private MedlinePlusResult createDefaultResult(String icdCode) {
        String title = icdCode != null && !icdCode.trim().isEmpty() 
                ? icdCode : "Information non disponible";
        return new MedlinePlusResult(title, null, null);
    }
}

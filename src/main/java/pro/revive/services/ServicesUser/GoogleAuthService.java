package pro.revive.services.ServicesUser;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class GoogleAuthService {

    private static final String CLIENT_ID     = "id";
    private static final String CLIENT_SECRET = "key";
    private static final String REDIRECT_URI  = "http://localhost:9876/callback";
    private static final String SCOPE         = "openid email profile";

    private static final String AUTH_URL      = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL     = "https://oauth2.googleapis.com/token";

    /**
     * Lance le flow OAuth2 Google.
     * Ouvre le navigateur, attend le callback, retourne l'email Google ou null.
     */
    public static String authenticate() {
        try {
            // 1. Construire l'URL d'autorisation
            String state = Long.toHexString(System.currentTimeMillis());
            String authUrl = AUTH_URL
                    + "?client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8)
                    + "&state=" + state
                    + "&access_type=offline"
                    + "&prompt=select_account";

            // 2. Démarrer le serveur local pour recevoir le callback
            CountDownLatch latch = new CountDownLatch(1);
            String[] codeHolder = {null};

            HttpServer server = HttpServer.create(new InetSocketAddress(9876), 0);
            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            codeHolder[0] = URLDecoder.decode(param.substring(5), StandardCharsets.UTF_8);
                        }
                    }
                }
                // Réponse HTML de succès
                String response = "<html><body style='font-family:sans-serif;text-align:center;padding:60px;'>"
                        + "<h2 style='color:#1A56DB;'>✅ Authentification réussie !</h2>"
                        + "<p>Vous pouvez fermer cette fenêtre et retourner à REVIVE.</p>"
                        + "</body></html>";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                latch.countDown();
            });
            server.start();

            // 3. Ouvrir le navigateur
            try {
                // Windows
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", authUrl});
            } catch (Exception ex) {
                try {
                    // Fallback Desktop
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(new URI(authUrl));
                    } else {
                        System.err.println("Impossible d'ouvrir le navigateur.");
                        server.stop(0);
                        return null;
                    }
                } catch (Exception ex2) {
                    System.err.println("GoogleAuth: impossible d'ouvrir le navigateur: " + ex2.getMessage());
                    server.stop(0);
                    return null;
                }
            }

            // 4. Attendre le callback (max 2 minutes)
            boolean received = latch.await(2, TimeUnit.MINUTES);
            server.stop(1);

            if (!received || codeHolder[0] == null) {
                System.err.println("GoogleAuth: timeout ou code non recu. received=" + received + " code=" + codeHolder[0]);
                return null;
            }

            // 5. Échanger le code contre un token
            String accessToken = exchangeCodeForToken(codeHolder[0]);
            if (accessToken == null) return null;

            // 6. Récupérer l'email depuis le token
            return getEmailFromToken(accessToken);

        } catch (Exception e) {
            System.err.println("GoogleAuth error: " + e.getMessage());
            return null;
        }
    }

    private static String exchangeCodeForToken(String code) {
        try {
            URL url = new URL(TOKEN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&client_id=" + URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8)
                    + "&client_secret=" + URLEncoder.encode(CLIENT_SECRET, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                    + "&grant_type=authorization_code";

            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            if (json.has("access_token")) {
                return json.get("access_token").getAsString();
            }
        } catch (Exception e) {
            System.err.println("exchangeCodeForToken error: " + e.getMessage());
        }
        return null;
    }

    private static String getEmailFromToken(String accessToken) {
        try {
            URL url = new URL("https://www.googleapis.com/oauth2/v3/userinfo");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            if (json.has("email")) {
                return json.get("email").getAsString();
            }
        } catch (Exception e) {
            System.err.println("getEmailFromToken error: " + e.getMessage());
        }
        return null;
    }
}

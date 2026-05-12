package pro.revive.services.ServicesMateriel;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmailService {

    private final String username = "waflm21@gmail.com";
    private final String password = "crtvniylofexjmkx";

    // Date du dernier mail traité — on ne retraite pas les anciens
    private java.util.Date lastProcessedDate = null;

    // Dernier statut lisible pour l'UI
    private String lastStatus = "";

    /**
     * Teste uniquement la connexion IMAP sans toucher aux mails.
     * Lance une exception si la connexion échoue.
     */
    public void testConnection() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "10000");
        props.put("mail.imaps.connectiontimeout", "10000");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        try {
            store.connect("imap.gmail.com", username, password);
            System.out.println("[EmailService] ✅ Connexion Gmail réussie.");
        } finally {
            if (store.isConnected()) store.close();
        }
    }

    /**
     * Vérifie les mails non lus et retourne un EmailAlert si une localisation est détectée.
     * Retourne null si aucun mail ne contient une localisation valide.
     */
    public EmailAlert fetchLatestEmergencyAlert() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "15000");
        props.put("mail.imaps.connectiontimeout", "15000");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");

        try {
            store.connect("imap.gmail.com", username, password);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            int total = inbox.getMessageCount();
            System.out.println("[EmailService] 📬 Total mails INBOX : " + total);
            lastStatus = total + " mail(s) dans la boîte";

            if (total == 0) {
                lastStatus = "Boîte vide";
                inbox.close(false);
                return null;
            }

            // Lire les 10 derniers mails (lus ou non lus)
            int start = Math.max(1, total - 9);
            Message[] recent = inbox.getMessages(start, total);

            System.out.println("[EmailService] 🔍 Analyse des " + recent.length + " derniers mails...");
            lastStatus = "Analyse de " + recent.length + " mail(s)...";

            // Du plus récent au plus ancien
            for (int i = recent.length - 1; i >= 0; i--) {
                Message msg = recent[i];

                java.util.Date sentDate = msg.getSentDate();
                String sender  = msg.getFrom() != null ? msg.getFrom()[0].toString() : "Inconnu";
                String subject = msg.getSubject() != null ? msg.getSubject() : "(sans sujet)";

                // Ignorer les mails déjà traités (plus anciens que le dernier traité)
                if (lastProcessedDate != null && sentDate != null && !sentDate.after(lastProcessedDate)) {
                    System.out.println("[EmailService] ⏭ Ignoré (déjà traité) : " + subject);
                    continue;
                }

                String body    = getTextFromMessage(msg);
                String fullText = subject + "\n" + body;

                System.out.println("[EmailService] ── Mail " + (i+1) + " ──");
                System.out.println("  De      : " + sender);
                System.out.println("  Sujet   : " + subject);
                System.out.println("  Date    : " + sentDate);
                System.out.println("  Corps   : [" + body.substring(0, Math.min(body.length(), 300)).replace("\n", " ") + "]");

                String location = extractLocation(fullText);
                System.out.println("  Localisation : " + (location != null ? "✅ " + location : "❌ aucune"));

                if (location != null) {                    // Mémoriser la date pour ne pas retraiter ce mail
                    if (sentDate != null) lastProcessedDate = sentDate;

                    // Résoudre les liens Google Maps
                    if (location.startsWith("http")) {
                        System.out.println("[EmailService] 🔗 Résolution : " + location);
                        location = resolveMapsLink(location);
                        System.out.println("[EmailService] 📍 Résolu : " + location);
                    }

                    String preview = body.length() > 300 ? body.substring(0, 300) + "..." : body;
                    inbox.close(false);
                    return new EmailAlert(sender, subject, preview, location);
                }
            }

            inbox.close(false);
            lastStatus = "Aucune localisation dans les " + recent.length + " derniers mails";
            return null;

        } finally {
            if (store.isConnected()) store.close();
        }
    }

    /** Retourne le dernier statut lisible pour l'UI */
    public String getLastStatus() { return lastStatus; }

    // ─────────────────────────────────────────────────────────────────
    // Extraction de localisation
    // ─────────────────────────────────────────────────────────────────

    private String extractLocation(String text) {
        if (text == null || text.isBlank()) return null;

        // 1. Lien Google Maps court (maps.app.goo.gl) — capture tout jusqu'à un espace/retour ligne
        Pattern pShort = Pattern.compile("https?://maps\\.app\\.goo\\.gl/[^\\s\"<>\\[\\]]+", Pattern.CASE_INSENSITIVE);
        Matcher mShort = pShort.matcher(text);
        if (mShort.find()) return mShort.group(0);

        // 2. Lien Google Maps long
        Pattern pLong = Pattern.compile("https?://(?:www\\.)?google\\.com/maps[^\\s\"<>]+", Pattern.CASE_INSENSITIVE);
        Matcher mLong = pLong.matcher(text);
        if (mLong.find()) return mLong.group(0);

        // 3. Coordonnées GPS brutes (ex: 36.8065, 10.1815)
        Pattern pCoords = Pattern.compile("(-?\\d{1,3}\\.\\d{3,})[,\\s]+(-?\\d{1,3}\\.\\d{3,})");
        Matcher mCoords = pCoords.matcher(text);
        if (mCoords.find()) {
            double lat = Double.parseDouble(mCoords.group(1));
            double lon = Double.parseDouble(mCoords.group(2));
            if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                return mCoords.group(1) + "," + mCoords.group(2);
            }
        }

        // 4. Texte après mots-clés de localisation
        Pattern pAddr = Pattern.compile(
            "(?:je suis à|je suis a|localisation|adresse|position|urgence à|urgence a|lieu|location)\\s*[:\\-]?\\s*([^\\n]{5,100})",
            Pattern.CASE_INSENSITIVE
        );
        Matcher mAddr = pAddr.matcher(text);
        if (mAddr.find()) {
            String addr = mAddr.group(1).trim().replaceAll("[.,;!?]+$", "").trim();
            if (!addr.isBlank()) return addr;
        }

        return null;
    }

    private String resolveMapsLink(String url) {
        try {
            String current = url;
            for (int i = 0; i < 5; i++) {
                HttpURLConnection conn = (HttpURLConnection) new URL(current).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();
                int status = conn.getResponseCode();
                System.out.println("[EmailService] Redirect " + i + " HTTP " + status + " → " + current);
                if (status == 301 || status == 302 || status == 307 || status == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc == null) break;
                    current = loc;
                    conn.disconnect();
                } else {
                    conn.disconnect();
                    break;
                }
            }

            System.out.println("[EmailService] URL finale : " + current);

            // Format !3d<lat>!4d<lon>  (le plus courant dans les liens Google Maps)
            Pattern p2 = Pattern.compile("!3d(-?\\d+\\.\\d+).*?!4d(-?\\d+\\.\\d+)");
            Matcher m2 = p2.matcher(current);
            if (m2.find()) {
                System.out.println("[EmailService] Coords extraites (!3d/!4d) : " + m2.group(1) + "," + m2.group(2));
                return m2.group(1) + "," + m2.group(2);
            }

            // Format @<lat>,<lon>
            Pattern p1 = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher m1 = p1.matcher(current);
            if (m1.find()) {
                System.out.println("[EmailService] Coords extraites (@) : " + m1.group(1) + "," + m1.group(2));
                return m1.group(1) + "," + m1.group(2);
            }

            // Format ll=<lat>,<lon>
            Pattern p3 = Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher m3 = p3.matcher(current);
            if (m3.find()) return m3.group(1) + "," + m3.group(2);

            // Format q=<lat>,<lon>
            Pattern p4 = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher m4 = p4.matcher(current);
            if (m4.find()) return m4.group(1) + "," + m4.group(2);

            // Retourner l'URL résolue si aucune coordonnée extraite
            System.out.println("[EmailService] Aucune coordonnée extraite, retour URL résolue.");
            return current;
        } catch (Exception e) {
            System.err.println("[EmailService] Erreur résolution lien : " + e.getMessage());
            return url;
        }
    }

    private String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) return message.getContent().toString();
        if (message.isMimeType("text/html")) {
            return message.getContent().toString()
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("\\s+", " ").trim();
        }
        if (message.isMimeType("multipart/*")) {
            MimeMultipart mp = (MimeMultipart) message.getContent();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    sb.append(bp.getContent().toString());
                } else if (bp.isMimeType("text/html")) {
                    sb.append(bp.getContent().toString()
                            .replaceAll("<[^>]+>", " ")
                            .replaceAll("\\s+", " ").trim());
                }
            }
            return sb.toString();
        }
        return "";
    }
}

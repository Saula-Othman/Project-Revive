package pro.revive.tests.testsMateriel;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Diagnostic complet de la connexion Gmail et de l'extraction de localisation.
 * Lancer ce main() pour voir exactement ce qui se passe.
 */
public class EmailDiagnostic {

    private static final String USERNAME = "waflm21@gmail.com";
    private static final String PASSWORD = "crtvniylofexjmkx";

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║   REVIVE — Diagnostic Gmail / Email IA   ║");
        System.out.println("╚══════════════════════════════════════════╝\n");

        // ── Étape 1 : Connexion IMAP ──────────────────────────────────
        System.out.println("▶ [1] Connexion à Gmail IMAP...");
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", "imap.gmail.com");
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.timeout", "15000");
        props.put("mail.imaps.connectiontimeout", "15000");

        Store store = null;
        Folder inbox = null;

        try {
            Session session = Session.getInstance(props);
            store = session.getStore("imaps");
            store.connect("imap.gmail.com", USERNAME, PASSWORD);
            System.out.println("   ✅ Connexion réussie !\n");
        } catch (Exception e) {
            System.err.println("   ❌ ÉCHEC connexion : " + e.getMessage());
            System.err.println("   → Vérifiez que l'accès IMAP est activé dans Gmail");
            System.err.println("   → Vérifiez le mot de passe d'application (App Password)");
            return;
        }

        // ── Étape 2 : Ouvrir la boîte de réception ───────────────────
        System.out.println("▶ [2] Ouverture de la boîte de réception...");
        try {
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            int total = inbox.getMessageCount();
            int unread = inbox.getUnreadMessageCount();
            System.out.println("   ✅ INBOX ouverte — Total: " + total + " mails | Non lus: " + unread + "\n");
        } catch (Exception e) {
            System.err.println("   ❌ ÉCHEC ouverture INBOX : " + e.getMessage());
            return;
        }

        // ── Étape 3 : Lister TOUS les mails non lus ──────────────────
        System.out.println("▶ [3] Recherche des mails non lus...");
        try {
            Message[] unreadMessages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            System.out.println("   → " + unreadMessages.length + " mail(s) non lu(s) trouvé(s)\n");

            if (unreadMessages.length == 0) {
                System.out.println("   ⚠️  Aucun mail non lu !");
                System.out.println("   → Le mail que vous avez envoyé est peut-être déjà marqué comme lu.");
                System.out.println("   → Essayez de renvoyer un mail depuis une autre adresse.\n");

                // Afficher quand même les 3 derniers mails (lus ou non)
                System.out.println("▶ [3b] Affichage des 3 derniers mails (tous) :");
                int total = inbox.getMessageCount();
                int start = Math.max(1, total - 2);
                Message[] recent = inbox.getMessages(start, total);
                for (Message m : recent) {
                    System.out.println("   ─────────────────────────────────────");
                    System.out.println("   De      : " + (m.getFrom() != null ? m.getFrom()[0] : "?"));
                    System.out.println("   Sujet   : " + m.getSubject());
                    System.out.println("   Date    : " + m.getSentDate());
                    System.out.println("   Lu      : " + m.isSet(Flags.Flag.SEEN));
                    String body = getTextFromMessage(m);
                    System.out.println("   Corps   : " + body.substring(0, Math.min(body.length(), 200)));
                    String loc = extractLocation(m.getSubject() + "\n" + body);
                    System.out.println("   Localisation extraite : " + (loc != null ? "✅ " + loc : "❌ aucune"));
                }
            } else {
                // ── Étape 4 : Analyser chaque mail non lu ────────────
                System.out.println("▶ [4] Analyse de chaque mail non lu :");
                for (int i = 0; i < unreadMessages.length; i++) {
                    Message msg = unreadMessages[i];
                    System.out.println("\n   ══ Mail #" + (i + 1) + " ══");
                    System.out.println("   De      : " + (msg.getFrom() != null ? msg.getFrom()[0] : "?"));
                    System.out.println("   Sujet   : " + msg.getSubject());
                    System.out.println("   Date    : " + msg.getSentDate());
                    System.out.println("   Type    : " + msg.getContentType());

                    String body = getTextFromMessage(msg);
                    System.out.println("   Corps   : " + body.substring(0, Math.min(body.length(), 400)));

                    String fullText = (msg.getSubject() != null ? msg.getSubject() : "") + "\n" + body;
                    String loc = extractLocation(fullText);

                    if (loc != null) {
                        System.out.println("   ✅ Localisation brute : " + loc);
                        if (loc.startsWith("http")) {
                            System.out.println("   → Résolution du lien Maps...");
                            String resolved = resolveMapsLink(loc);
                            System.out.println("   ✅ Localisation résolue : " + resolved);
                        }
                    } else {
                        System.out.println("   ❌ Aucune localisation détectée dans ce mail.");
                        System.out.println("   → Formats supportés :");
                        System.out.println("      • Lien Google Maps : https://maps.app.goo.gl/...");
                        System.out.println("      • Coordonnées GPS  : 36.8065, 10.1815");
                        System.out.println("      • Texte            : je suis à [adresse]");
                    }
                }
            }

            inbox.close(false);
            store.close();

        } catch (Exception e) {
            System.err.println("   ❌ Erreur lecture mails : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n╔══════════════════════════════════════════╗");
        System.out.println("║              Diagnostic terminé          ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }

    // ── Méthodes copiées depuis EmailService pour test isolé ─────────

    private static String extractLocation(String text) {
        if (text == null || text.isBlank()) return null;

        // 1. Lien Google Maps court — capture tout jusqu'à un espace/retour ligne
        Pattern pShort = Pattern.compile("https?://maps\\.app\\.goo\\.gl/[^\\s\"<>\\[\\]]+", Pattern.CASE_INSENSITIVE);
        Matcher mShort = pShort.matcher(text);
        if (mShort.find()) return mShort.group(0);

        Pattern pLong = Pattern.compile("https?://(?:www\\.)?google\\.com/maps[^\\s\"<>]+", Pattern.CASE_INSENSITIVE);
        Matcher mLong = pLong.matcher(text);
        if (mLong.find()) return mLong.group(0);

        Pattern pCoords = Pattern.compile("(-?\\d{1,3}\\.\\d{3,})[,\\s]+(-?\\d{1,3}\\.\\d{3,})");
        Matcher mCoords = pCoords.matcher(text);
        if (mCoords.find()) {
            double lat = Double.parseDouble(mCoords.group(1));
            double lon = Double.parseDouble(mCoords.group(2));
            if (lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                return mCoords.group(1) + "," + mCoords.group(2);
            }
        }

        Pattern pAddr = Pattern.compile(
            "(?:je suis à|je suis a|localisation|adresse|position|urgence à|urgence a|lieu)\\s*[:\\-]?\\s*([^\\n]{5,100})",
            Pattern.CASE_INSENSITIVE
        );
        Matcher mAddr = pAddr.matcher(text);
        if (mAddr.find()) {
            String addr = mAddr.group(1).trim().replaceAll("[.,;!?]+$", "").trim();
            if (!addr.isBlank()) return addr;
        }

        return null;
    }

    private static String resolveMapsLink(String url) {
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
                System.out.println("      Redirect " + i + " — HTTP " + status + " → " + current);
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
            System.out.println("      URL finale : " + current);

            // !3d<lat>!4d<lon> — format le plus courant
            Pattern p2 = Pattern.compile("!3d(-?\\d+\\.\\d+).*?!4d(-?\\d+\\.\\d+)");
            Matcher m2 = p2.matcher(current);
            if (m2.find()) return m2.group(1) + "," + m2.group(2);

            Pattern p1 = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher m1 = p1.matcher(current);
            if (m1.find()) return m1.group(1) + "," + m1.group(2);

            Pattern p3 = Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher m3 = p3.matcher(current);
            if (m3.find()) return m3.group(1) + "," + m3.group(2);

            Pattern p4 = Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            Matcher m4 = p4.matcher(current);
            if (m4.find()) return m4.group(1) + "," + m4.group(2);

            return current;
        } catch (Exception e) {
            System.err.println("      ❌ Erreur résolution : " + e.getMessage());
            return url;
        }
    }

    private static String getTextFromMessage(Message message) throws Exception {
        if (message.isMimeType("text/plain")) return message.getContent().toString();
        if (message.isMimeType("text/html")) {
            return message.getContent().toString().replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
        }
        if (message.isMimeType("multipart/*")) {
            MimeMultipart mp = (MimeMultipart) message.getContent();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) sb.append(bp.getContent().toString());
                else if (bp.isMimeType("text/html")) {
                    sb.append(bp.getContent().toString().replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim());
                }
            }
            return sb.toString();
        }
        return "";
    }
}

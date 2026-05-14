package pro.revive.services.ServicesMed;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import pro.revive.entities.EntitiesMed.AdviceData;
import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.MedlinePlusResult;
import pro.revive.entities.EntitiesMed.Ordonnance;
import okhttp3.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service d'envoi d'emails post-consultation via l'API Brevo (HTTP).
 * Pas de restriction IP, pas de SMTP — utilise OkHttp + Gson.
 */
public class PatientEmailService {

    private static final String BREVO_API_URL    = "https://api.brevo.com/v3/smtp/email";
    private static final String BREVO_API_KEY    = "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";
    private static final String BREVO_SENDER_EMAIL = "siwarsoltani791@gmail.com";
    private static final String BREVO_SENDER_NAME  = "Urgences REVIVE";
    private static final String HOSPITAL_NAME    = "Urgences REVIVE";
    private static final String HOSPITAL_PHONE   = "+216 93 039 166";

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OkHttpClient httpClient;

    public PatientEmailService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Envoie un email de resume post-consultation via l'API Brevo.
     * Ne lance jamais d'exception — gestion interne des erreurs.
     */
    public void sendPostConsultationEmail(
            Map<String, Object> patient,
            Consultation consultation,
            Map<String, Object> admission,
            List<Ordonnance> ordonnances,
            MedlinePlusResult medlineInfo,
            AdviceData adviceData) {

        try {
            // Validation email patient
            String emailDest = (String) patient.get("email");
            System.out.println("[Email] Destinataire : " + emailDest);
            if (emailDest == null || emailDest.trim().isEmpty()) {
                System.out.println("[Email] Patient sans email, envoi ignore.");
                return;
            }

            String apiKey      = BREVO_API_KEY;
            String senderEmail = BREVO_SENDER_EMAIL;
            String senderName  = BREVO_SENDER_NAME;

            // Construction du corps HTML
            String htmlContent = buildHtmlEmail(patient, consultation, admission,
                    ordonnances, medlineInfo, adviceData);

            String dateConsult = consultation.getDateHeureDebut() != null
                    ? consultation.getDateHeureDebut().format(DATE_FMT) : "";
            String subject = "REVIVE - Resume de votre consultation du " + dateConsult;

            // Construction JSON pour l'API Brevo
            JsonObject body = new JsonObject();

            JsonObject sender = new JsonObject();
            sender.addProperty("name",  senderName);
            sender.addProperty("email", senderEmail);
            body.add("sender", sender);

            JsonArray to = new JsonArray();
            JsonObject recipient = new JsonObject();
            String prenom = patient.get("prenom") != null ? patient.get("prenom").toString() : "";
            String nom    = patient.get("nom")    != null ? patient.get("nom").toString()    : "";
            recipient.addProperty("email", emailDest.trim());
            recipient.addProperty("name",  (prenom + " " + nom).trim());
            to.add(recipient);
            body.add("to", to);

            body.addProperty("subject",      subject);
            body.addProperty("htmlContent",  htmlContent);

            // Appel API
            RequestBody requestBody = RequestBody.create(
                    body.toString(),
                    MediaType.get("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(BREVO_API_URL)
                    .addHeader("api-key",      apiKey)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept",       "application/json")
                    .post(requestBody)
                    .build();

            System.out.println("[Email] Envoi via API Brevo...");
            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    System.out.println("[Email] \u2705 Email envoye avec succes a " + emailDest);
                } else {
                    System.err.println("[Email] \u274C Erreur API Brevo " + response.code() + " : " + responseBody);
                }
            }

        } catch (Exception e) {
            System.err.println("[Email] Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Construction HTML ─────────────────────────────────────────────────

    private String buildHtmlEmail(
            Map<String, Object> patient,
            Consultation consultation,
            Map<String, Object> admission,
            List<Ordonnance> ordonnances,
            MedlinePlusResult medlineInfo,
            AdviceData adviceData) {

        StringBuilder html = new StringBuilder();
        String prenom      = escapeHtml((String) patient.get("prenom"));
        String dateDebut   = consultation.getDateHeureDebut() != null
                ? consultation.getDateHeureDebut().format(DATETIME_FMT) : "\u2014";
        String hospitalName  = HOSPITAL_NAME;
        String hospitalPhone = HOSPITAL_PHONE;

        html.append("<!DOCTYPE html><html lang='fr'><head>")
            .append("<meta charset='UTF-8'>")
            .append("<meta name='viewport' content='width=device-width,initial-scale=1.0'>")
            .append("<title>Resume Consultation</title><style>")
            .append("body{margin:0;padding:20px;background:#f0f4f8;font-family:'Segoe UI',sans-serif;}")
            .append(".wrap{max-width:600px;margin:0 auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 12px rgba(0,0,0,.1);}")
            .append(".hdr{background:linear-gradient(135deg,#1a4f7a,#1a6b5a);padding:32px 28px;text-align:center;color:#fff;}")
            .append(".hdr .logo{font-size:24px;font-weight:700;margin-bottom:8px;}")
            .append(".hdr .sub{font-size:14px;opacity:.9;margin-bottom:8px;}")
            .append(".hdr .dt{font-size:12px;opacity:.8;}")
            .append(".greet{padding:24px 28px;}")
            .append(".greet h2{margin:0 0 10px;color:#1a1a2e;font-size:18px;}")
            .append(".greet p{margin:0;color:#475569;font-size:14px;line-height:1.6;}")
            .append(".card{margin:0 20px 16px;padding:20px 24px;border-radius:12px;border:1px solid #e2e8f0;}")
            .append(".card-blue{background:#EFF6FF;border-left:4px solid #2563eb;}")
            .append(".card-green{background:#F0FDF4;border-left:4px solid #16a34a;}")
            .append(".card-teal{background:#F0FDFA;border-left:4px solid #0ea5a0;}")
            .append(".card-red{background:#FEF2F2;border-left:4px solid #e05252;}")
            .append(".card-title{font-size:15px;font-weight:700;color:#1a1a2e;margin:0 0 14px;}")
            .append(".disease{font-size:17px;font-weight:700;color:#1a1a2e;margin-bottom:4px;}")
            .append(".icd{font-size:11px;color:#94a3b8;margin-bottom:10px;}")
            .append(".summary{color:#475569;font-size:13px;line-height:1.6;margin-bottom:14px;}")
            .append(".btn{display:inline-block;padding:9px 18px;background:#2563eb;color:#fff;text-decoration:none;border-radius:8px;font-size:13px;font-weight:600;}")
            .append(".vitals{display:table;width:100%;border-spacing:8px;}")
            .append(".vrow{display:table-row;}")
            .append(".vcell{display:table-cell;width:50%;padding:10px;background:#f8fafc;border-radius:8px;vertical-align:top;}")
            .append(".vlabel{font-size:11px;color:#64748b;margin-bottom:3px;}")
            .append(".vval{font-size:15px;font-weight:700;}")
            .append(".ok{color:#16a34a;}.warn{color:#d97706;}.crit{color:#dc2626;}")
            .append(".med{padding:10px 12px;background:#f8fafc;border-radius:8px;margin-bottom:8px;font-size:13px;color:#1a1a2e;}")
            .append(".wbox{background:#fef3c7;border:1px solid #fbbf24;border-radius:8px;padding:10px 14px;margin-top:10px;}")
            .append(".wbox p{margin:0;color:#92400e;font-size:13px;font-weight:600;}")
            .append("ul.alist{margin:0;padding-left:18px;}")
            .append("ul.alist li{margin-bottom:8px;color:#475569;font-size:13px;line-height:1.5;}")
            .append(".ftr{background:#1a4f7a;color:#fff;text-align:center;padding:22px 28px;}")
            .append(".ftr p{margin:5px 0;font-size:13px;opacity:.9;}")
            .append(".ftr .note{font-size:11px;opacity:.7;margin-top:10px;}")
            .append("</style></head><body><div class='wrap'>");

        // HEADER
        html.append("<div class='hdr'>")
            .append("<div class='logo'>\uD83C\uDFE5 REVIVE</div>")
            .append("<div class='sub'>Service des Urgences \u2014 R\u00e9sum\u00e9 de consultation</div>")
            .append("<div class='dt'>").append(dateDebut).append("</div>")
            .append("</div>");

        // GREETING
        html.append("<div class='greet'>")
            .append("<h2>Bonjour ").append(escapeHtml(prenom)).append(",</h2>")
            .append("<p>Votre consultation du ").append(dateDebut)
            .append(" est maintenant cl\u00f4tur\u00e9e. Voici un r\u00e9sum\u00e9 de votre \u00e9tat de sant\u00e9.</p>")
            .append("</div>");

        // DIAGNOSTIC
        html.append("<div class='card card-blue'>")
            .append("<div class='card-title'>\uD83D\uDCCB Votre Diagnostic</div>")
            .append("<div class='disease'>").append(escapeHtml(medlineInfo.getDiseaseTitle())).append("</div>");
        if (consultation.getIcdCode() != null && !consultation.getIcdCode().isEmpty())
            html.append("<div class='icd'>Code ICD-10 : ").append(consultation.getIcdCode()).append("</div>");
        if (medlineInfo.getSummary() != null && !medlineInfo.getSummary().isEmpty())
            html.append("<div class='summary'>").append(escapeHtml(medlineInfo.getSummary())).append("</div>");
        if (medlineInfo.getFullInfoUrl() != null && !medlineInfo.getFullInfoUrl().isEmpty())
            html.append("<a href='").append(medlineInfo.getFullInfoUrl()).append("' class='btn'>En savoir plus \u2192</a>");
        html.append("</div>");

        // CONSTANTES VITALES
        if (admission != null && hasVitals(admission)) {
            html.append("<div class='card'>")
                .append("<div class='card-title'>\uD83D\uDCCA Vos Constantes \u00e0 la Sortie</div>")
                .append("<table width='100%' cellspacing='8' cellpadding='0'><tr>");
            int col = 0;
            col += vitalCell(html, "\u2764\uFE0F Pouls",          admission.get("constancesPouls"),      "bpm", 60,100,50,110);
            col += vitalCell(html, "\uD83C\uDF21\uFE0F Temp.",     admission.get("constancesTemperature"),"°C",  36.5,37.5,35,38.5);
            if (col == 2) { html.append("</tr><tr>"); col = 0; }
            col += vitalCell(html, "\uD83E\uDEC7 SpO2",            admission.get("spo2"),                 "%",   95,100,90,94);
            col += vitalCell(html, "\uD83E\uDE78 Glyc\u00e9mie",   admission.get("glycemie"),             "g/L", 0.7,1.1,0.6,1.4);
            if (col == 2) { html.append("</tr><tr>"); col = 0; }
            col += vitalCell(html, "\uD83D\uDE23 Douleur",         admission.get("scoreDouleur"),         "/10", 0,3,4,10);
            vitalCell(html,        "\uD83E\uDEC1 Fr\u00e9q. Resp.",admission.get("frequenceRespiratoire"),"/min",12,20,10,25);
            html.append("</tr></table></div>");
        }

        // ORDONNANCES
        if (ordonnances != null && !ordonnances.isEmpty()) {
            html.append("<div class='card card-green'>")
                .append("<div class='card-title'>\uD83D\uDC8A Votre Traitement Prescrit</div>");
            for (Ordonnance o : ordonnances) {
                html.append("<div class='med'>\u2022 <strong>")
                    .append(escapeHtml(o.getMedicament())).append("</strong> \u2014 ")
                    .append(escapeHtml(o.getPosologie())).append(" \u2014 ")
                    .append(o.getDureeJours()).append(" jours</div>");
            }
            html.append("<div class='wbox'><p>\u26A0\uFE0F Prenez vos m\u00e9dicaments jusqu'au bout, m\u00eame si vous vous sentez mieux.</p></div>")
                .append("</div>");
        }

        // CONSEILS
        if (adviceData != null && adviceData.getConseils() != null && !adviceData.getConseils().isEmpty()) {
            html.append("<div class='card card-teal'>")
                .append("<div class='card-title'>\u2705 Nos Conseils Pour Vous</div>")
                .append("<ul class='alist'>");
            for (String c : adviceData.getConseils())
                html.append("<li>\u2192 ").append(escapeHtml(c)).append("</li>");
            html.append("</ul></div>");
        }

        // ALERTES
        if (adviceData != null && adviceData.getAlertes() != null && !adviceData.getAlertes().isEmpty()) {
            html.append("<div class='card card-red'>")
                .append("<div class='card-title'>\u26A0\uFE0F Revenez en Urgence Si :</div>")
                .append("<ul class='alist'>");
            for (String a : adviceData.getAlertes())
                html.append("<li>").append(escapeHtml(a)).append("</li>");
            html.append("</ul></div>");
        }

        // FOOTER
        html.append("<div class='ftr'>")
            .append("<p>L'\u00e9quipe m\u00e9dicale \u2014 ").append(hospitalName).append("</p>")
            .append("<p>\uD83D\uDCDE ").append(hospitalPhone).append("</p>")
            .append("<p class='note'>Cet email est g\u00e9n\u00e9r\u00e9 automatiquement. Ne pas r\u00e9pondre.</p>")
            .append("</div></div></body></html>");

        return html.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean hasVitals(Map<String, Object> a) {
        return a.get("constancesPouls") != null || a.get("constancesTemperature") != null
            || a.get("spo2") != null || a.get("glycemie") != null
            || a.get("scoreDouleur") != null || a.get("frequenceRespiratoire") != null;
    }

    /** Retourne 1 si la cellule a ete ajoutee, 0 sinon. */
    private int vitalCell(StringBuilder html, String label, Object value, String unit,
                           double nMin, double nMax, double wMin, double wMax) {
        if (value == null) return 0;
        try {
            double v = Double.parseDouble(value.toString());
            String cls = (v < wMin || v > wMax) ? "crit" : (v < nMin || v > nMax) ? "warn" : "ok";
            html.append("<td width='50%' style='padding:8px;background:#f8fafc;border-radius:8px;vertical-align:top;'>")
                .append("<div style='font-size:11px;color:#64748b;margin-bottom:3px;'>").append(label).append("</div>")
                .append("<div style='font-size:15px;font-weight:700;' class='").append(cls).append("'>")
                .append(fmt(v)).append(" ").append(unit).append("</div></td>");
            return 1;
        } catch (NumberFormatException e) { return 0; }
    }

    private String fmt(double v) {
        return v == (long) v ? String.valueOf((long) v) : String.format("%.1f", v);
    }

    private String escapeHtml(String t) {
        if (t == null) return "";
        return t.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }

    private String sanitize(String s) { return s == null ? "" : s.replace("<","&lt;").replace(">","&gt;"); }
}

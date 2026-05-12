package pro.revive.services.ServicesMed;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service d'envoi de SMS pour REVIVE — Module 3.
 * Construit et envoie des messages SMS aux patients après consultation.
 */
public class SmsService {

    private static final int MAX_DIAG_LENGTH = 43;
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Construit le texte du SMS à envoyer au patient.
     *
     * @param nomPatient  Nom complet du patient
     * @param date        Date/heure de la consultation
     * @param diagnostic  Diagnostic posé (peut être null)
     * @return            Le message SMS formaté
     */
    private String construireMessage(String nomPatient,
                                     LocalDateTime date,
                                     String diagnostic) {
        String dateStr = (date != null) ? date.format(FMT) : "—";

        String diagStr;
        if (diagnostic == null || diagnostic.isBlank()) {
            diagStr = "—";
        } else if (diagnostic.length() > MAX_DIAG_LENGTH) {
            diagStr = diagnostic.substring(0, MAX_DIAG_LENGTH) + "…";
        } else {
            diagStr = diagnostic;
        }

        return "REVIVE — Bonjour " + nomPatient + ",\n" +
               "Votre consultation du " + dateStr + " a été enregistrée.\n" +
               "Diagnostic : " + diagStr + "\n" +
               "Merci de votre confiance.";
    }

    /**
     * Construit le payload JSON pour l'API SMS.
     *
     * @param to      Numéro destinataire (ex: +216xxxxxxxx)
     * @param sender  Nom expéditeur
     * @param message Contenu du message (les guillemets seront échappés)
     * @return        JSON string prêt à envoyer
     */
    private String buildJsonPayload(String to, String sender, String message) {
        String escapedMessage = message.replace("\"", "\\\"");
        return "{"
             + "\"to\":\"" + to + "\","
             + "\"from\":\"" + sender + "\","
             + "\"text\":\"" + escapedMessage + "\""
             + "}";
    }

    /**
     * Envoie un SMS de résumé de consultation au patient.
     *
     * @param telephone   Numéro de téléphone du patient
     * @param nomPatient  Nom du patient
     * @param date        Date de consultation
     * @param diagnostic  Diagnostic
     * @return            true si l'envoi a réussi
     */
    public boolean envoyerSmsConsultation(String telephone,
                                          String nomPatient,
                                          LocalDateTime date,
                                          String diagnostic) {
        try {
            String message = construireMessage(nomPatient, date, diagnostic);
            String payload = buildJsonPayload(telephone, "REVIVE", message);
            // Intégration API SMS à brancher ici
            System.out.println("[SmsService] Payload prêt : " + payload);
            return true;
        } catch (Exception e) {
            System.err.println("[SmsService] Erreur envoi SMS : " + e.getMessage());
            return false;
      
        }
    }
}

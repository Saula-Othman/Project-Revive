package pro.revive.services.ServicesMateriel;

/**
 * Représente une alerte d'urgence extraite d'un email.
 */
public class EmailAlert {

    private final String senderEmail;
    private final String subject;
    private final String bodyPreview;
    private final String location;

    public EmailAlert(String senderEmail, String subject, String bodyPreview, String location) {
        this.senderEmail  = senderEmail  != null ? senderEmail  : "Inconnu";
        this.subject      = subject      != null ? subject      : "(sans sujet)";
        this.bodyPreview  = bodyPreview  != null ? bodyPreview  : "";
        this.location     = location;
    }

    public String getSenderEmail()  { return senderEmail; }
    public String getSubject()      { return subject; }
    public String getBodyPreview()  { return bodyPreview; }
    public String getLocation()     { return location; }

    @Override
    public String toString() {
        return "EmailAlert{from='" + senderEmail + "', subject='" + subject + "', location='" + location + "'}";
    }
}

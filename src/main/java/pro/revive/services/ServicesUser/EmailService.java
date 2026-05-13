package pro.revive.services.ServicesUser;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import pro.revive.entities.EntitiesUser.Personne;

import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST    = "smtp.gmail.com";
    private static final int    SMTP_PORT    = 465;
    private static final String SENDER_EMAIL = "boughzala.medsalah@gmail.com";
    private static final String SENDER_PWD   = "dxoxylee zplpuutw";

    // ══════════════════════════════════════════
    // Email de confirmation d'inscription
    // ══════════════════════════════════════════
    public static void sendConfirmationEmail(Personne personne) {
        if (personne.getEmail() == null || personne.getEmail().isBlank()) {
            System.err.println("EmailService: adresse email vide, envoi annule.");
            return;
        }
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Systeme de Gestion"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(personne.getEmail()));
            message.setSubject("Confirmation de votre inscription - REVIVE");
            message.setContent(buildConfirmationBody(personne), "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Email de confirmation envoye a: " + personne.getEmail());
        } catch (Exception e) {
            System.err.println("sendConfirmationEmail error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email de code de reinitialisation
    // ══════════════════════════════════════════
    public static void sendResetCode(String toEmail, String nom, String prenom, String code) {
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Securite"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Code de reinitialisation - REVIVE");
            message.setContent(buildResetBody(nom, prenom, code), "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Code de reset envoye a: " + toEmail);
        } catch (Exception e) {
            System.err.println("sendResetCode error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email notification to admin for new signup request
    // ══════════════════════════════════════════
    public static void sendAdminNotification(String adminEmail, String nom, String prenom, String role) {
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Inscriptions"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(adminEmail));
            message.setSubject("Nouvelle demande d'inscription - REVIVE");
            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
                + "<div style='max-width:500px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#0B4EA2;padding:24px 32px;'><h1 style='color:#fff;margin:0;font-size:22px;'>REVIVE</h1>"
                + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:12px;'>Nouvelle demande d'inscription</p></div>"
                + "<div style='padding:28px;'>"
                + "<p style='color:#1A1D23;font-size:15px;'>Une nouvelle demande d'inscription a ete soumise :</p>"
                + "<div style='background:#F8FAFC;border:1.5px solid #E5E7EB;border-radius:10px;padding:16px;margin:16px 0;'>"
                + "<p style='margin:4px 0;font-size:13px;'><b>Nom :</b> " + prenom + " " + nom + "</p>"
                + "<p style='margin:4px 0;font-size:13px;'><b>Role :</b> " + role + "</p></div>"
                + "<p style='color:#6B7280;font-size:12px;'>Connectez-vous a l'interface admin pour accepter ou refuser cette demande.</p>"
                + "</div></div></body></html>";
            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Notification admin envoyee a: " + adminEmail);
        } catch (Exception e) {
            System.err.println("sendAdminNotification error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email approval — send credentials to agent
    // ══════════════════════════════════════════
    public static void sendApprovalEmail(Personne personne) {
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Inscription"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(personne.getEmail()));
            message.setSubject("Votre inscription a ete acceptee - REVIVE");
            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#059669;padding:28px 32px;'><h1 style='color:#fff;margin:0;font-size:24px;'>REVIVE</h1>"
                + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:13px;'>Inscription acceptee !</p></div>"
                + "<div style='padding:32px;'>"
                + "<h2 style='color:#1A1D23;font-size:18px;margin:0 0 8px;'>Bienvenue, " + personne.getPrenom() + " " + personne.getNom() + " !</h2>"
                + "<p style='color:#6B7280;font-size:13px;margin:0 0 20px;'>Votre demande d'inscription a ete acceptee. Voici vos identifiants temporaires :</p>"
                + "<div style='background:#F9FAFB;border:1.5px solid #E5E7EB;border-radius:12px;padding:20px;margin-bottom:20px;'>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Identifiant</td>"
                + "<td style='color:#0B4EA2;font-weight:bold;font-size:14px;text-align:right;'>" + personne.getIdentifiant() + "</td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Mot de passe temporaire</td>"
                + "<td style='color:#1A1D23;font-weight:bold;font-size:14px;text-align:right;'>" + personne.getMotDePasse() + "</td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Role</td>"
                + "<td style='color:#1A1D23;font-size:13px;text-align:right;'>" + personne.getRole() + "</td></tr>"
                + "</table></div>"
                + "<p style='color:#EF4444;font-size:12px;'>Lors de votre premiere connexion, vous serez oblige de changer votre mot de passe.</p>"
                + "</div></div></body></html>";
            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Email approbation envoye a: " + personne.getEmail());
        } catch (Exception e) {
            System.err.println("sendApprovalEmail error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email rejection
    // ══════════════════════════════════════════
    public static void sendRejectionEmail(String toEmail, String nom, String prenom) {
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Inscription"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Votre demande d'inscription - REVIVE");
            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
                + "<div style='max-width:500px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#38BDF8;padding:24px 32px;'><h1 style='color:#fff;margin:0;font-size:22px;'>REVIVE</h1></div>"
                + "<div style='padding:28px;'>"
                + "<p style='color:#1A1D23;font-size:15px;'>Bonjour <b>" + prenom + " " + nom + "</b>,</p>"
                + "<p style='color:#6B7280;font-size:13px;'>Nous sommes desoles, votre demande d'inscription n'a pas ete acceptee.</p>"
                + "<p style='color:#6B7280;font-size:13px;'>Pour plus d'informations, veuillez contacter l'administration.</p>"
                + "</div></div></body></html>";
            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Email refus envoye a: " + toEmail);
        } catch (Exception e) {
            System.err.println("sendRejectionEmail error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email notification — account modified
    // ══════════════════════════════════════════
    public static void sendModificationEmail(Personne personne, String newPassword) {
        if (personne.getEmail() == null || personne.getEmail().isBlank()) return;
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Gestion du Personnel"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(personne.getEmail()));
            message.setSubject("Modification de votre compte - REVIVE");

            boolean pwdChanged = newPassword != null && !newPassword.isBlank();
            String pwdRow = pwdChanged
                ? "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Nouveau mot de passe</td>"
                + "<td style='color:#EF4444;font-weight:bold;font-size:14px;text-align:right;'>" + newPassword + "</td></tr>"
                : "";
            String pwdNote = pwdChanged
                ? "<p style='color:#EF4444;font-size:12px;margin:0 0 16px;'>⚠ Votre mot de passe a ete modifie. Utilisez le nouveau mot de passe ci-dessus pour vous connecter.</p>"
                : "";

            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#0891B2;padding:28px 32px;'>"
                + "<h1 style='color:#fff;margin:0;font-size:24px;'>REVIVE</h1>"
                + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:13px;'>Modification de compte</p></div>"
                + "<div style='padding:32px;'>"
                + "<h2 style='color:#1A1D23;font-size:18px;margin:0 0 8px;'>Bonjour, " + personne.getPrenom() + " " + personne.getNom() + "</h2>"
                + "<p style='color:#6B7280;font-size:13px;margin:0 0 20px;'>Votre compte a ete modifie par un administrateur. Voici vos informations mises a jour :</p>"
                + "<div style='background:#F9FAFB;border:1.5px solid #E5E7EB;border-radius:12px;padding:20px;margin-bottom:20px;'>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Nom</td>"
                + "<td style='color:#1A1D23;font-weight:bold;font-size:13px;text-align:right;'>" + personne.getNom() + " " + personne.getPrenom() + "</td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Identifiant</td>"
                + "<td style='color:#0B4EA2;font-weight:bold;font-size:14px;text-align:right;'>" + personne.getIdentifiant() + "</td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Role</td>"
                + "<td style='color:#1A1D23;font-size:13px;text-align:right;'>" + personne.getRole() + "</td></tr>"
                + pwdRow
                + "</table></div>"
                + pwdNote
                + "<p style='color:#9CA3AF;font-size:11px;'>Si vous n'etes pas a l'origine de cette modification, contactez l'administration immediatement.</p>"
                + "</div>"
                + "<div style='background:#F3F4F6;padding:14px 32px;text-align:center;'>"
                + "<p style='color:#9CA3AF;font-size:11px;margin:0;'>Email automatique - Ne pas repondre</p>"
                + "</div></div></body></html>";

            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Email modification envoye a: " + personne.getEmail());
        } catch (Exception e) {
            System.err.println("sendModificationEmail error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email notification — account deleted
    // ══════════════════════════════════════════
    public static void sendDeletionEmail(Personne personne) {
        if (personne.getEmail() == null || personne.getEmail().isBlank()) return;
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Gestion du Personnel"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(personne.getEmail()));
            message.setSubject("Suppression de votre compte - REVIVE");

            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#475569;padding:28px 32px;'>"
                + "<h1 style='color:#fff;margin:0;font-size:24px;'>REVIVE</h1>"
                + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:13px;'>Suppression de compte</p></div>"
                + "<div style='padding:32px;'>"
                + "<h2 style='color:#1A1D23;font-size:18px;margin:0 0 8px;'>Bonjour, " + personne.getPrenom() + " " + personne.getNom() + "</h2>"
                + "<p style='color:#6B7280;font-size:13px;margin:0 0 20px;'>Votre compte REVIVE a ete supprime par un administrateur.</p>"
                + "<div style='background:#FEF2F2;border:1.5px solid #FECACA;border-radius:12px;padding:20px;margin-bottom:20px;'>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Identifiant</td>"
                + "<td style='color:#1A1D23;font-weight:bold;font-size:13px;text-align:right;'>" + personne.getIdentifiant() + "</td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Role</td>"
                + "<td style='color:#1A1D23;font-size:13px;text-align:right;'>" + personne.getRole() + "</td></tr>"
                + "</table></div>"
                + "<p style='color:#9CA3AF;font-size:11px;'>Vous n'avez plus acces au systeme REVIVE. Pour toute question, contactez l'administration.</p>"
                + "</div>"
                + "<div style='background:#F3F4F6;padding:14px 32px;text-align:center;'>"
                + "<p style='color:#9CA3AF;font-size:11px;margin:0;'>Email automatique - Ne pas repondre</p>"
                + "</div></div></body></html>";

            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Email suppression envoye a: " + personne.getEmail());
        } catch (Exception e) {
            System.err.println("sendDeletionEmail error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // Email shift notification to agent
    // ══════════════════════════════════════════
    public static void sendShiftNotification(String toEmail, String nom, String prenom,
                                              String typeShift, java.time.LocalDate debut,
                                              java.time.LocalDate fin) {
        try {
            Session session = buildSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL, "REVIVE - Planning"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Votre planning de la semaine - REVIVE");
            String heures = typeShift.startsWith("Matin") ? "06h00 - 14h00"
                          : typeShift.startsWith("Soir")  ? "14h00 - 22h00"
                          : "22h00 - 06h00";
            String couleur = typeShift.startsWith("Matin") ? "#059669"
                           : typeShift.startsWith("Soir")  ? "#0891B2" : "#7C3AED";
            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
                + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
                + "<div style='background:#0B4EA2;padding:24px 32px;'><h1 style='color:#fff;margin:0;font-size:22px;'>REVIVE</h1>"
                + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:12px;'>Planning de la semaine</p></div>"
                + "<div style='padding:28px;'><p style='color:#1A1D23;font-size:15px;'>Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>"
                + "<p style='color:#6B7280;font-size:13px;'>Votre planning pour la semaine a ete mis a jour :</p>"
                + "<div style='background:#F8FAFC;border:1.5px solid #E5E7EB;border-radius:12px;padding:20px;margin:16px 0;'>"
                + "<table style='width:100%;border-collapse:collapse;'>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Semaine</td>"
                + "<td style='color:#1A1D23;font-weight:bold;font-size:13px;text-align:right;'>" + debut + " → " + fin + "</td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Type de shift</td>"
                + "<td style='text-align:right;'><span style='background:" + couleur + ";color:#fff;padding:3px 12px;border-radius:20px;font-size:12px;font-weight:bold;'>" + typeShift + "</span></td></tr>"
                + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Horaires</td>"
                + "<td style='color:#1A1D23;font-weight:bold;font-size:14px;text-align:right;'>" + heures + "</td></tr>"
                + "</table></div>"
                + "<p style='color:#9CA3AF;font-size:11px;'>Pour toute question, contactez l'administration.</p>"
                + "</div><div style='background:#F3F4F6;padding:14px 32px;text-align:center;'>"
                + "<p style='color:#9CA3AF;font-size:11px;margin:0;'>Email automatique - Ne pas repondre</p>"
                + "</div></div></body></html>";
            message.setContent(body, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Notification shift envoyee a: " + toEmail);
        } catch (Exception e) {
            System.err.println("sendShiftNotification error: " + e.getMessage());
        }
    }

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.ssl.enable",      "true");   // SSL direct sur port 465
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);
        props.put("mail.smtp.connectiontimeout", "15000");
        props.put("mail.smtp.timeout",           "15000");
        props.put("mail.smtp.user",            SENDER_EMAIL);
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, SENDER_PWD);
            }
        });
        session.setDebug(true);
        return session;
    }

    private static String buildConfirmationBody(Personne p) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
             + "<div style='max-width:520px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;"
             + "box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
             + "<div style='background:#1A56DB;padding:28px 32px;'>"
             + "<h1 style='color:#fff;margin:0;font-size:24px;'>REVIVE</h1>"
             + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:13px;'>Systeme de Gestion des Urgences</p>"
             + "</div>"
             + "<div style='padding:32px;'>"
             + "<h2 style='color:#1A1D23;font-size:18px;margin:0 0 8px;'>Bienvenue, " + p.getPrenom() + " " + p.getNom() + " !</h2>"
             + "<p style='color:#6B7280;font-size:13px;margin:0 0 24px;'>Votre compte a ete cree avec succes. Voici vos coordonnees d'acces :</p>"
             + "<div style='background:#F9FAFB;border:1.5px solid #E5E7EB;border-radius:12px;padding:20px;margin-bottom:24px;'>"
             + "<table style='width:100%;border-collapse:collapse;'>"
             + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Identifiant</td>"
             + "<td style='color:#1A56DB;font-weight:bold;font-size:14px;text-align:right;'>" + p.getIdentifiant() + "</td></tr>"
             + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Mot de passe</td>"
             + "<td style='color:#1A1D23;font-weight:bold;font-size:14px;text-align:right;'>" + p.getMotDePasse() + "</td></tr>"
             + "<tr><td style='color:#6B7280;font-size:13px;padding:6px 0;'>Role</td>"
             + "<td style='color:#1A1D23;font-size:13px;text-align:right;'>" + p.getRole() + "</td></tr>"
             + "</table></div>"
             + "<p style='color:#EF4444;font-size:12px;margin:0 0 24px;'>Veuillez changer votre mot de passe lors de votre premiere connexion.</p>"
             + "<p style='color:#6B7280;font-size:12px;margin:0;'>Cordialement,<br><strong style='color:#1A1D23;'>L'equipe REVIVE</strong></p>"
             + "</div>"
             + "<div style='background:#F3F4F6;padding:16px 32px;text-align:center;'>"
             + "<p style='color:#9CA3AF;font-size:11px;margin:0;'>Email automatique - Ne pas repondre.</p>"
             + "</div></div></body></html>";
    }

    private static String buildResetBody(String nom, String prenom, String code) {
        return "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;background:#F0F4F8;padding:30px;'>"
             + "<div style='max-width:480px;margin:auto;background:#fff;border-radius:16px;overflow:hidden;"
             + "box-shadow:0 4px 20px rgba(0,0,0,0.1);'>"
             + "<div style='background:#1A56DB;padding:24px 32px;'>"
             + "<h1 style='color:#fff;margin:0;font-size:22px;'>REVIVE</h1>"
             + "<p style='color:rgba(255,255,255,0.75);margin:4px 0 0;font-size:12px;'>Reinitialisation du mot de passe</p>"
             + "</div>"
             + "<div style='padding:32px;'>"
             + "<p style='color:#1A1D23;font-size:15px;'>Bonjour <strong>" + prenom + " " + nom + "</strong>,</p>"
             + "<p style='color:#6B7280;font-size:13px;'>Voici votre code de reinitialisation :</p>"
             + "<div style='background:#EEF2FF;border:2px solid #1A56DB;border-radius:12px;padding:24px;text-align:center;margin:20px 0;'>"
             + "<span style='font-size:36px;font-weight:bold;color:#1A56DB;letter-spacing:8px;'>" + code + "</span>"
             + "</div>"
             + "<p style='color:#EF4444;font-size:12px;'>Ce code expire dans <strong>5 minutes</strong>.</p>"
             + "<p style='color:#9CA3AF;font-size:11px;'>Si vous n'avez pas demande cette reinitialisation, ignorez cet email.</p>"
             + "</div>"
             + "<div style='background:#F3F4F6;padding:14px 32px;text-align:center;'>"
             + "<p style='color:#9CA3AF;font-size:11px;margin:0;'>Email automatique - Ne pas repondre</p>"
             + "</div></div></body></html>";
    }
}

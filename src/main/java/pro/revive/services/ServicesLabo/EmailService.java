package pro.revive.services.ServicesLabo;

import pro.revive.utils.MyConnection;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

public class EmailService {

    private static final String EXPEDITEUR_EMAIL = "your-email@gmail.com";      // TODO: set via config
    private static final String EXPEDITEUR_MDP   = "your-app-password-here";    // TODO: Gmail App Password — NEVER commit real value
    private static final String LOGO_PATH        = "src/main/resources/images/logo_revive.png";

    private final Connection cnx;

    public EmailService() {
        this.cnx = MyConnection.getInstance().getCnx();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTHODE PUBLIQUE — appelée depuis AjouterResultatController
    // ─────────────────────────────────────────────────────────────────────────

    public void envoyerRapportMedecin(int idDemande,
                                      String typeExamen,
                                      String compteRendu,
                                      String fichierJoint,
                                      Date dateResultat,
                                      int scoreGravite,
                                      String niveauGravite,
                                      String recommandation) {
        try {
            String emailMedecin = getEmailMedecin(idDemande);
            if (emailMedecin == null || emailMedecin.isBlank()) {
                System.err.println("Email médecin introuvable pour id_demande = " + idDemande);
                return;
            }
            String nomMedecin = getNomMedecin(idDemande);
            String nomPatient = getNomPatient(idDemande);

            envoyerEmail(emailMedecin, nomMedecin, nomPatient, typeExamen,
                    compteRendu, fichierJoint, dateResultat,
                    scoreGravite, niveauGravite, recommandation);

            System.out.println("Email envoyé au médecin : " + emailMedecin);
        } catch (Exception e) {
            System.err.println("Erreur envoi email : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REQUÊTES BD
    // ─────────────────────────────────────────────────────────────────────────

    private String getEmailMedecin(int idDemande) {
        String sql = "SELECT per.email FROM examens_demandes ed " +
                "JOIN consultations c ON ed.id_consultation = c.id_consultation " +
                "JOIN personnel per ON c.id_personnel_medecin = per.id_personnel " +
                "WHERE ed.id_demande = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("email");
        } catch (SQLException e) {
            System.err.println("Erreur getEmailMedecin : " + e.getMessage());
        }
        return null;
    }

    private String getNomMedecin(int idDemande) {
        String sql = "SELECT per.nom, per.prenom FROM examens_demandes ed " +
                "JOIN consultations c ON ed.id_consultation = c.id_consultation " +
                "JOIN personnel per ON c.id_personnel_medecin = per.id_personnel " +
                "WHERE ed.id_demande = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom"), prenom = rs.getString("prenom");
                if (nom != null && prenom != null) return nom + " " + prenom;
                if (nom != null) return nom;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getNomMedecin : " + e.getMessage());
        }
        return "Docteur";
    }

    private String getNomPatient(int idDemande) {
        String sql = "SELECT p.nom, p.prenom FROM examens_demandes ed " +
                "JOIN consultations c ON ed.id_consultation = c.id_consultation " +
                "JOIN admissions a ON c.id_admission = a.id_admission " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "WHERE ed.id_demande = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom"), prenom = rs.getString("prenom");
                if (nom != null && prenom != null) return nom + " " + prenom;
                if (nom != null) return nom;
            }
        } catch (SQLException e) {
            System.err.println("Erreur getNomPatient : " + e.getMessage());
        }
        return "Patient inconnu";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUCTION ET ENVOI DU MAIL
    // ─────────────────────────────────────────────────────────────────────────

    private void envoyerEmail(String destinataire,
                              String nomMedecin,
                              String nomPatient,
                              String typeExamen,
                              String compteRendu,
                              String fichierJoint,
                              Date dateResultat,
                              int scoreGravite,
                              String niveauGravite,
                              String recommandation)
            throws MessagingException, UnsupportedEncodingException {

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EXPEDITEUR_EMAIL, EXPEDITEUR_MDP);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EXPEDITEUR_EMAIL, "REVIVE.Labo&Imagerie", "UTF-8"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));

        // ── Sujet adapté selon le niveau
        String sujetEmoji = switch (niveauGravite != null ? niveauGravite : "") {
            case "Critique" -> "🔴 [CRITIQUE]";
            case "Élevé"    -> "🟠 [ÉLEVÉ]";
            case "Moyen"    -> "🟡 [MOYEN]";
            default         -> "🟢 [NORMAL]";
        };
        message.setSubject(sujetEmoji + " Résultat d'examen — REVIVE.Labo&Imagerie", "UTF-8");

        // ── Données formatées
        String dateStr         = dateResultat != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dateResultat) : "-";
        String typeAffiche     = typeExamen != null
                ? typeExamen.replace("[ANALYSE] ", "").replace("[IMAGERIE] ", "") : "-";
        String compteRenduHtml = compteRendu != null
                ? compteRendu.replace("\n", "<br>") : "-";

        // ── Détecter le type (imagerie vs analyse)
        boolean estImagerie = fichierJoint != null && !fichierJoint.isBlank();

        // ── Construire le bloc IA selon le type
        String blocIA;
        if (estImagerie) {
            // Résultat IA Radio (DenseNet121)
            boolean grave    = "Grave".equalsIgnoreCase(niveauGravite) ||
                               "Élevé".equalsIgnoreCase(niveauGravite) ||
                               "Critique".equalsIgnoreCase(niveauGravite);
            String etatIA    = grave ? "PNEUMONIE DÉTECTÉE" : "NORMAL";
            String couleurIA = grave ? "#DC2626" : "#16A34A";
            String fondIA    = grave ? "#FEF2F2" : "#F0FDF4";
            String emojiIA   = grave ? "🔴" : "🟢";

            blocIA =
                "<div style='background:" + fondIA + ";border:2px solid " + couleurIA + ";" +
                "border-radius:14px;padding:18px;margin:20px 0;'>" +
                "<h3 style='color:" + couleurIA + ";margin:0 0 12px 0;font-size:15px;'>" +
                "🤖 Résultat IA — Analyse Radiologique (DenseNet121)</h3>" +
                "<div style='margin-bottom:10px;'>" +
                "<span style='background:" + couleurIA + ";color:white;font-weight:bold;" +
                "padding:6px 18px;border-radius:20px;font-size:14px;'>" +
                emojiIA + " " + etatIA + "</span>" +
                "</div>" +
                "<p style='margin:0;color:#475569;font-size:12px;'>" +
                (grave
                    ? "⚠️ Anomalie pulmonaire détectée. Consultation médicale urgente recommandée."
                    : "✅ Aucune anomalie pulmonaire détectée. Résultat rassurant.") +
                "</p></div>";
        } else {
            // Résultat Analyse Biologique Intelligente
            AnalyseBiologiqueService.ResultatAnalyse bio =
                    AnalyseBiologiqueService.analyser(compteRendu != null ? compteRendu : "");

            String couleurBio = AnalyseBiologiqueService.couleurNiveau(bio.niveauAttention);
            String fondBio    = AnalyseBiologiqueService.couleurFondNiveau(bio.niveauAttention);
            String emojiBio   = AnalyseBiologiqueService.emojiNiveau(bio.niveauAttention);

            StringBuilder anomaliesHtml = new StringBuilder();
            if (bio.anomalieDetectee) {
                for (AnalyseBiologiqueService.Anomalie a : bio.anomalies) {
                    anomaliesHtml.append(
                        "<div style='background:white;border-left:3px solid " +
                        AnalyseBiologiqueService.couleurNiveau(a.niveau) +
                        ";padding:6px 10px;margin:4px 0;border-radius:4px;'>" +
                        "<b style='color:" + AnalyseBiologiqueService.couleurNiveau(a.niveau) + ";'>" +
                        a.emoji + " " + a.biomarqueur + " — " + a.statut + "</b>" +
                        "<div style='font-size:11px;color:#64748B;'>" + a.signification + "</div>" +
                        "</div>");
                }
            }

            blocIA =
                "<div style='background:" + fondBio + ";border:2px solid " + couleurBio + ";" +
                "border-radius:14px;padding:18px;margin:20px 0;'>" +
                "<h3 style='color:" + couleurBio + ";margin:0 0 12px 0;font-size:15px;'>" +
                "🔬 Analyse Biologique Intelligente</h3>" +
                "<div style='margin-bottom:12px;'>" +
                "<span style='background:" + couleurBio + ";color:white;font-weight:bold;" +
                "padding:5px 16px;border-radius:20px;font-size:13px;'>" +
                emojiBio + " Niveau d'attention : " + bio.niveauAttention + "</span>" +
                "</div>" +
                (bio.anomalieDetectee
                    ? "<div style='margin-bottom:10px;'>" + anomaliesHtml + "</div>"
                    : "<p style='color:#16A34A;font-size:12px;margin:0 0 10px 0;'>✅ Aucune anomalie biologique majeure détectée.</p>") +
                "<div style='background:white;border-radius:8px;padding:10px;" +
                "border-left:4px solid " + couleurBio + ";'>" +
                "<b style='color:" + couleurBio + ";font-size:11px;'>🩺 Aide à la décision :</b>" +
                "<p style='margin:4px 0 0 0;color:#334155;font-size:12px;line-height:1.5;'>" +
                bio.aideDecision.replace("\n", "<br>") + "</p>" +
                "</div></div>";
        }

        // ── HTML du mail
        String html =
            "<html><body style='font-family:Arial,sans-serif;background:#F3F6FA;padding:20px;'>" +
            "<div style='max-width:650px;margin:auto;background:white;border-radius:16px;padding:25px;" +
            "box-shadow:0 4px 14px rgba(0,0,0,0.12);'>" +

            // Logo + titre
            "<div style='text-align:center;margin-bottom:20px;'>" +
            "<img src='cid:logoRevive' style='width:120px;height:auto;margin-bottom:10px;'/>" +
            "<h2 style='color:#0B4EA2;margin:5px 0;'>REVIVE.Labo&amp;Imagerie</h2>" +
            "<p style='color:#10B981;font-size:15px;font-weight:bold;'>🧪 Nouveau résultat disponible</p>" +
            "</div>" +

            // Salutation
            "<p style='font-size:15px;color:#0F172A;'>Bonjour <b>Monsieur/Madame " + nomMedecin + "</b>,</p>" +
            "<p style='font-size:14px;color:#334155;line-height:1.6;'>" +
            "Le résultat d'un examen est maintenant disponible dans le système REVIVE.</p>" +

            // Infos patient
            "<div style='background:#EEF6FF;border-left:5px solid #0B4EA2;padding:15px;" +
            "border-radius:10px;margin:20px 0;'>" +
            "<p style='margin:7px 0;'>👤 <b>Patient :</b> " + nomPatient + "</p>" +
            "<p style='margin:7px 0;'>🧾 <b>Type d'examen :</b> " + typeAffiche + "</p>" +
            "<p style='margin:7px 0;'>📅 <b>Date du résultat :</b> " + dateStr + "</p>" +
            "</div>" +

            // ══ BLOC IA (résultat radio ou analyse biologique) ══
            blocIA +

            // Compte rendu
            "<h3 style='color:#0B4EA2;'>📋 Compte rendu médical</h3>" +
            "<div style='background:#F8FAFC;border:1px solid #E2E8F0;border-radius:10px;" +
            "padding:15px;color:#334155;line-height:1.6;font-size:13px;'>" +
            compteRenduHtml + "</div>" +

            // Signature
            "<p style='margin-top:25px;color:#334155;'>Cordialement,</p>" +
            "<p style='font-weight:bold;color:#0B4EA2;font-size:16px;'>REVIVE.Labo&amp;Imagerie</p>" +

            "</div></body></html>";

        MimeMultipart multipart = new MimeMultipart("related");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(html, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        // Logo inline
        File logoFile = new File(LOGO_PATH);
        if (logoFile.exists() && logoFile.isFile()) {
            MimeBodyPart logoPart = new MimeBodyPart();
            DataSource logoSource = new FileDataSource(logoFile);
            logoPart.setDataHandler(new DataHandler(logoSource));
            logoPart.setHeader("Content-ID", "<logoRevive>");
            logoPart.setDisposition(MimeBodyPart.INLINE);
            multipart.addBodyPart(logoPart);
        } else {
            System.err.println("Logo introuvable : " + LOGO_PATH);
        }

        // Fichier joint (image radio)
        if (fichierJoint != null && !fichierJoint.isBlank()) {
            File fichier = new File(fichierJoint);
            if (fichier.exists() && fichier.isFile()) {
                MimeBodyPart pieceJointe = new MimeBodyPart();
                DataSource source = new FileDataSource(fichier);
                pieceJointe.setDataHandler(new DataHandler(source));
                pieceJointe.setFileName(fichier.getName());
                multipart.addBodyPart(pieceJointe);
            } else {
                System.err.println("Fichier joint introuvable : " + fichierJoint);
            }
        }

        message.setContent(multipart);
        Transport.send(message);
    }
}

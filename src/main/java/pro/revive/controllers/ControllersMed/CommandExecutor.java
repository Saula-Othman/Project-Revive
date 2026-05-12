package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.Ordonnance;
import pro.revive.services.ServicesMed.OrdonnanceService;
import pro.revive.services.ServicesMed.VoiceCommandService.Command;
import javafx.application.Platform;
import javafx.scene.control.*;

/**
 * Exécute les commandes vocales parsées sur les champs du formulaire de consultation.
 * Toutes les mises à jour UI sont faites via Platform.runLater.
 */
public class CommandExecutor {

    // ── Champs injectés ───────────────────────────────────────────────────
    private TextField   fieldSymptomes;
    private TextArea    fieldDiagnostic;
    private RadioButton radioSortie;
    private RadioButton radioHospitalisation;
    private RadioButton radioTransfert;
    private Label       lblStatut;

    private Consultation    consultationCourante;
    private final OrdonnanceService ordonnanceService = new OrdonnanceService();
    private Runnable        onCloturer;

    // ── Injection ─────────────────────────────────────────────────────────

    public void setFields(TextField symptomes, TextArea diagnostic,
                          RadioButton sortie, RadioButton hospit, RadioButton transfert,
                          Label statut) {
        this.fieldSymptomes       = symptomes;
        this.fieldDiagnostic      = diagnostic;
        this.radioSortie          = sortie;
        this.radioHospitalisation = hospit;
        this.radioTransfert       = transfert;
        this.lblStatut            = statut;
    }

    public void setConsultationCourante(Consultation c) { this.consultationCourante = c; }
    public void setOnCloturer(Runnable cb)              { this.onCloturer = cb; }

    // ── Exécution ─────────────────────────────────────────────────────────

    public String executer(Command cmd) {
        if (cmd == null) return "Commande nulle.";
        return switch (cmd.action) {
            case "remplir_champ"      -> executerRemplirChamp(cmd);
            case "ajouter_ordonnance" -> executerOrdonnance(cmd);
            case "cloturer"           -> executerCloturer();
            case "annuler"            -> executerAnnuler(cmd);
            case "inconnu"            -> "Commande non comprise : " + cmd.getParam("message");
            default                   -> "Action inconnue : " + cmd.action;
        };
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private String executerRemplirChamp(Command cmd) {
        String champ  = cmd.getParam("champ");
        String valeur = cmd.getParam("valeur");
        String orient = cmd.getParam("orientation");

        Platform.runLater(() -> {
            switch (champ.toLowerCase()) {
                case "symptomes", "symptômes" -> {
                    if (fieldSymptomes != null && !valeur.isEmpty())
                        fieldSymptomes.setText(valeur);
                }
                case "diagnostic" -> {
                    if (fieldDiagnostic != null && !valeur.isEmpty())
                        fieldDiagnostic.setText(valeur);
                }
                case "orientation" -> appliquerOrientation(valeur);
            }
            if (!orient.isEmpty()) appliquerOrientation(orient);
            afficherStatut("✓ " + champ + " mis à jour.", "#1E8449");
        });

        return "Champ '" + champ + "' mis à jour"
            + (!orient.isEmpty() ? " + orientation " + orient : "");
    }

    private String executerOrdonnance(Command cmd) {
        String medicament = cmd.getParam("medicament");
        String dosage     = cmd.getParam("dosage");
        String frequence  = cmd.getParam("frequence");
        String duree      = cmd.getParam("duree");

        if (medicament.isEmpty()) {
            Platform.runLater(() -> afficherStatut("Médicament non spécifié.", "#C0392B"));
            return "Médicament non spécifié.";
        }

        // Construire la posologie
        StringBuilder posologie = new StringBuilder();
        if (!dosage.isEmpty())    posologie.append(dosage);
        if (!frequence.isEmpty()) posologie.append(posologie.length() > 0 ? " — " : "").append(frequence);
        if (posologie.length() == 0) posologie.append("Selon prescription");

        int dureeJours = extraireDureeJours(duree);

        if (consultationCourante != null) {
            Ordonnance o = new Ordonnance(
                consultationCourante.getIdConsultation(),
                capitaliser(medicament),
                posologie.toString(),
                dureeJours
            );
            try {
                ordonnanceService.addEntity(o);
                final String msg = "✓ Ordonnance : " + capitaliser(medicament)
                    + " " + dosage
                    + (!frequence.isEmpty() ? " — " + frequence : "")
                    + (dureeJours > 0 ? " / " + dureeJours + "j" : "");
                Platform.runLater(() -> afficherStatut(msg, "#1E8449"));
                return msg;
            } catch (Exception e) {
                Platform.runLater(() -> afficherStatut("Erreur BDD : " + e.getMessage(), "#C0392B"));
                return "Erreur : " + e.getMessage();
            }
        } else {
            Platform.runLater(() -> afficherStatut(
                "Ouvrez une consultation existante pour ajouter une ordonnance.", "#D35400"));
            return "Aucune consultation ouverte.";
        }
    }

    private String executerCloturer() {
        Platform.runLater(() -> {
            afficherStatut("Clôture en cours...", "#D35400");
            if (onCloturer != null) onCloturer.run();
        });
        return "Consultation clôturée.";
    }

    private String executerAnnuler(Command cmd) {
        String champ = cmd.getParam("champ");
        Platform.runLater(() -> {
            switch (champ.toLowerCase()) {
                case "symptomes", "symptômes" -> { if (fieldSymptomes  != null) fieldSymptomes.clear(); }
                case "diagnostic"             -> { if (fieldDiagnostic != null) fieldDiagnostic.clear(); }
                case "orientation" -> {
                    if (radioSortie          != null) radioSortie.setSelected(false);
                    if (radioHospitalisation != null) radioHospitalisation.setSelected(false);
                    if (radioTransfert       != null) radioTransfert.setSelected(false);
                }
                case "tout" -> {
                    if (fieldSymptomes  != null) fieldSymptomes.clear();
                    if (fieldDiagnostic != null) fieldDiagnostic.clear();
                    if (radioSortie          != null) radioSortie.setSelected(false);
                    if (radioHospitalisation != null) radioHospitalisation.setSelected(false);
                    if (radioTransfert       != null) radioTransfert.setSelected(false);
                }
            }
            afficherStatut("✓ " + champ + " effacé.", "#B7950B");
        });
        return "Champ '" + champ + "' effacé.";
    }

    // ── Utilitaires ───────────────────────────────────────────────────────

    private void appliquerOrientation(String valeur) {
        if (valeur == null || valeur.isEmpty()) return;
        String v = valeur.toLowerCase();
        if (v.contains("hospit"))
            { if (radioHospitalisation != null) radioHospitalisation.setSelected(true); }
        else if (v.contains("transfert") || v.contains("transfer"))
            { if (radioTransfert != null) radioTransfert.setSelected(true); }
        else if (v.contains("sortie") || v.contains("sort"))
            { if (radioSortie != null) radioSortie.setSelected(true); }
    }

    private void afficherStatut(String message, String couleur) {
        if (lblStatut != null) {
            lblStatut.setText(message);
            lblStatut.setStyle("-fx-text-fill: " + couleur
                + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private int extraireDureeJours(String duree) {
        if (duree == null || duree.isEmpty()) return 7;
        try {
            String num = duree.replaceAll("[^0-9]", "").trim();
            if (num.isEmpty()) return 7;
            int n = Integer.parseInt(num);
            if (duree.toLowerCase().contains("semaine")) return n * 7;
            if (duree.toLowerCase().contains("mois"))    return n * 30;
            return n;
        } catch (Exception e) { return 7; }
    }

    private String capitaliser(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.ConsultationService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Controleur du dialog "Demande d'examens".
 * Permet au medecin de saisir les analyses biologiques et imageries
 * pour une consultation donnee, sans modifier le reste de la consultation.
 */
public class ExamenDialogController {

    @FXML private Label   labelTitre;
    @FXML private Label   labelPatient;
    @FXML private Label   labelConsultation;
    @FXML private Label   labelStatut;
    @FXML private HBox    boxStatut;
    @FXML private TextArea textAnalyses;
    @FXML private TextArea textImageries;

    private final ConsultationService cs = new ConsultationService();
    private Consultation consultation;

    // ── Initialisation ────────────────────────────────────────────────────

    public void setConsultation(Consultation c) {
        this.consultation = c;

        // En-tete
        labelPatient.setText(nvl(c.getNomPatient()));
        labelConsultation.setText("Consultation #" + c.getIdConsultation());

        // Pre-remplir si des examens existent deja
        if (c.getAnalyses() != null)   textAnalyses.setText(c.getAnalyses());
        if (c.getImageries() != null)  textImageries.setText(c.getImageries());

        // Afficher le statut actuel
        mettreAJourStatutAffichage(c.getStatutDemande());
    }

    private void mettreAJourStatutAffichage(String statut) {
        if ("Envoyee".equals(statut)) {
            labelStatut.setText("Demande envoyee");
            labelStatut.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #16a34a;");
            boxStatut.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8;"
                + "-fx-border-color: #bbf7d0; -fx-border-radius: 8; -fx-padding: 10 14;");
        } else {
            labelStatut.setText("Non envoyee");
            labelStatut.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
            boxStatut.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8;"
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-padding: 10 14;");
        }
    }

    // ── Raccourcis analyses ───────────────────────────────────────────────

    @FXML private void ajouterNFS()           { appendAnalyse("NFS"); }
    @FXML private void ajouterCRP()           { appendAnalyse("CRP"); }
    @FXML private void ajouterTroponines()    { appendAnalyse("Troponines"); }
    @FXML private void ajouterBilanHepatique(){ appendAnalyse("Bilan hepatique"); }
    @FXML private void ajouterGlycemie()      { appendAnalyse("Glycemie"); }

    // ── Raccourcis imageries ──────────────────────────────────────────────

    @FXML private void ajouterRadioThorax()    { appendImagerie("Radio thorax"); }
    @FXML private void ajouterScannerCerebral(){ appendImagerie("Scanner cerebral"); }
    @FXML private void ajouterEchoAbdo()       { appendImagerie("Echo abdominale"); }
    @FXML private void ajouterIRMLombaire()    { appendImagerie("IRM lombaire"); }

    // ── Helpers raccourcis ────────────────────────────────────────────────

    private void appendAnalyse(String item) {
        String current = textAnalyses.getText();
        if (current == null || current.trim().isEmpty()) {
            textAnalyses.setText(item);
        } else if (!current.contains(item)) {
            textAnalyses.setText(current.trim() + ", " + item);
        }
    }

    private void appendImagerie(String item) {
        String current = textImageries.getText();
        if (current == null || current.trim().isEmpty()) {
            textImageries.setText(item);
        } else if (!current.contains(item)) {
            textImageries.setText(current.trim() + ", " + item);
        }
    }

    // ── Actions boutons ───────────────────────────────────────────────────

    @FXML
    private void onEnvoyer() {
        if (consultation == null) return;

        String analyses  = textAnalyses.getText()  != null ? textAnalyses.getText().trim()  : "";
        String imageries = textImageries.getText() != null ? textImageries.getText().trim() : "";

        consultation.setAnalyses(analyses.isEmpty()  ? null : analyses);
        consultation.setImageries(imageries.isEmpty() ? null : imageries);
        consultation.calculerStatutDemande();

        cs.updateExamens(consultation.getIdConsultation(), analyses, imageries);

        fermer();
    }

    @FXML
    private void onAnnuler() {
        fermer();
    }

    private Runnable onCloseCallback;
    public void setOnCloseCallback(Runnable cb) { this.onCloseCallback = cb; }

    private void fermer() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else {
            Stage stage = (Stage) textAnalyses.getScene().getWindow();
            stage.close();
        }
    }

    private String nvl(String s) { return s != null ? s : "—"; }
}

package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.services.ServicesLabo.Examens_demandesService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SupprimerExamenController {

    @FXML private Label lblInfo;
    @FXML private Label lblMessage;

    private final Examens_demandesService service = new Examens_demandesService();
    private Examens_demandes examen;

    @FXML
    public void initialize() {
        examen = GestionExamensController.getExamenSelectionne();

        if (examen != null) {
            String nomPatient = service.getNomPatientByConsultation(examen.getIdConsultation());
            String affichagePatient = (nomPatient != null) ? nomPatient : "Consultation #" + examen.getIdConsultation();
            lblInfo.setText(
                    "Voulez-vous supprimer l'examen « " + examen.getTypeExamen() +
                            " » du patient : " + affichagePatient + " ?"
            );
        } else {
            lblInfo.setText("Aucun examen sélectionné.");
        }
    }

    @FXML
    private void handleSupprimer() {
        if (examen == null) {
            afficherErreur("Aucun examen sélectionné.");
            return;
        }

        try {
            service.supprimerExamen(examen.getIdDemande());
            afficherSucces("✅  Examen supprimé avec succès !");

            // Pop-up de confirmation, puis fermeture
            afficherAlertSucces("Examen supprimé avec succès !");
            fermerFenetre();

        } catch (Exception e) {
            afficherErreur("Erreur lors de la suppression : " + e.getMessage());
            afficherAlertErreur("Erreur lors de la suppression :\n" + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }

    private void fermerFenetre() {
        Stage stage = (Stage) lblInfo.getScene().getWindow();
        stage.close();
    }

    private void afficherSucces(String msg) {
        lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }

    private void afficherErreur(String msg) {
        lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }

    // ── Pop-up Alerts
    private void afficherAlertSucces(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅  " + message);
        alert.showAndWait();
    }

    private void afficherAlertErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ── Effets hover sur les boutons icônes
    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setScaleX(1.15); btn.setScaleY(1.15);
        }
    }
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setScaleX(1.0); btn.setScaleY(1.0);
        }
    }
}
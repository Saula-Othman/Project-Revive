package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.services.ServicesLabo.ResultatService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class SupprimerResultatController {

    @FXML private Label lblInfo;
    @FXML private Label lblMessage;

    private final ResultatService service = new ResultatService();
    private Resultats resultat;

    @FXML
    public void initialize() {
        resultat = GestionResultatController.getResultatSelectionne();

        if (resultat != null) {
            String nomPatient = service.getNomPatientByDemande(resultat.getIdDemande());
            String affichagePatient = (nomPatient != null) ? nomPatient : "Demande #" + resultat.getIdDemande();
            lblInfo.setText(
                    "Voulez-vous supprimer le résultat du patient : " + affichagePatient + " ?"
            );
        } else {
            lblInfo.setText("Aucun résultat sélectionné.");
        }
    }

    @FXML
    private void handleSupprimer() {
        if (resultat == null) {
            afficherErreur("Aucun résultat sélectionné.");
            return;
        }

        try {
            service.supprimer(resultat.getIdResultat());

            afficherSucces("✅  Résultat supprimé avec succès !");

            // Pop-up de confirmation, puis fermeture
            afficherAlertSucces("Résultat supprimé avec succès !");
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

    // ── Affichage inline
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
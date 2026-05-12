package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.services.ServicesLabo.ResultatService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ModifierResultatController {

    // ── ID Demande : Label non éditable (lecture seule)
    @FXML private Label   lblIdDemandeVal;
    @FXML private TextArea  taCompteRendu;
    @FXML private TextField tfFichierJoint;
    @FXML private Label     lblMessage;

    // Labels d'erreur
    @FXML private Label lblErrCompteRendu;

    private final ResultatService service = new ResultatService();
    private Resultats resultat;

    @FXML
    public void initialize() {
        resultat = GestionResultatController.getResultatSelectionne();
        lblErrCompteRendu.setText("");

        if (resultat != null) {
            // Afficher nom du patient ou l'ID en fallback
            String nomPatient = service.getNomPatientByDemande(resultat.getIdDemande());
            lblIdDemandeVal.setText(
                    nomPatient != null ? nomPatient : "Demande #" + resultat.getIdDemande()
            );
            taCompteRendu.setText(resultat.getCompteRendu());
            tfFichierJoint.setText(resultat.getFichierJoint());
        }

        // ── Validation en temps réel : compte rendu doit être une chaîne
        taCompteRendu.textProperty().addListener((obs, oldVal, newVal) -> {
            validerCompteRendu(newVal);
        });
    }

    // ── Validation Compte Rendu : doit être une description textuelle
    private boolean validerCompteRendu(String val) {
        if (val == null || val.trim().isEmpty()) {
            lblErrCompteRendu.setText("⚠  Le compte rendu est obligatoire.");
            mettreEnErreurTA(taCompteRendu);
            return false;
        }
        if (val.trim().matches("\\d+")) {
            lblErrCompteRendu.setText("⚠  Le compte rendu doit être une description textuelle.");
            mettreEnErreurTA(taCompteRendu);
            return false;
        }
        lblErrCompteRendu.setText("");
        retablirTA(taCompteRendu);
        return true;
    }

    @FXML
    private void handleModifier() {
        lblMessage.setText("");

        if (resultat == null) {
            afficherErreur("Aucun résultat sélectionné.");
            return;
        }

        boolean crOk = validerCompteRendu(taCompteRendu.getText());
        if (!crOk) {
            afficherErreur("Veuillez corriger les erreurs avant de continuer.");
            return;
        }

        try {
            // ID Demande non modifiable : on garde l'original
            resultat.setCompteRendu(taCompteRendu.getText().trim());
            resultat.setFichierJoint(tfFichierJoint.getText().trim());

            service.modifier(resultat);

            afficherSucces("✅  Résultat modifié avec succès !");

            // Pop-up de confirmation, puis fermeture
            afficherAlertSucces("Résultat modifié avec succès !");
            fermerFenetre();

        } catch (Exception e) {
            afficherErreur("Erreur : " + e.getMessage());
            afficherAlertErreur("Erreur lors de la modification :\n" + e.getMessage());
        }
    }

    private void fermerFenetre() {
        Stage stage = (Stage) taCompteRendu.getScene().getWindow();
        stage.close();
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

    // ── Helpers UI
    private static final String STYLE_BASE_TA =
            "-fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 9 14; -fx-font-size: 13px;";

    private void mettreEnErreurTA(TextArea ta) {
        ta.setStyle(STYLE_BASE_TA + "-fx-border-color: #EF4444;");
    }
    private void retablirTA(TextArea ta) {
        ta.setStyle(STYLE_BASE_TA + "-fx-border-color: #CBD5E1;");
    }
    private void afficherSucces(String msg) {
        lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }
    private void afficherErreur(String msg) {
        lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
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
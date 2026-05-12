package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.services.ServicesLabo.Examens_demandesService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ModifierExamenController {

    // ── ID Consultation : Label non éditable (récupéré depuis l'examen sélectionné)
    @FXML private Label       lblIdConsultationVal;   // affiche la valeur, non modifiable
    @FXML private TextField   tfTypeExamen;
    @FXML private ComboBox<String> cbStatut;
    @FXML private CheckBox    cbUrgent;
    @FXML private Label       lblMessage;
    @FXML private Label       lblErrTypeExamen;

    private final Examens_demandesService service = new Examens_demandesService();
    private Examens_demandes examen;

    @FXML
    public void initialize() {
        // ── Remplir ComboBox statut
        cbStatut.getItems().addAll("En attente", "Realisé");

        // ── Récupérer l'examen sélectionné (passé via variable statique du contrôleur parent)
        examen = GestionExamensController.getExamenSelectionne();

        if (examen != null) {
            // Afficher nom du patient ou l'ID en fallback
            String nomPatient = service.getNomPatientByConsultation(examen.getIdConsultation());
            lblIdConsultationVal.setText(
                    nomPatient != null
                            ? nomPatient + "  (Consultation #" + examen.getIdConsultation() + ")"
                            : "Consultation #" + examen.getIdConsultation()
            );
            tfTypeExamen.setText(examen.getTypeExamen());
            cbStatut.setValue(examen.getStatut());
            cbUrgent.setSelected(examen.isUrgent());
        }

        lblErrTypeExamen.setText("");

        // ── Validation en temps réel : type examen doit être une chaîne
        tfTypeExamen.textProperty().addListener((obs, oldVal, newVal) -> {
            validerTypeExamen(newVal);
        });
    }

    // ── Validation du type d'examen
    private boolean validerTypeExamen(String valeur) {
        if (valeur == null || valeur.trim().isEmpty()) {
            lblErrTypeExamen.setText("⚠  Le type d'examen est obligatoire.");
            mettreEnErreur(tfTypeExamen);
            return false;
        }
        if (valeur.trim().matches("\\d+")) {
            lblErrTypeExamen.setText("⚠  Le type d'examen doit être du texte, pas un nombre.");
            mettreEnErreur(tfTypeExamen);
            return false;
        }
        lblErrTypeExamen.setText("");
        retablirChamp(tfTypeExamen);
        return true;
    }

    @FXML
    private void handleModifier() {
        lblMessage.setText("");

        if (examen == null) {
            afficherErreur("Aucun examen sélectionné.");
            return;
        }

        // ── Contrôle type examen
        String typeExamen = tfTypeExamen.getText().trim();
        if (!validerTypeExamen(typeExamen)) {
            afficherErreur("Veuillez corriger les erreurs avant de continuer.");
            return;
        }

        try {
            // ID consultation n'est pas modifiable : on garde l'original
            examen.setTypeExamen(typeExamen);
            examen.setStatut(cbStatut.getValue());
            examen.setUrgent(cbUrgent.isSelected());

            service.modifierExamen(examen);

            afficherSucces("✅  Examen modifié avec succès !");

            // Pop-up de confirmation, puis fermeture
            afficherAlertSucces("Examen modifié avec succès !");
            fermerFenetre();

        } catch (Exception e) {
            afficherErreur("Erreur : " + e.getMessage());
            afficherAlertErreur("Erreur lors de la modification :\n" + e.getMessage());
        }
    }

    private void fermerFenetre() {
        Stage stage = (Stage) tfTypeExamen.getScene().getWindow();
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
    private void mettreEnErreur(TextField tf) {
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #EF4444; -fx-padding: 9 14; -fx-font-size: 13px;");
    }

    private void retablirChamp(TextField tf) {
        tf.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; " +
                "-fx-border-color: #CBD5E1; -fx-padding: 9 14; -fx-font-size: 13px;");
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
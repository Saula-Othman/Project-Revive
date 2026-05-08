package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.EmailService;
import pro.revive.services.ServicesUser.PersonneService;

import java.util.Random;

public class ForgotPasswordController {

    @FXML private TextField tfEmail;
    @FXML private Label     lblError;
    @FXML private Label     lblSuccess;

    private final PersonneService service = new PersonneService();

    @FXML
    void handleSendCode() {
        String email = tfEmail.getText().trim();

        if (email.isEmpty()) {
            showError("Veuillez saisir votre adresse email.");
            return;
        }

        Personne p = service.getByEmail(email);
        if (p == null) {
            showError("Aucun compte trouve avec cet email.");
            return;
        }

        // Générer code 6 chiffres
        String code = String.format("%06d", new Random().nextInt(999999));
        long expiry = System.currentTimeMillis() + 5 * 60 * 1000; // 5 minutes

        // Envoyer l'email
        EmailService.sendResetCode(email, p.getNom(), p.getPrenom(), code);

        // Passer à la fenêtre de reset
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/ResetPassword.fxml"));
            Parent root = loader.load();
            ResetPasswordController ctrl = loader.getController();
            ctrl.setResetData(p, code, expiry);

            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement.");
        }
    }

    @FXML
    void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfEmail.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
        if (lblSuccess != null) { lblSuccess.setVisible(false); lblSuccess.setManaged(false); }
    }
}

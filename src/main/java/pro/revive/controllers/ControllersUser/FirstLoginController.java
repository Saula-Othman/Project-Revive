package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.util.ResourceBundle;

public class FirstLoginController implements Initializable {

    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label         lblPasswordStrength;
    @FXML private Label         lblError;
    @FXML private Label         lblWelcome;

    private final PersonneService service = new PersonneService();
    private Personne currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pfNewPassword.textProperty().addListener((obs, o, n) -> updateStrength(n));
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblWelcome != null) {
            lblWelcome.setText("Bienvenue " + user.getPrenom() + " " + user.getNom()
                    + " ! Veuillez choisir votre mot de passe.");
        }
    }

    @FXML void handleChangePassword() {
        String newPwd  = pfNewPassword.getText();
        String confirm = pfConfirm.getText();

        if (newPwd.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (!newPwd.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }
        if (newPwd.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caracteres.");
            return;
        }

        // Update password and mark first login done
        service.updateEntity2(currentUser.getIdPersonnel(), newPwd);
        service.setPremierConnexionFalse(currentUser.getIdPersonnel());
        currentUser.setMotDePasse(newPwd);
        currentUser.setPremierConnexion(false);

        // Navigate to appropriate dashboard based on role
        navigateAfterPasswordChange();
    }

    private void navigateAfterPasswordChange() {
        try {
            // For now navigate to login — other modules handle their own dashboards
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) pfNewPassword.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) { lblPasswordStrength.setText(""); return; }
        int score = 0;
        if (pwd.length() >= 8)                        score++;
        if (pwd.matches(".*[A-Z].*"))                 score++;
        if (pwd.matches(".*[0-9].*"))                 score++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;
        switch (score) {
            case 0: case 1:
                lblPasswordStrength.setText("● Faible");
                lblPasswordStrength.setStyle("-fx-text-fill:#EF4444;-fx-font-weight:bold;-fx-font-size:12px;");
                break;
            case 2: case 3:
                lblPasswordStrength.setText("●● Moyen");
                lblPasswordStrength.setStyle("-fx-text-fill:#F59E0B;-fx-font-weight:bold;-fx-font-size:12px;");
                break;
            case 4:
                lblPasswordStrength.setText("●●● Fort");
                lblPasswordStrength.setStyle("-fx-text-fill:#10B981;-fx-font-weight:bold;-fx-font-size:12px;");
                break;
        }
    }

    private void showError(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
}

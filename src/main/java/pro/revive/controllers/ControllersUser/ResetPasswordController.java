package pro.revive.controllers.ControllersUser;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.PersonneService;

import java.net.URL;
import java.util.ResourceBundle;

public class ResetPasswordController implements Initializable {

    @FXML private TextField     tfCode;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirm;
    @FXML private Label         lblPasswordStrength;
    @FXML private Label         lblTimer;
    @FXML private Label         lblError;
    @FXML private Label         lblSuccess;

    private final PersonneService service = new PersonneService();
    private Personne personne;
    private String   expectedCode;
    private long     expiryTime;
    private Timeline timer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Password strength
        pfNewPassword.textProperty().addListener((obs, o, n) -> updateStrength(n));
    }

    public void setResetData(Personne p, String code, long expiry) {
        this.personne     = p;
        this.expectedCode = code;
        this.expiryTime   = expiry;
        startTimer();
    }

    private void startTimer() {
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = expiryTime - System.currentTimeMillis();
            if (remaining <= 0) {
                lblTimer.setText("Code expiré !");
                lblTimer.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                timer.stop();
            } else {
                long mins = remaining / 60000;
                long secs = (remaining % 60000) / 1000;
                lblTimer.setText(String.format("Code expire dans %d:%02d", mins, secs));
                lblTimer.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 12px;");
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    @FXML
    void handleReset() {
        String code    = tfCode.getText().trim();
        String newPwd  = pfNewPassword.getText();
        String confirm = pfConfirm.getText();

        if (code.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // Vérifier expiration
        if (System.currentTimeMillis() > expiryTime) {
            showError("Le code a expiré. Recommencez la procédure.");
            return;
        }

        // Vérifier code
        if (!code.equals(expectedCode)) {
            showError("Code incorrect.");
            return;
        }

        // Vérifier correspondance mots de passe
        if (!newPwd.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        if (newPwd.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }

        // Mettre à jour en DB
        service.updateEntity2(personne.getIdPersonnel(), newPwd);
        if (timer != null) timer.stop();

        // Retour au login
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 750);
            URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    void goBack() {
        if (timer != null) timer.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1100, 750);
            URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            Stage stage = (Stage) tfCode.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) {
            lblPasswordStrength.setText(""); return;
        }
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
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}

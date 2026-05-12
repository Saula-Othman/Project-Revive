package pro.revive.controllers.ControllersUser;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import pro.revive.App;
import pro.revive.SessionManager;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.AuditService;
import pro.revive.services.ServicesUser.CaptchaService;
import pro.revive.services.ServicesUser.GoogleAuthService;
import pro.revive.services.ServicesUser.LoginAttemptService;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

public class LoginController implements Initializable {

    @FXML private TextField     tfIdentifiant;
    @FXML private PasswordField pfMotDePasse;
    @FXML private TextField     tfCaptcha;
    @FXML private Canvas        captchaCanvas;
    @FXML private Label         lblError;
    @FXML private StackPane     rightPanel;

    private final PersonneService     service    = new PersonneService();
    private final CaptchaService      captcha    = new CaptchaService();
    private final LoginAttemptService attemptSvc = new LoginAttemptService();
    private final AuditService        auditSvc   = new AuditService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        captcha.generate(captchaCanvas);
        if (rightPanel != null) {
            AnimationUtil.popupIn(rightPanel, 600);
        }
    }

    @FXML
    void handleLogin() {
        String id  = tfIdentifiant.getText().trim();
        String pwd = pfMotDePasse.getText();
        String cap = tfCaptcha.getText().trim();

        if (id.isEmpty() || pwd.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        // 1. Check blocked
        String blockedMsg = attemptSvc.checkBlocked(id);
        if (blockedMsg != null) {
            showError(blockedMsg);
            captcha.generate(captchaCanvas);
            tfCaptcha.clear();
            return;
        }

        // 2. CAPTCHA
        if (!captcha.validate(cap)) {
            showError("Code CAPTCHA incorrect. Veuillez reessayer.");
            captcha.generate(captchaCanvas);
            tfCaptcha.clear();
            return;
        }

        // 3. Authenticate
        Personne user = service.getData4(id, pwd);
        if (user == null) {
            String status = service.getAccountStatus(id);
            if ("EN_ATTENTE".equals(status)) {
                showError("Votre compte est en attente de validation par l'administrateur.");
            } else {
                String msg = attemptSvc.recordFailedAttempt(id);
                showError(msg);
            }
            captcha.generate(captchaCanvas);
            tfCaptcha.clear();
            return;
        }

        // 4. Success — store session
        attemptSvc.resetAttempts(id);
        SessionManager.login(user);
        auditSvc.log(user.getIdPersonnel(), "CONNEXION",
                "Connexion reussie — role: " + user.getRole(), user.getIdentifiant());
        hideError();
        openModuleForUser(user);
    }

    @FXML
    void handleGoogleLogin() {
        showError("Connexion Google en cours...");
        CompletableFuture.supplyAsync(() -> GoogleAuthService.authenticate())
            .thenAccept(email -> Platform.runLater(() -> {
                if (email == null) { showError("Connexion Google annulee ou echouee."); return; }
                Personne user = service.getByEmail(email);
                if (user == null) { showError("Aucun compte REVIVE associe a ce compte Google (" + email + ")."); return; }
                attemptSvc.resetAttempts(user.getIdentifiant());
                SessionManager.login(user);
                auditSvc.log(user.getIdPersonnel(), "CONNEXION", "Connexion via Google OAuth", user.getIdentifiant());
                hideError();
                openModuleForUser(user);
            }));
    }

    @FXML void refreshCaptcha() {
        captcha.generate(captchaCanvas);
        tfCaptcha.clear();
    }

    @FXML void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/ForgotPassword.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfIdentifiant.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void handleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/SignUp.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfIdentifiant.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Role-based routing ────────────────────────────────────────────────

    /**
     * Routes the authenticated user to the correct module dashboard based on their role.
     *
     * Role → Module mapping:
     *   Administrateur          → Module 6  (Personnel)       M6_Dashboard.fxml
     *   Medecin Urgentiste      → Module 3  (Consultation)    dashboardMed.fxml
     *   Infirmier Triage        → Module 2  (Triage)          DashboardTriage.fxml
     *   Biologiste Radiologue   → Module 4  (Labo)            DashboardLabo.fxml
     *   Responsable Logistique  → Module 5  (Matériel)        dashboardMateriel.fxml
     *   Agent Accueil           → Module 6  (Personnel view)  M6_Dashboard.fxml
     */
    private void openModuleForUser(Personne user) {
        // First login — force password change regardless of role
        if (user.isPremierConnexion()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(
                        "/ResourcesUser/images/fxml/FirstLogin.fxml"));
                Parent root = loader.load();
                FirstLoginController ctrl = loader.getController();
                ctrl.setCurrentUser(user);
                Stage stage = (Stage) tfIdentifiant.getScene().getWindow();
                AnimationUtil.navigateWithFade(stage, root, () -> {});
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur lors du chargement de la page de premiere connexion.");
            }
            return;
        }

        String role = user.getRole() != null ? user.getRole() : "";
        try {
            switch (role) {

                case "Administrateur":
                case "Agent Accueil": {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/ResourcesUser/images/fxml/M6_Dashboard.fxml"));
                    Parent root = loader.load();
                    M6DashboardController ctrl = loader.getController();
                    ctrl.setCurrentUser(user);
                    navigateTo(root, "REVIVE — Gestion du Personnel");
                    break;
                }

                case "Medecin Urgentiste": {
                    Parent root = FXMLLoader.load(getClass().getResource(
                            "/ResourcesMed/module3/fxml/dashboardMed.fxml"));
                    navigateTo(root, "REVIVE — Consultations Médicales");
                    break;
                }

                case "Infirmier Triage": {
                    Parent root = FXMLLoader.load(getClass().getResource(
                            "/TriageResources/fxml/DashboardTriage.fxml"));
                    navigateTo(root, "REVIVE — Triage");
                    break;
                }

                case "Biologiste Radiologue": {
                    Parent root = FXMLLoader.load(getClass().getResource(
                            "/ResourcesLabo/DashboardLabo.fxml"));
                    navigateTo(root, "REVIVE — Laboratoire");
                    break;
                }

                case "Responsable Logistique": {
                    Parent root = FXMLLoader.load(getClass().getResource(
                            "/ResourcesMateriel/module5/view/dashboardMateriel.fxml"));
                    navigateTo(root, "REVIVE — Logistique & Matériel");
                    break;
                }

                default: {
                    // Unknown role — fall back to personnel dashboard
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/ResourcesUser/images/fxml/M6_Dashboard.fxml"));
                    Parent root = loader.load();
                    M6DashboardController ctrl = loader.getController();
                    ctrl.setCurrentUser(user);
                    navigateTo(root, "REVIVE — Tableau de Bord");
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement du tableau de bord : " + e.getMessage());
        }
    }

    /**
     * Swaps the scene on the primary stage with a fade transition.
     */
    private void navigateTo(Parent root, String title) {
        Stage stage = (Stage) tfIdentifiant.getScene().getWindow();
        AnimationUtil.navigateWithFade(stage, root, () -> stage.setTitle(title));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}

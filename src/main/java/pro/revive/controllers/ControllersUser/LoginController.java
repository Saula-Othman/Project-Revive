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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import pro.revive.App;
import pro.revive.SessionManager;
import pro.revive.controllers.ControllersAdmission.MainController;
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

    private MediaPlayer mediaPlayer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        captcha.generate(captchaCanvas);
        // Defer video injection until the panel is fully laid out and has real dimensions
        Platform.runLater(this::startVideoLoop);
        if (rightPanel != null) {
            AnimationUtil.popupIn(rightPanel, 600);
        }
    }

    /** Loads MED.mp4 into the right panel as a muted infinite loop. */
    private void startVideoLoop() {
        if (rightPanel == null) { System.err.println("[LoginController] rightPanel is null — skipping video."); return; }
        try {
            java.net.URL res = getClass().getResource("/ResourcesUser/images/MED.mp4");
            if (res == null) { System.err.println("[LoginController] MED.mp4 not found in resources."); return; }

            Media media = new Media(res.toExternalForm());
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            mediaPlayer.setMute(true);
            mediaPlayer.setAutoPlay(true);

            // Fallback: if the cycle somehow ends, seek back to start and replay
            mediaPlayer.setOnEndOfMedia(() -> {
                mediaPlayer.seek(mediaPlayer.getStartTime());
                mediaPlayer.play();
            });

            // Fallback: if a media error occurs, log it silently (don't crash the UI)
            mediaPlayer.setOnError(() ->
                System.err.println("[LoginController] Video error: " + mediaPlayer.getError())
            );

            MediaView mediaView = new MediaView(mediaPlayer);
            mediaView.setPreserveRatio(true);

            mediaView.fitWidthProperty().bind(rightPanel.widthProperty());
            mediaView.fitHeightProperty().bind(rightPanel.heightProperty());

            // Insert at index 0 so any future overlay content stays on top
            rightPanel.getChildren().add(0, mediaView);
        } catch (Exception e) {
            System.err.println("[LoginController] Could not load video: " + e.getMessage());
        }
    }

    /** Stops the video before navigating away to free the media thread. */
    private void stopVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
            mediaPlayer = null;
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
            stopVideo();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void handleSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/SignUp.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfIdentifiant.getScene().getWindow();
            stopVideo();
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
     *   Agent Accueil           → Module 1  (Admission)       MainAdmission.fxml
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

                case "Administrateur": {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/ResourcesUser/images/fxml/M6_Dashboard.fxml"));
                    Parent root = loader.load();
                    M6DashboardController ctrl = loader.getController();
                    ctrl.setCurrentUser(user);
                    navigateTo(root, "REVIVE — Gestion du Personnel");
                    break;
                }

                case "Agent Accueil": {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource(
                            "/ResourceAdmission/urgence/fxml/MainAdmission.fxml"));
                    Parent root = loader.load();
                    root.getStylesheets().add(getClass().getResource(
                            "/ResourceAdmission/urgence/css/theme.css").toExternalForm());
                    MainController ctrl = loader.getController();
                    ctrl.initData();
                    navigateTo(root, "REVIVE — Admission & Accueil");
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
        stopVideo();
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

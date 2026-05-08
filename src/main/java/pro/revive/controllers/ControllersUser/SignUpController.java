package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.EmailService;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;

public class SignUpController implements Initializable {

    @FXML private TextField        tfNom;
    @FXML private TextField        tfPrenom;
    @FXML private ComboBox<String> cbRole;
    @FXML private DatePicker       dpDateNaissance;
    @FXML private Label            lblAge;
    @FXML private Label            lblAgeError;
    @FXML private ComboBox<String> cbPays;
    @FXML private TextField        tfTelephone;
    @FXML private TextField        tfEmail;
    @FXML private Label            lblEmailStatus;
    @FXML private Label            lblError;
    @FXML private Label            lblSuccess;

    private final PersonneService service = new PersonneService();

    private static final String ADMIN_EMAIL = "boughzala.medsalah@gmail.com";

    private static final String[] PAYS = {
        "+93 Afghanistan", "+213 Algérie", "+54 Argentine", "+61 Australie",
        "+43 Autriche", "+32 Belgique", "+55 Brésil", "+1 Canada",
        "+86 Chine", "+45 Danemark", "+20 Égypte", "+33 France",
        "+49 Allemagne", "+30 Grèce", "+91 Inde", "+39 Italie",
        "+81 Japon", "+962 Jordanie", "+961 Liban", "+218 Libye",
        "+212 Maroc", "+52 Mexique", "+31 Pays-Bas", "+234 Nigeria",
        "+92 Pakistan", "+351 Portugal", "+7 Russie", "+966 Arabie Saoudite",
        "+221 Sénégal", "+34 Espagne", "+41 Suisse", "+963 Syrie",
        "+216 Tunisie", "+90 Turquie", "+971 Émirats Arabes Unis",
        "+44 Royaume-Uni", "+1 États-Unis", "+84 Vietnam"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbRole.getItems().addAll(
            "Medecin Urgentiste", "Infirmier Triage",
            "Agent Accueil", "Biologiste Radiologue",
            "Responsable Logistique"
            // Note: Administrateur excluded from self-signup
        );
        cbPays.getItems().addAll(PAYS);
        cbPays.setValue("+216 Tunisie");

        // Letters-only
        tfNom.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZÀ-ÿ\\s\\-']*")) tfNom.setText(o);
        });
        tfPrenom.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZÀ-ÿ\\s\\-']*")) tfPrenom.setText(o);
        });

        // Email validation
        tfEmail.textProperty().addListener((obs, o, n) -> validateEmail(n));

        // Age validation
        dpDateNaissance.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                int age = Period.between(n, LocalDate.now()).getYears();
                lblAge.setText(age + " ans");
                if (age < 18) {
                    lblAge.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    lblAgeError.setText("⚠ Age minimum : 18 ans");
                    lblAgeError.setVisible(true); lblAgeError.setManaged(true);
                } else if (age > 70) {
                    lblAge.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    lblAgeError.setText("⚠ Age maximum : 70 ans");
                    lblAgeError.setVisible(true); lblAgeError.setManaged(true);
                } else {
                    lblAge.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    lblAgeError.setVisible(false); lblAgeError.setManaged(false);
                }
            } else {
                lblAge.setText("—");
                lblAgeError.setVisible(false); lblAgeError.setManaged(false);
            }
        });
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) { lblEmailStatus.setText(""); return; }
        if (email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            lblEmailStatus.setText("✓ Email valide");
            lblEmailStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lblEmailStatus.setText("✗ Format invalide");
            lblEmailStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    @FXML void handleSignUp() {
        String nom    = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String role   = cbRole.getValue();
        LocalDate dob = dpDateNaissance.getValue();
        String tel    = tfTelephone.getText().trim();
        String email  = tfEmail.getText().trim();
        String pays   = cbPays.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || role == null || dob == null || tel.isEmpty() || email.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires.");
            return;
        }
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Adresse email invalide.");
            return;
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) { showError("Age minimum : 18 ans."); return; }
        if (age > 70) { showError("Age maximum : 70 ans."); return; }

        String indicatif = pays.split(" ")[0];
        String telComplet = indicatif + " " + tel;

        Personne p = new Personne();
        p.setNom(nom); p.setPrenom(prenom); p.setRole(role);
        p.setDateNaissance(dob); p.setTelephone(telComplet); p.setEmail(email);

        service.addSignupRequest(p);

        // Notify admin
        EmailService.sendAdminNotification(ADMIN_EMAIL, nom, prenom, role);

        // Show success
        hideError();
        lblSuccess.setText("✓ Demande envoyee ! Vous recevrez un email lorsque votre compte sera active.");
        lblSuccess.setVisible(true); lblSuccess.setManaged(true);

        // Disable form
        tfNom.setDisable(true); tfPrenom.setDisable(true);
        cbRole.setDisable(true); dpDateNaissance.setDisable(true);
        tfTelephone.setDisable(true); tfEmail.setDisable(true);
    }

    @FXML void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) tfNom.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
        lblSuccess.setVisible(false); lblSuccess.setManaged(false);
    }
    private void hideError() {
        lblError.setVisible(false); lblError.setManaged(false);
    }
}

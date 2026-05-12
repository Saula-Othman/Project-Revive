package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.AuditService;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;

public class M6PersonnelAddController implements Initializable {

    @FXML private TextField        tfNom;
    @FXML private TextField        tfPrenom;
    @FXML private ComboBox<String> cbRole;
    @FXML private Label            lblIdentifiant;
    @FXML private Label            lblError;
    @FXML private Label            lblUserName;
    @FXML private Label            lblUserRole;
    @FXML private DatePicker       dpDateNaissance;
    @FXML private Label            lblAge;
    @FXML private Label            lblAgeError;
    @FXML private ComboBox<String> cbPays;
    @FXML private TextField        tfTelephone;
    @FXML private TextField        tfEmail;
    @FXML private Label            lblEmailStatus;
    @FXML private Label            lblDuplicate;

    private final PersonneService service  = new PersonneService();
    private final AuditService    auditSvc = new AuditService();
    private Personne currentUser;
    private boolean  alreadySaved = false;

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
            "Responsable Logistique", "Administrateur"
        );
        cbPays.getItems().addAll(PAYS);
        cbPays.setValue("+216 Tunisie");

        // Letters-only
        tfNom.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZÀ-ÿ\\s\\-']*")) tfNom.setText(o);
            alreadySaved = false;
        });
        tfPrenom.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZÀ-ÿ\\s\\-']*")) tfPrenom.setText(o);
            alreadySaved = false;
        });

        // Email validation
        tfEmail.textProperty().addListener((obs, o, n) -> {
            validateEmail(n);
            alreadySaved = false;
        });

        // Age validation
        dpDateNaissance.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                int age = Period.between(n, LocalDate.now()).getYears();
                lblAge.setText(age + " ans");
                if (age < 18) {
                    lblAge.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    if (lblAgeError != null) { lblAgeError.setText("⚠ Age minimum : 18 ans"); lblAgeError.setVisible(true); lblAgeError.setManaged(true); }
                } else if (age > 70) {
                    lblAge.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                    if (lblAgeError != null) { lblAgeError.setText("⚠ Age maximum : 70 ans"); lblAgeError.setVisible(true); lblAgeError.setManaged(true); }
                } else {
                    lblAge.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    if (lblAgeError != null) { lblAgeError.setVisible(false); lblAgeError.setManaged(false); }
                }
            } else {
                lblAge.setText("—");
                if (lblAgeError != null) { lblAgeError.setVisible(false); lblAgeError.setManaged(false); }
            }
            alreadySaved = false;
        });

        cbRole.valueProperty().addListener((obs, o, n) -> alreadySaved = false);
        tfTelephone.textProperty().addListener((obs, o, n) -> alreadySaved = false);
    }

    private void validateEmail(String email) {
        if (lblEmailStatus == null) return;
        if (email == null || email.isBlank()) { lblEmailStatus.setText(""); return; }
        if (email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            lblEmailStatus.setText("✓ Email valide");
            lblEmailStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lblEmailStatus.setText("✗ Format invalide");
            lblEmailStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    @FXML void save() {
        if (alreadySaved) {
            showError("Deja enregistre. Effacez le formulaire pour ajouter un nouveau personnel.");
            return;
        }

        String nom    = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String role   = cbRole.getValue();
        LocalDate dob = dpDateNaissance.getValue();
        String tel    = tfTelephone.getText().trim();
        String email  = tfEmail.getText().trim();
        String pays   = cbPays.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || role == null
                || dob == null || tel.isEmpty() || email.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires (*).");
            return;
        }
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Adresse email invalide.");
            return;
        }
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) { showError("L'agent doit avoir au moins 18 ans."); return; }
        if (age > 70) { showError("L'age ne peut pas depasser 70 ans."); return; }

        // Duplicate detection
        java.util.List<Personne> duplicates = service.checkDuplicate(nom, prenom);
        if (!duplicates.isEmpty() && lblDuplicate != null) {
            lblDuplicate.setText("⚠ Agent similaire : " + duplicates.get(0).getNom()
                    + " " + duplicates.get(0).getPrenom() + " (" + duplicates.get(0).getRole() + ")");
            lblDuplicate.setVisible(true); lblDuplicate.setManaged(true);
        } else if (lblDuplicate != null) {
            lblDuplicate.setVisible(false); lblDuplicate.setManaged(false);
        }

        String indicatif = pays.split(" ")[0];
        String telComplet = indicatif + " " + tel;

        // Auto-generate password
        String autoPassword = generateTempPassword();

        Personne p = new Personne(nom, prenom, role, "", autoPassword);
        p.setDateNaissance(dob);
        p.setTelephone(telComplet);
        p.setEmail(email);

        service.addEntity(p); // INSERT + sends confirmation email with auto password

        if (currentUser != null) {
            auditSvc.log(p.getIdPersonnel(), "AJOUT",
                    "Ajout de " + nom + " " + prenom + " (" + role + ")",
                    currentUser.getIdentifiant(), p);
        }

        lblIdentifiant.setText(p.getIdentifiant() != null ? p.getIdentifiant() : "Genere !");
        hideError();
        alreadySaved = true;
        goPersonnel();
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$!";
        java.util.Random rnd = new java.util.Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    @FXML void clearForm() {
        tfNom.clear(); tfPrenom.clear();
        cbRole.getSelectionModel().clearSelection();
        dpDateNaissance.setValue(null);
        cbPays.setValue("+216 Tunisie");
        tfTelephone.clear(); tfEmail.clear();
        lblIdentifiant.setText("——");
        lblAge.setText("—");
        if (lblEmailStatus != null) lblEmailStatus.setText("");
        hideError();
        alreadySaved = false;
    }

    @FXML void cancel()         { goPersonnel(); }
    @FXML void goDashboard()    { nav("/ResourcesUser/images/fxml/M6_Dashboard.fxml", true); }
    @FXML void goPersonnel()    { nav("/ResourcesUser/images/fxml/M6_Personnel_List.fxml", false); }
    @FXML void goInscriptions() { nav("/ResourcesUser/images/fxml/InscriptionRequests.fxml", false); }
    @FXML void goHistorique()   { nav("/ResourcesUser/images/fxml/Historique.fxml", false); }
    @FXML void goShifts()       { nav("/ResourcesUser/images/fxml/Shifts.fxml", false); }
    @FXML void deconnexion()    { nav("/ResourcesUser/images/fxml/Login.fxml", false); }

    private void nav(String fxmlPath, boolean isDashboard) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            if (isDashboard && currentUser != null)
                ((M6DashboardController) loader.getController()).setCurrentUser(currentUser);
            else if (fxmlPath.contains("List") && currentUser != null)
                ((M6PersonnelListController) loader.getController()).setCurrentUser(currentUser);
            else if (fxmlPath.contains("Historique") && currentUser != null)
                ((HistoriqueController) loader.getController()).setCurrentUser(currentUser);
            else if (fxmlPath.contains("InscriptionRequests") && currentUser != null)
                ((InscriptionRequestsController) loader.getController()).setCurrentUser(currentUser);
            else if (fxmlPath.contains("Shifts") && currentUser != null)
                ((ShiftsController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }
    private void hideError() {
        lblError.setVisible(false); lblError.setManaged(false);
    }
}

package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.AuditService;
import pro.revive.services.ServicesUser.EmailService;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;

public class M6PersonnelEditController implements Initializable {

    @FXML private TextField        tfIdentifiant;
    @FXML private TextField        tfNom;
    @FXML private TextField        tfPrenom;
    @FXML private ComboBox<String> cbRole;
    @FXML private PasswordField    pfNewPassword;
    @FXML private Label            lblPasswordStrength;
    @FXML private DatePicker       dpDateNaissance;
    @FXML private Label            lblAge;
    @FXML private Label            lblAgeError;
    @FXML private ComboBox<String> cbPays;
    @FXML private TextField        tfTelephone;
    @FXML private TextField        tfEmail;
    @FXML private Label            lblEmailStatus;
    @FXML private Label            lblEditSub;
    @FXML private Label            lblSidebarName;
    @FXML private Label            lblCurrentName;
    @FXML private Label            lblCurrentRole;
    @FXML private Label            lblError;
    @FXML private Label            lblUserName;
    @FXML private Label            lblUserRole;

    private final PersonneService service = new PersonneService();
    private final AuditService    auditSvc = new AuditService();
    private Personne currentUser;
    private Personne personne;

    private static final String[] PAYS = {
        "+93 Afghanistan", "+355 Albanie", "+213 Algérie", "+376 Andorre",
        "+244 Angola", "+54 Argentine", "+374 Arménie", "+61 Australie",
        "+43 Autriche", "+994 Azerbaïdjan", "+880 Bangladesh", "+32 Belgique",
        "+55 Brésil", "+1 Canada", "+86 Chine", "+57 Colombie",
        "+45 Danemark", "+20 Égypte", "+251 Éthiopie", "+33 France",
        "+49 Allemagne", "+233 Ghana", "+30 Grèce", "+91 Inde",
        "+62 Indonésie", "+98 Iran", "+964 Irak", "+353 Irlande",
        "+972 Israël", "+39 Italie", "+81 Japon", "+962 Jordanie",
        "+254 Kenya", "+965 Koweït", "+961 Liban", "+218 Libye",
        "+60 Malaisie", "+212 Maroc", "+52 Mexique", "+258 Mozambique",
        "+31 Pays-Bas", "+64 Nouvelle-Zélande", "+234 Nigeria", "+47 Norvège",
        "+968 Oman", "+92 Pakistan", "+507 Panama", "+51 Pérou",
        "+63 Philippines", "+48 Pologne", "+351 Portugal", "+974 Qatar",
        "+40 Roumanie", "+7 Russie", "+250 Rwanda", "+966 Arabie Saoudite",
        "+221 Sénégal", "+65 Singapour", "+27 Afrique du Sud", "+34 Espagne",
        "+94 Sri Lanka", "+46 Suède", "+41 Suisse", "+963 Syrie",
        "+66 Thaïlande", "+216 Tunisie", "+90 Turquie", "+380 Ukraine",
        "+971 Émirats Arabes Unis", "+44 Royaume-Uni", "+1 États-Unis",
        "+58 Venezuela", "+84 Vietnam", "+967 Yémen", "+263 Zimbabwe"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbRole.getItems().addAll(
            "Medecin Urgentiste", "Infirmier Triage",
            "Agent Accueil", "Biologiste Radiologue",
            "Responsable Logistique", "Administrateur"
        );
        cbPays.getItems().addAll(PAYS);

        // Letters-only
        tfNom.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZÀ-ÿ\\s\\-']*")) tfNom.setText(o);
        });
        tfPrenom.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZÀ-ÿ\\s\\-']*")) tfPrenom.setText(o);
        });

        // Password strength
        pfNewPassword.textProperty().addListener((obs, o, n) -> updatePasswordStrength(n));

        // Real-time email validation
        tfEmail.textProperty().addListener((obs, o, n) -> validateEmail(n));

        // Calcul âge + validation
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
        });
    }

    private void validateEmail(String email) {
        if (lblEmailStatus == null) return;
        if (email == null || email.isBlank()) { lblEmailStatus.setText(""); return; }
        if (email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            lblEmailStatus.setText("✓ Email valide");
            lblEmailStatus.setStyle("-fx-text-fill: #059669; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lblEmailStatus.setText("✗ Format invalide (ex: nom@domaine.com)");
            lblEmailStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private void updatePasswordStrength(String pwd) {
        if (pwd == null || pwd.isEmpty()) {
            lblPasswordStrength.setText("");
            lblPasswordStrength.setStyle("");
            return;
        }
        int score = 0;
        if (pwd.length() >= 8)                        score++;
        if (pwd.matches(".*[A-Z].*"))                 score++;
        if (pwd.matches(".*[0-9].*"))                 score++;
        if (pwd.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score++;

        switch (score) {
            case 0: case 1:
                lblPasswordStrength.setText("● Faible");
                lblPasswordStrength.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 12px;");
                break;
            case 2: case 3:
                lblPasswordStrength.setText("●● Moyen");
                lblPasswordStrength.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 12px;");
                break;
            case 4:
                lblPasswordStrength.setText("●●● Fort");
                lblPasswordStrength.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-font-size: 12px;");
                break;
        }
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    public void setPersonne(Personne p) {
        this.personne = p;
        String fullName = p.getPrenom() + " " + p.getNom();
        if (lblEditSub != null)     lblEditSub.setText("Edition du profil de " + fullName);
        if (lblSidebarName != null) lblSidebarName.setText(fullName);
        if (lblCurrentName != null) lblCurrentName.setText(fullName);
        if (lblCurrentRole != null) {
            lblCurrentRole.setText(p.getRole() != null ? p.getRole() : "—");
            lblCurrentRole.getStyleClass().clear();
            lblCurrentRole.getStyleClass().addAll("badge", getRoleBadgeClass(p.getRole()));
        }

        tfIdentifiant.setText(p.getIdentifiant());
        tfNom.setText(p.getNom());
        tfPrenom.setText(p.getPrenom());
        cbRole.setValue(p.getRole());

        // Date naissance + âge
        if (p.getDateNaissance() != null) {
            dpDateNaissance.setValue(p.getDateNaissance());
            int age = Period.between(p.getDateNaissance(), LocalDate.now()).getYears();
            lblAge.setText(age + " ans");
            lblAge.setStyle("-fx-text-fill: #1A56DB; -fx-font-weight: bold;");
        }

        // Téléphone — séparer indicatif et numéro
        if (p.getTelephone() != null && p.getTelephone().contains(" ")) {
            String[] parts = p.getTelephone().split(" ", 2);
            // Trouver le pays correspondant à l'indicatif
            for (String pays : PAYS) {
                if (pays.startsWith(parts[0] + " ")) {
                    cbPays.setValue(pays);
                    break;
                }
            }
            tfTelephone.setText(parts[1]);
        } else {
            cbPays.setValue("+216 Tunisie");
            tfTelephone.setText(p.getTelephone() != null ? p.getTelephone() : "");
        }

        tfEmail.setText(p.getEmail() != null ? p.getEmail() : "");
    }

    @FXML void update() {
        String nom    = tfNom.getText().trim();
        String prenom = tfPrenom.getText().trim();
        String role   = cbRole.getValue();
        LocalDate dob = dpDateNaissance.getValue();
        String tel    = tfTelephone.getText().trim();
        String email  = tfEmail.getText().trim();
        String pays   = cbPays.getValue();

        if (nom.isEmpty() || prenom.isEmpty() || role == null || dob == null
                || tel.isEmpty() || email.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires.");
            return;
        }
        if (!email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            showError("Adresse email invalide.");
            return;
        }

        // Age validation
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 18) { showError("L'agent doit avoir au moins 18 ans."); return; }
        if (age > 70) { showError("L'age ne peut pas depasser 70 ans."); return; }

        String indicatif = pays.split(" ")[0];
        String telComplet = indicatif + " " + tel;

        Personne updated = new Personne();
        updated.setNom(nom);
        updated.setPrenom(prenom);
        updated.setRole(role);
        updated.setIdentifiant(personne.getIdentifiant());
        updated.setDateNaissance(dob);
        updated.setTelephone(telComplet);
        updated.setEmail(email);

        service.updateEntity(personne.getIdPersonnel(), updated);

        // Log MODIFICATION with snapshot of OLD values (before update)
        if (currentUser != null) {
            auditSvc.log(personne.getIdPersonnel(), "MODIFICATION",
                    "Modification de " + nom + " " + prenom + " (" + role + ")",
                    currentUser.getIdentifiant(), personne); // personne = old values
        }

        String newPwd = pfNewPassword.getText();
        if (!newPwd.isEmpty()) service.updateEntity2(personne.getIdPersonnel(), newPwd);

        // Notify the agent by email
        updated.setMotDePasse(personne.getMotDePasse()); // keep old pwd in object for email if not changed
        EmailService.sendModificationEmail(updated, newPwd.isEmpty() ? null : newPwd);

        goPersonnel();
    }

    @FXML void delete() {
        // Log SUPPRESSION with snapshot BEFORE deleting
        if (currentUser != null) {
            auditSvc.log(personne.getIdPersonnel(), "SUPPRESSION",
                    "Suppression de " + personne.getNom() + " " + personne.getPrenom()
                    + " (" + personne.getRole() + ")",
                    currentUser.getIdentifiant(), personne); // personne = full snapshot
        }
        // Notify the agent by email before deleting
        EmailService.sendDeletionEmail(personne);
        service.deleteEntity(personne);
        goPersonnel();
    }
    @FXML void cancel()         { goPersonnel(); }
    @FXML void goDashboard()    { navTo("/ResourcesUser/images/fxml/M6_Dashboard.fxml"); }
    @FXML void goPersonnel()    { navTo("/ResourcesUser/images/fxml/M6_Personnel_List.fxml"); }
    @FXML void goInscriptions() { navTo("/ResourcesUser/images/fxml/InscriptionRequests.fxml"); }
    @FXML void goHistorique()   { navTo("/ResourcesUser/images/fxml/Historique.fxml"); }
    @FXML void goShifts()       { navTo("/ResourcesUser/images/fxml/Shifts.fxml"); }
    @FXML void deconnexion()    { navTo("/ResourcesUser/images/fxml/Login.fxml"); }

    private void navTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            if (path.contains("List") && currentUser != null)
                ((M6PersonnelListController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("Dashboard") && currentUser != null)
                ((M6DashboardController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("Historique") && currentUser != null)
                ((HistoriqueController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("InscriptionRequests") && currentUser != null)
                ((InscriptionRequestsController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("Shifts") && currentUser != null)
                ((ShiftsController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) tfNom.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showError(String msg) {
        lblError.setText(msg); lblError.setVisible(true); lblError.setManaged(true);
    }

    private String getRoleBadgeClass(String role) {
        if (role == null) return "badge-xx";
        switch (role) {
            case "Medecin Urgentiste":     return "badge-dr";
            case "Infirmier Triage":       return "badge-tr";
            case "Agent Accueil":          return "badge-aa";
            case "Biologiste Radiologue":  return "badge-br";
            case "Responsable Logistique": return "badge-rl";
            case "Administrateur":         return "badge-ad";
            default:                       return "badge-xx";
        }
    }
}

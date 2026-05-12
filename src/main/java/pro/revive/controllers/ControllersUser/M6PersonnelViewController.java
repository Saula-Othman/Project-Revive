package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;

public class M6PersonnelViewController {

    @FXML private Label lblSidebarTitle;
    @FXML private Label lblSidebarRole;
    @FXML private Label lblPageTitle;
    @FXML private Label lblPageSub;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private Label lblAvatar;
    @FXML private Label lblFullName;
    @FXML private Label lblSubInfo;
    @FXML private Label lblRoleBadge;

    @FXML private Label lblIdentifiant;
    @FXML private Label lblRoleDetail;

    @FXML private Label lblIdentifiantBig;
    @FXML private Label lblInfoNom;
    @FXML private Label lblInfoRole;
    @FXML private Label lblInfoDateNaissance;
    @FXML private Label lblInfoAge;
    @FXML private Label lblInfoTelephone;
    @FXML private Label lblInfoEmail;

    private Personne personne;
    private Personne currentUser;

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    public void setPersonne(Personne p) {
        this.personne = p;
        String fullName = p.getPrenom() + " " + p.getNom();
        String role  = p.getRole() != null ? p.getRole() : "—";
        String ident = p.getIdentifiant() != null ? p.getIdentifiant() : "—";

        lblSidebarTitle.setText(fullName);
        lblSidebarRole.setText(role);

        lblPageTitle.setText("Details — " + fullName);
        lblPageSub.setText("Fiche complete");

        lblAvatar.setText(getInitials(p));
        lblAvatar.setStyle("-fx-text-fill: " + getRoleColorText(role) + "; -fx-font-size: 26px; -fx-font-weight: bold;");
        lblAvatar.getParent().setStyle("-fx-background-color: " + getRoleColorBg(role) + "; -fx-background-radius: 50%; -fx-min-width: 60px; -fx-min-height: 60px; -fx-pref-width: 60px; -fx-pref-height: 60px; -fx-alignment: center;");

        lblFullName.setText(fullName);
        lblSubInfo.setText(ident);

        lblRoleBadge.setText(role);
        lblRoleBadge.getStyleClass().clear();
        lblRoleBadge.getStyleClass().addAll("badge", getRoleBadgeClass(role));

        lblIdentifiant.setText(ident);
        lblRoleDetail.setText(role);

        lblIdentifiantBig.setText(ident);
        lblInfoNom.setText(fullName);
        lblInfoRole.setText(role);

        // Date naissance + âge
        if (lblInfoDateNaissance != null) {
            String dob = p.getDateNaissance() != null ? p.getDateNaissance().toString() : "—";
            lblInfoDateNaissance.setText(dob);
        }
        if (lblInfoAge != null) {
            lblInfoAge.setText(p.getDateNaissance() != null ? p.getAge() + " ans" : "—");
        }
        if (lblInfoTelephone != null) {
            lblInfoTelephone.setText(p.getTelephone() != null && !p.getTelephone().isBlank() ? p.getTelephone() : "—");
        }
        if (lblInfoEmail != null) {
            lblInfoEmail.setText(p.getEmail() != null && !p.getEmail().isBlank() ? p.getEmail() : "—");
        }
    }

    @FXML void edit() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Personnel_Edit.fxml"));
            Parent root = loader.load();
            M6PersonnelEditController ctrl = loader.getController();
            ctrl.setPersonne(personne);
            ctrl.setCurrentUser(currentUser);
            ((Stage) lblFullName.getScene().getWindow()).getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void delete() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Personnel_Delete.fxml"));
            Parent root = loader.load();
            M6PersonnelDeleteController ctrl = loader.getController();
            ctrl.setPersonne(personne);
            ctrl.setCurrentUser(currentUser);
            ((Stage) lblFullName.getScene().getWindow()).getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

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
            Stage stage = (Stage) lblFullName.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applyCSS(Scene scene) {
        URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }

    private String getInitials(Personne p) {
        String n  = p.getNom()    != null && !p.getNom().isEmpty()    ? String.valueOf(p.getNom().charAt(0))    : "?";
        String pr = p.getPrenom() != null && !p.getPrenom().isEmpty() ? String.valueOf(p.getPrenom().charAt(0)) : "?";
        return (n + pr).toUpperCase();
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

    private String getRoleColorBg(String role) {
        if (role == null) return "#F3F4F6";
        switch (role) {
            case "Medecin Urgentiste":     return "#EEF2FF";
            case "Infirmier Triage":       return "#DCFCE7";
            case "Agent Accueil":          return "#FEF3C7";
            case "Biologiste Radiologue":  return "#F3E8FF";
            case "Responsable Logistique": return "#FEE2E2";
            case "Administrateur":         return "#F3F4F6";
            default:                       return "#F3F4F6";
        }
    }

    private String getRoleColorText(String role) {
        if (role == null) return "#6B7280";
        switch (role) {
            case "Medecin Urgentiste":     return "#1A56DB";
            case "Infirmier Triage":       return "#16A34A";
            case "Agent Accueil":          return "#D97706";
            case "Biologiste Radiologue":  return "#9333EA";
            case "Responsable Logistique": return "#DC2626";
            case "Administrateur":         return "#374151";
            default:                       return "#6B7280";
        }
    }
}

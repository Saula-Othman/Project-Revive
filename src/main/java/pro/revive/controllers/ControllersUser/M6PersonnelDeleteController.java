package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.AuditService;
import pro.revive.services.ServicesUser.PersonneService;

public class M6PersonnelDeleteController {

    @FXML private Label lblNomPrenom;
    @FXML private Label lblIdentifiant;
    @FXML private Label lblRole;

    private final PersonneService service = new PersonneService();
    private final AuditService    auditSvc = new AuditService();
    private Personne personne;
    private Personne currentUser;

    public void setCurrentUser(Personne user) { this.currentUser = user; }

    public void setPersonne(Personne p) {
        this.personne = p;
        lblNomPrenom.setText(p.getPrenom() + " " + p.getNom());
        lblIdentifiant.setText(p.getIdentifiant() != null ? p.getIdentifiant() : "—");
        lblRole.setText(p.getRole() != null ? p.getRole() : "—");
        lblRole.getStyleClass().clear();
        lblRole.getStyleClass().addAll("badge", getRoleBadgeClass(p.getRole()));
    }

    @FXML void confirmDelete() {
        if (personne != null) {
            // Log BEFORE deleting so snapshot is saved
            if (currentUser != null) {
                auditSvc.log(personne.getIdPersonnel(), "SUPPRESSION",
                        "Suppression de " + personne.getNom() + " " + personne.getPrenom()
                        + " (" + personne.getRole() + ")",
                        currentUser.getIdentifiant(), personne);
            }
            service.deleteEntity(personne);
            System.out.println("Suppression confirmee pour: " + personne.getNom());
        }
        goPersonnel();
    }

    @FXML void cancel() { goPersonnel(); }
    @FXML void goDashboard() { navTo("/ResourcesUser/images/fxml/M6_Dashboard.fxml"); }
    @FXML void goPersonnel() { navTo("/ResourcesUser/images/fxml/M6_Personnel_List.fxml"); }
    @FXML void deconnexion() { navTo("/ResourcesUser/images/fxml/Login.fxml"); }

    private void navTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            if (path.contains("List") && currentUser != null)
                ((M6PersonnelListController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("Dashboard") && currentUser != null)
                ((M6DashboardController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) lblNomPrenom.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
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

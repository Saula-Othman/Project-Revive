package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class M6DashboardController implements Initializable {

    @FXML private Label    lblWelcome;
    @FXML private Label    lblUserName;
    @FXML private Label    lblUserRole;
    @FXML private Label    lblTotal;
    @FXML private Label    lblMedecins;
    @FXML private Label    lblInfirmiers;
    @FXML private Label    lblAutres;
    @FXML private Label    lblPanelCount;
    @FXML private FlowPane flowStaff;

    private final PersonneService service = new PersonneService();
    private Personne currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) { loadStats(); }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        lblWelcome.setText("Bienvenue, " + user.getPrenom() + " " + user.getNom());
        lblUserName.setText(user.getNom() + " " + user.getPrenom());
        lblUserRole.setText(user.getRole());
    }

    private void loadStats() {
        List<Personne> all = service.getData();
        long medecins   = all.stream().filter(p -> p.getRole() != null && p.getRole().startsWith("Medecin")).count();
        long infirmiers = all.stream().filter(p -> p.getRole() != null && p.getRole().startsWith("Infirmier")).count();
        long autres     = Math.max(0, all.size() - medecins - infirmiers);
        lblTotal.setText(String.valueOf(all.size()));
        lblMedecins.setText(String.valueOf(medecins));
        lblInfirmiers.setText(String.valueOf(infirmiers));
        lblAutres.setText(String.valueOf(autres));
        if (lblPanelCount != null) lblPanelCount.setText(all.size() + " employes");
        flowStaff.getChildren().clear();
        for (Personne p : all) flowStaff.getChildren().add(buildMiniCard(p));
    }

    private VBox buildMiniCard(Personne p) {
        VBox card = new VBox(6);
        card.getStyleClass().add("staff-card");
        Label av = new Label(getInitials(p));
        av.getStyleClass().add("staff-avatar");
        // Avatar background matches badge color
        String avatarColor = getRoleAvatarColor(p.getRole());
        av.setStyle("-fx-background-color: " + avatarColor + "; -fx-text-fill: #ffffff;");
        Label name = new Label(p.getNom() + " " + p.getPrenom());
        name.getStyleClass().add("staff-name");
        Label badge = new Label(p.getRole() != null ? p.getRole() : "—");
        badge.getStyleClass().addAll("badge", getRoleBadgeClass(p.getRole()));
        Label idLbl = new Label(p.getIdentifiant() != null ? p.getIdentifiant() : "");
        idLbl.getStyleClass().add("staff-id");
        card.getChildren().addAll(av, name, badge, idLbl);
        card.setOnMouseClicked(e -> nav("/ResourcesUser/images/fxml/M6_Personnel_View.fxml", loader -> {
            M6PersonnelViewController ctrl = loader.getController();
            ctrl.setPersonne(p);
            ctrl.setCurrentUser(currentUser);
        }));
        return card;
    }

    @FXML void goDashboard()    { loadStats(); }
    @FXML void goPersonnel()    { nav("/ResourcesUser/images/fxml/M6_Personnel_List.fxml", l -> ((M6PersonnelListController)  l.getController()).setCurrentUser(currentUser)); }
    @FXML void goAddPersonnel() { nav("/ResourcesUser/images/fxml/M6_Personnel_Add.fxml", l -> ((M6PersonnelAddController)   l.getController()).setCurrentUser(currentUser)); }
    @FXML void goHistorique()   { nav("/ResourcesUser/images/fxml/Historique.fxml", l -> ((HistoriqueController)           l.getController()).setCurrentUser(currentUser)); }
    @FXML void goShifts()       { nav("/ResourcesUser/images/fxml/Shifts.fxml", l -> ((ShiftsController)               l.getController()).setCurrentUser(currentUser)); }
    @FXML void goInscriptions() { nav("/ResourcesUser/images/fxml/InscriptionRequests.fxml", l -> ((InscriptionRequestsController)  l.getController()).setCurrentUser(currentUser)); }
    @FXML void deconnexion()    { nav("/ResourcesUser/images/fxml/Login.fxml", l -> {}); }

    private void nav(String fxmlPath, LoaderConsumer consumer) {
        try {
            Stage stage = (Stage) lblWelcome.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            consumer.accept(loader);
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FunctionalInterface interface LoaderConsumer {
        void accept(FXMLLoader loader) throws Exception;
    }

    private String getInitials(Personne p) {
        String n  = p.getNom()    != null && !p.getNom().isEmpty()    ? String.valueOf(p.getNom().charAt(0))    : "?";
        String pr = p.getPrenom() != null && !p.getPrenom().isEmpty() ? String.valueOf(p.getPrenom().charAt(0)) : "?";
        return (n + pr).toUpperCase();
    }

    private String getRoleColor(String role) {
        if (role == null) return "#6B7280";
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

    private String getRoleAvatarColor(String role) {
        if (role == null) return "#94A3B8";
        switch (role) {
            case "Medecin Urgentiste":     return "#1E40AF";
            case "Infirmier Triage":       return "#059669";
            case "Agent Accueil":          return "#D97706";
            case "Biologiste Radiologue":  return "#7C3AED";
            case "Responsable Logistique": return "#0891B2";
            case "Administrateur":         return "#475569";
            default:                       return "#94A3B8";
        }
    }
}

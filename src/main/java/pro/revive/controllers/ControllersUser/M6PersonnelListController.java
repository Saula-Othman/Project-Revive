package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class M6PersonnelListController implements Initializable {

    @FXML private TextField  tfSearch;
    @FXML private VBox       vbStaffList;
    @FXML private Label      lblUserName;
    @FXML private Label      lblUserRole;
    @FXML private Label      lblSubtitle;
    @FXML private Label      lblSelectionCount;
    @FXML private Button     btnDeleteSelected;
    @FXML private CheckBox   cbSelectAll;

    @FXML private Button btnFilterTous;
    @FXML private Button btnFilterMedecin;
    @FXML private Button btnFilterInfirmier;
    @FXML private Button btnFilterAgent;
    @FXML private Button btnFilterBiologiste;
    @FXML private Button btnFilterLogistique;
    @FXML private Button btnFilterAdmin;

    private final PersonneService service = new PersonneService();
    private Personne currentUser;
    private String currentRoleFilter = "Tous";

    // Track selected agents
    private final List<Personne> selectedAgents = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        tfSearch.textProperty().addListener((obs, o, n) -> loadCards());
        loadCards();
        updateSelectionUI();
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    // ── Filter handlers ────────────────────────────────────────────
    @FXML void filterTous()       { applyFilter("Tous",                   btnFilterTous); }
    @FXML void filterMedecin()    { applyFilter("Medecin Urgentiste",     btnFilterMedecin); }
    @FXML void filterInfirmier()  { applyFilter("Infirmier Triage",       btnFilterInfirmier); }
    @FXML void filterAgent()      { applyFilter("Agent Accueil",          btnFilterAgent); }
    @FXML void filterBiologiste() { applyFilter("Biologiste Radiologue",  btnFilterBiologiste); }
    @FXML void filterLogistique() { applyFilter("Responsable Logistique", btnFilterLogistique); }
    @FXML void filterAdmin()      { applyFilter("Administrateur",         btnFilterAdmin); }

    private void applyFilter(String role, Button activeBtn) {
        this.currentRoleFilter = role;
        Button[] allBtns = {btnFilterTous, btnFilterMedecin, btnFilterInfirmier,
                            btnFilterAgent, btnFilterBiologiste, btnFilterLogistique, btnFilterAdmin};
        for (Button b : allBtns) if (b != null) b.getStyleClass().remove("active");
        if (activeBtn != null) activeBtn.getStyleClass().add("active");
        selectedAgents.clear();
        if (cbSelectAll != null) cbSelectAll.setSelected(false);
        loadCards();
    }

    // ── Select All ─────────────────────────────────────────────────
    @FXML void handleSelectAll() {
        boolean checked = cbSelectAll.isSelected();
        selectedAgents.clear();
        // Re-render cards with updated checkbox state
        String keyword = tfSearch.getText().trim();
        List<Personne> list = keyword.isEmpty() ? service.getData() : service.getData2(keyword);
        if (!"Tous".equals(currentRoleFilter) && !currentRoleFilter.isEmpty()) {
            list = list.stream().filter(p -> currentRoleFilter.equals(p.getRole())).toList();
        }
        if (checked) selectedAgents.addAll(list);
        renderCards(list);
        updateSelectionUI();
    }

    // ── Delete selected ────────────────────────────────────────────
    @FXML void deleteSelected() {
        if (selectedAgents.isEmpty()) return;
        for (Personne p : new ArrayList<>(selectedAgents)) {
            service.deleteEntity(p);
        }
        selectedAgents.clear();
        if (cbSelectAll != null) cbSelectAll.setSelected(false);
        loadCards();
        updateSelectionUI();
    }

    // ── Load & render ──────────────────────────────────────────────
    private void loadCards() {
        String keyword = tfSearch.getText().trim();
        List<Personne> list = keyword.isEmpty() ? service.getData() : service.getData2(keyword);
        if (!"Tous".equals(currentRoleFilter) && !currentRoleFilter.isEmpty()) {
            list = list.stream().filter(p -> currentRoleFilter.equals(p.getRole())).toList();
        }
        if (lblSubtitle != null) lblSubtitle.setText(list.size() + " employe(s) enregistre(s)");
        renderCards(list);
    }

    private void renderCards(List<Personne> list) {
        vbStaffList.getChildren().clear();
        for (Personne p : list) {
            vbStaffList.getChildren().add(buildPcard(p));
        }
    }

    private HBox buildPcard(Personne p) {
        HBox card = new HBox();
        card.getStyleClass().add("pcard");
        card.setSpacing(10);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Checkbox
        CheckBox cb = new CheckBox();
        cb.setSelected(selectedAgents.stream().anyMatch(s -> s.getIdPersonnel() == p.getIdPersonnel()));
        cb.selectedProperty().addListener((obs, o, selected) -> {
            if (selected) {
                if (selectedAgents.stream().noneMatch(s -> s.getIdPersonnel() == p.getIdPersonnel())) {
                    selectedAgents.add(p);
                }
            } else {
                selectedAgents.removeIf(s -> s.getIdPersonnel() == p.getIdPersonnel());
                if (cbSelectAll != null) cbSelectAll.setSelected(false);
            }
            updateSelectionUI();
        });

        // Color stripe
        Region stripe = new Region();
        stripe.getStyleClass().addAll("stripe", getRoleStripeClass(p.getRole()));

        // Left info
        VBox info = new VBox(3);
        info.getStyleClass().add("pinfo");
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label nameLabel = new Label(p.getNom() + " " + p.getPrenom());
        nameLabel.getStyleClass().add("pname");

        Label idLabel = new Label("Identifiant: " + (p.getIdentifiant() != null ? p.getIdentifiant() : "—")
                + "  |  Role: " + (p.getRole() != null ? p.getRole() : "—"));
        idLabel.getStyleClass().add("pid");

        info.getChildren().addAll(nameLabel, idLabel);

        // Right: badge + actions
        VBox right = new VBox(4);
        right.getStyleClass().add("pright");
        right.setAlignment(javafx.geometry.Pos.TOP_RIGHT);

        HBox badgeRow = new HBox(6);
        badgeRow.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label badge = new Label(p.getRole() != null ? p.getRole() : "—");
        badge.getStyleClass().addAll("badge", getRoleBadgeClass(p.getRole()));
        badgeRow.getChildren().add(badge);

        HBox actions = new HBox(6);
        actions.getStyleClass().add("card-actions");
        actions.setStyle("-fx-padding: 6px 0 0 0;");

        Button btnVoir = new Button("👁 Voir");
        btnVoir.getStyleClass().addAll("act-icon", "act-view");
        btnVoir.setOnAction(e -> openView(p));

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.getStyleClass().addAll("act-icon", "act-edit");
        btnEdit.setOnAction(e -> openEdit(p));

        Button btnDel = new Button("🗑 Supprimer");
        btnDel.getStyleClass().addAll("act-icon", "act-del");
        btnDel.setOnAction(e -> openDelete(p));

        actions.getChildren().addAll(btnVoir, btnEdit, btnDel);
        right.getChildren().addAll(badgeRow, actions);

        card.getChildren().addAll(cb, stripe, info, right);
        return card;
    }

    private void updateSelectionUI() {
        int count = selectedAgents.size();
        if (lblSelectionCount != null) {
            if (count > 0) {
                lblSelectionCount.setText(count + " agent(s) selectionne(s)");
                lblSelectionCount.setVisible(true);
                lblSelectionCount.setManaged(true);
            } else {
                lblSelectionCount.setVisible(false);
                lblSelectionCount.setManaged(false);
            }
        }
        if (btnDeleteSelected != null) {
            btnDeleteSelected.setVisible(count > 0);
            btnDeleteSelected.setManaged(count > 0);
        }
    }

    private void openView(Personne p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Personnel_View.fxml"));
            Parent root = loader.load();
            M6PersonnelViewController ctrl = loader.getController();
            ctrl.setPersonne(p); ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openEdit(Personne p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Personnel_Edit.fxml"));
            Parent root = loader.load();
            M6PersonnelEditController ctrl = loader.getController();
            ctrl.setPersonne(p); ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openDelete(Personne p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Personnel_Delete.fxml"));
            Parent root = loader.load();
            M6PersonnelDeleteController ctrl = loader.getController();
            ctrl.setPersonne(p); ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void goAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Personnel_Add.fxml"));
            Parent root = loader.load();
            M6PersonnelAddController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void goDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/M6_Dashboard.fxml"));
            Parent root = loader.load();
            M6DashboardController ctrl = loader.getController();
            ctrl.setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void goPersonnel() { /* already here */ }

    @FXML void goHistorique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Historique.fxml"));
            Parent root = loader.load();
            ((HistoriqueController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void goShifts() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Shifts.fxml"));
            Parent root = loader.load();
            ((ShiftsController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void goInscriptions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/InscriptionRequests.fxml"));
            Parent root = loader.load();
            ((InscriptionRequestsController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML void deconnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) vbStaffList.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void applyCSS(Scene scene) {
        URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
    }

    private String getRoleStripeClass(String role) {
        if (role == null) return "s5";
        switch (role) {
            case "Medecin Urgentiste":     return "s1";
            case "Infirmier Triage":       return "s4";
            case "Agent Accueil":          return "s2";
            case "Biologiste Radiologue":  return "s3";
            case "Responsable Logistique": return "s1";
            case "Administrateur":         return "s5";
            default:                       return "s5";
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

    // Avatar background color matches badge color
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

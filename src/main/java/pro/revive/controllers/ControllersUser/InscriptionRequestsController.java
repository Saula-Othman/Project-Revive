package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.AuditService;
import pro.revive.services.ServicesUser.EmailService;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class InscriptionRequestsController implements Initializable {

    @FXML private VBox  vbRequests;
    @FXML private Label lblCount;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    private final PersonneService service  = new PersonneService();
    private final AuditService    auditSvc = new AuditService();
    private Personne currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadRequests();
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    private void loadRequests() {
        vbRequests.getChildren().clear();
        List<Personne> requests = service.getPendingRequests();

        if (lblCount != null) lblCount.setText(requests.size() + " demande(s) en attente");

        if (requests.isEmpty()) {
            Label empty = new Label("Aucune demande d'inscription en attente.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 20px;");
            vbRequests.getChildren().add(empty);
            return;
        }

        for (Personne p : requests) {
            HBox row = new HBox(12);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 12px; " +
                         "-fx-padding: 14px 18px; " +
                         "-fx-effect: dropshadow(gaussian,rgba(11,78,162,0.08),8,0,0,2);");

            // Avatar
            Label av = new Label(getInitials(p));
            av.setStyle("-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF; " +
                        "-fx-font-weight: bold; -fx-font-size: 14px; " +
                        "-fx-min-width: 44px; -fx-min-height: 44px; " +
                        "-fx-background-radius: 50%; -fx-alignment: center;");

            // Info
            VBox info = new VBox(3);
            HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

            Label nameLabel = new Label(p.getPrenom() + " " + p.getNom());
            nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1A1D23;");

            Label roleLabel = new Label(p.getRole() != null ? p.getRole() : "—");
            roleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

            Label emailLabel = new Label(p.getEmail() != null ? p.getEmail() : "—");
            emailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #0B4EA2;");

            Label dateLabel = new Label(p.getDateNaissance() != null
                    ? "Né(e) le " + p.getDateNaissance().toString() + " (" + p.getAge() + " ans)" : "");
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

            Label telLabel = new Label(p.getTelephone() != null ? "📞 " + p.getTelephone() : "");
            telLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");

            info.getChildren().addAll(nameLabel, roleLabel, emailLabel, dateLabel, telLabel);

            // Actions
            VBox actions = new VBox(6);
            actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            Button btnAccept = new Button("✓ Accepter");
            btnAccept.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; " +
                               "-fx-background-radius: 8px; -fx-padding: 7px 16px; " +
                               "-fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
            btnAccept.setOnAction(e -> handleAccept(p));

            Button btnReject = new Button("✗ Refuser");
            btnReject.setStyle("-fx-background-color: #E0F2FE; -fx-text-fill: #0369A1; " +
                               "-fx-background-radius: 8px; -fx-padding: 7px 16px; " +
                               "-fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand;");
            btnReject.setOnAction(e -> handleReject(p));

            actions.getChildren().addAll(btnAccept, btnReject);
            row.getChildren().addAll(av, info, actions);
            vbRequests.getChildren().add(row);
        }
    }

    private void handleAccept(Personne p) {
        Personne approved = service.approveRequest(p.getIdPersonnel());
        if (approved != null) {
            // Send approval email with credentials
            EmailService.sendApprovalEmail(approved);
            // Audit log
            if (currentUser != null) {
                auditSvc.log(approved.getIdPersonnel(), "AJOUT",
                        "Inscription acceptee pour " + approved.getNom() + " " + approved.getPrenom()
                        + " (" + approved.getRole() + ")",
                        currentUser.getIdentifiant(), approved);
            }
        }
        loadRequests();
    }

    private void handleReject(Personne p) {
        service.rejectRequest(p.getIdPersonnel());
        EmailService.sendRejectionEmail(p.getEmail(), p.getNom(), p.getPrenom());
        if (currentUser != null) {
            auditSvc.log(0, "SUPPRESSION",
                    "Inscription refusee pour " + p.getNom() + " " + p.getPrenom(),
                    currentUser.getIdentifiant());
        }
        loadRequests();
    }

    private String getInitials(Personne p) {
        String n  = p.getNom()    != null && !p.getNom().isEmpty()    ? String.valueOf(p.getNom().charAt(0))    : "?";
        String pr = p.getPrenom() != null && !p.getPrenom().isEmpty() ? String.valueOf(p.getPrenom().charAt(0)) : "?";
        return (n + pr).toUpperCase();
    }

    @FXML void refresh() { loadRequests(); }
    @FXML void goDashboard() { navTo("/ResourcesUser/images/fxml/M6_Dashboard.fxml"); }
    @FXML void goPersonnel() { navTo("/ResourcesUser/images/fxml/M6_Personnel_List.fxml"); }
    @FXML void deconnexion() { navTo("/ResourcesUser/images/fxml/Login.fxml"); }

    private void navTo(String path) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(path));
            javafx.scene.Parent root = loader.load();
            if (path.contains("Dashboard") && currentUser != null)
                ((M6DashboardController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("List") && currentUser != null)
                ((M6PersonnelListController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) vbRequests.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }
}

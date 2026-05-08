package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.AuditService;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class HistoriqueController implements Initializable {

    @FXML private VBox  vbLogs;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    private final AuditService auditSvc = new AuditService();
    private Personne currentUser;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadLogs();
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    private void loadLogs() {
        vbLogs.getChildren().clear();
        List<String[]> logs = auditSvc.getLogs();

        if (logs.isEmpty()) {
            Label empty = new Label("Aucun historique disponible.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 20px;");
            vbLogs.getChildren().add(empty);
            return;
        }

        for (String[] log : logs) {
            // log: [0:id, 1:nom_agent, 2:action, 3:details, 4:fait_par, 5:date, 6:snapshot]
            HBox row = new HBox(10);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; " +
                         "-fx-padding: 10px 14px; " +
                         "-fx-effect: dropshadow(gaussian,rgba(11,78,162,0.06),6,0,0,2);");

            // Action badge
            Label actionBadge = new Label(log[2]);
            actionBadge.setStyle("-fx-padding: 3px 10px; -fx-background-radius: 20px; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold; " +
                    "-fx-background-color: " + getActionColor(log[2]) + "; " +
                    "-fx-text-fill: #ffffff; -fx-min-width: 100px;");

            // Agent name
            Label agentLabel = new Label(log[1]);
            agentLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1A1D23; -fx-min-width: 150px;");

            // Details
            Label detailsLabel = new Label(log[3]);
            detailsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
            HBox.setHgrow(detailsLabel, javafx.scene.layout.Priority.ALWAYS);

            // Done by
            Label byLabel = new Label("Par: " + log[4]);
            byLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #0B4EA2; -fx-font-weight: bold;");

            // Date
            Label dateLabel = new Label(log[5] != null ? log[5].substring(0, 16) : "");
            dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF; -fx-min-width: 120px;");

            row.getChildren().addAll(actionBadge, agentLabel, detailsLabel, byLabel, dateLabel);

            // Revert button — only for AJOUT, MODIFICATION, SUPPRESSION with snapshot
            String action   = log[2];
            String snapshot = log[6];
            boolean canRevert = (action.equals("AJOUT") || action.equals("MODIFICATION") || action.equals("SUPPRESSION"))
                                && !action.equals("ANNULATION")
                                && !action.equals("CONNEXION");

            if (canRevert) {
                final int logId = Integer.parseInt(log[0]);
                javafx.scene.control.Button btnRevert = new javafx.scene.control.Button("↩ Annuler");
                btnRevert.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; " +
                                   "-fx-background-radius: 6px; -fx-padding: 4px 10px; " +
                                   "-fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");

                // Disable if no snapshot for MODIFICATION/SUPPRESSION
                if ((action.equals("MODIFICATION") || action.equals("SUPPRESSION")) && snapshot.isBlank()) {
                    btnRevert.setDisable(true);
                    btnRevert.setStyle(btnRevert.getStyle() + "-fx-opacity: 0.5;");
                }

                btnRevert.setOnAction(e -> handleRevert(logId, action));
                row.getChildren().add(btnRevert);
            }

            vbLogs.getChildren().add(row);
        }
    }

    private void handleRevert(int logId, String action) {
        // Confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer l'annulation");
        alert.setHeaderText("Annuler cette action ?");
        alert.setContentText(getRevertMessage(action));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String faitPar = currentUser != null ? currentUser.getIdentifiant() : "admin";
            boolean success = auditSvc.revert(logId, faitPar);
            if (success) {
                showInfo("Action annulee avec succes !");
                loadLogs(); // refresh
            } else {
                showError("Impossible d'annuler cette action. Donnees insuffisantes.");
            }
        }
    }

    private String getRevertMessage(String action) {
        switch (action) {
            case "SUPPRESSION":  return "L'agent supprime sera restaure dans le systeme.";
            case "AJOUT":        return "L'agent ajoute sera supprime du systeme.";
            case "MODIFICATION": return "Les informations de l'agent seront restaurees a leur etat precedent.";
            default:             return "Cette action sera annulee.";
        }
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succes");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private String getActionColor(String action) {
        if (action == null) return "#94A3B8";
        switch (action) {
            case "AJOUT":        return "#059669";
            case "MODIFICATION": return "#0891B2";
            case "SUPPRESSION":  return "#38BDF8";
            case "CONNEXION":    return "#7C3AED";
            case "ANNULATION":   return "#D97706";
            default:             return "#94A3B8";
        }
    }

    @FXML void refresh() { loadLogs(); }

    @FXML void goDashboard() { navTo("/ResourcesUser/images/fxml/M6_Dashboard.fxml"); }
    @FXML void goPersonnel() { navTo("/ResourcesUser/images/fxml/M6_Personnel_List.fxml"); }
    @FXML void deconnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) vbLogs.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            if (path.contains("Dashboard") && currentUser != null)
                ((M6DashboardController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("List") && currentUser != null)
                ((M6PersonnelListController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) vbLogs.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}

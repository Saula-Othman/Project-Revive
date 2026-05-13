package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.AdmissionDAO;
import pro.revive.daoAdmission.AmbulanceDAO;
import pro.revive.daoAdmission.PatientDAO;
import pro.revive.entities.EntitiesAdmission.Admission;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label cardAdmissions;
    @FXML private Label cardAttente;
    @FXML private Label cardAmbulances;
    @FXML private Label cardTotalPatients;
    @FXML private VBox admissionCardsBox;

    private final AdmissionDAO admDAO = new AdmissionDAO();
    private final PatientDAO patDAO = new PatientDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadData();
    }

    private void loadData() {
        try {
            cardAdmissions.setText(String.valueOf(admDAO.countToday()));
            cardAttente.setText(String.valueOf(admDAO.countWaiting()));
            cardTotalPatients.setText(String.valueOf(patDAO.findAll().size()));
            cardAmbulances.setText(String.valueOf(new AmbulanceDAO().findActiveAmbulances().size()));

            admissionCardsBox.getChildren().clear();
            List<Admission> today = admDAO.findTodayAdmissions();
            if (today.isEmpty()) {
                Label empty = new Label("Aucune admission aujourd'hui");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-padding: 20 10 20 10;");
                admissionCardsBox.getChildren().add(empty);
            } else {
                for (Admission a : today) {
                    admissionCardsBox.getChildren().add(buildAdmissionCard(a));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private HBox buildAdmissionCard(Admission a) {
        String priorite = a.getPrioriteInitiale() != null ? a.getPrioriteInitiale() : "";
        String borderColor;
        if      (priorite.equals("Critique")) borderColor = "#dc2626";
        else if (priorite.equals("Urgent"))   borderColor = "#ea580c";
        else if (priorite.equals("Modéré"))   borderColor = "#ca8a04";
        else                                   borderColor = "#0D629C";

        HBox card = new HBox(12);
        card.setPadding(new Insets(12, 14, 12, 14));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: 0 0 0 4;" +
                        "-fx-border-radius: 0 6 6 0;" +
                        "-fx-background-radius: 6;"
        );
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.08));
        shadow.setRadius(6);
        shadow.setOffsetY(2);
        card.setEffect(shadow);

        // Avatar initiales
        String nomComplet = (a.getPatient() != null) ? a.getPatient().getNomComplet() : "?";
        String initiales = getInitiales(nomComplet);
        Label avatar = new Label(initiales);
        avatar.setMinSize(36, 36);
        avatar.setPrefSize(36, 36);
        avatar.setMaxSize(36, 36);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(
                "-fx-background-color: #0D629C;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 12px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 18;"
        );

        // Colonne centrale
        String sousTitre = "";
        if (a.getModeArrivee() != null)    sousTitre += a.getModeArrivee();
        if (a.getMotifAdmission() != null) {
            String motif = a.getMotifAdmission();
            if (motif.length() > 60) motif = motif.substring(0, 57) + "\u2026";
            sousTitre += (sousTitre.isEmpty() ? "" : " \u2014 ") + motif;
        }
        Label nomLabel = new Label(nomComplet);
        nomLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1e293b;");
        Label detailLabel = new Label(sousTitre);
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        VBox centre = new VBox(2, nomLabel, detailLabel);
        HBox.setHgrow(centre, Priority.ALWAYS);

        // Badge statut + heure
        String statut = a.getStatut() != null ? a.getStatut() : "";
        String badgeColor;
        if      (statut.equals("En attente"))      badgeColor = "#0D629C";
        else if (statut.equals("En triage"))       badgeColor = "#ea580c";
        else if (statut.equals("En consultation")) badgeColor = "#7c3aed";
        else                                        badgeColor = "#64748b";

        Label badgeLabel = new Label(statut.isEmpty() ? "\u2014" : statut);
        badgeLabel.setStyle(
                "-fx-background-color: " + badgeColor + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 10px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 3 8 3 8;" +
                        "-fx-background-radius: 10;"
        );

        String heure = (a.getDateAdmission() != null) ? a.getDateAdmission().format(FMT) : "\u2014";
        Label heureLabel = new Label(heure);
        heureLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        VBox droite = new VBox(4, badgeLabel, heureLabel);
        droite.setAlignment(Pos.CENTER_RIGHT);

        card.getChildren().addAll(avatar, centre, droite);
        return card;
    }

    private String getInitiales(String nomComplet) {
        if (nomComplet == null || nomComplet.isBlank()) return "?";
        String[] parts = nomComplet.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    @FXML private void quickNewPatient()    { openDialog("/ResourceAdmission/urgence/fxml/PatientForm.fxml", "Nouveau Patient"); }
    @FXML private void quickNewAdmission() { openDialog("/ResourceAdmission/urgence/fxml/AdmissionForm.fxml", "Nouvelle Admission"); }
    @FXML private void quickAmbulances()   { if (MainController.getInstance() != null) MainController.getInstance().showAmbulances(); }
    @FXML private void showAllAdmissions() { if (MainController.getInstance() != null) MainController.getInstance().showAdmissions(); }

    private void openDialog(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(title);
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            loadData();
            if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
        } catch (Exception e) { e.printStackTrace(); }
    }
}

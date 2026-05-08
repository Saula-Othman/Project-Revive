package pro.revive.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pro.revive.Navigator;
import pro.revive.entities.Salle;
import pro.revive.entities.Triage;
import pro.revive.services.SalleService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class SalleViewController implements Initializable {

    @FXML private Label lblUserName, lblPageTitle, lblPageSub;
    @FXML private Label lblSideTitle, lblSideOccup, lblSideType;
    @FXML private Label lblSalleName, lblSalleInfo, lblSalleStatut;
    @FXML private Label lblNombreActuel, lblSurCapacite, lblPct;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblCapaciteMax, lblPlacesLibres, lblEnAttente, lblNiveauGravite;
    @FXML private Label lblPatientsTitle;
    @FXML private VBox patientRows;
    @FXML private Label statActifs, statLibres, statConsult;

    private final SalleService salleService = new SalleService();
    private Salle current;
    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        loadData();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML public void refresh() { loadData(); }

    private void loadData() {
        current = salleService.getById(Navigator.currentSalleId);
        if (current == null) { Navigator.goTo("Salle_List"); return; }

        lblPageTitle.setText(current.getNomSalle() + " — Details");
        lblPageSub.setText(current.getTypeSalle() + " • Lecture seule");
        lblSideTitle.setText(current.getNomSalle());
        lblSideType.setText(current.getTypeSalle());

        int max = current.getCapaciteMax();

        lblSalleName.setText(current.getNomSalle());
        lblSalleInfo.setText(current.getTypeSalle() + " • Niveau Gravite: " + current.getNiveauGraviteCible());
        lblCapaciteMax.setText(String.valueOf(max));
        lblEnAttente.setText(String.valueOf(current.getPatientsEnAttente()));
        lblNiveauGravite.setText("Niv." + current.getNiveauGraviteCible());

        // Real patient count — authoritative source (stored counter can be stale)
        List<Triage> patientsInRoom = salleService.getPatientsByRoom(current.getIdSalle());
        int act    = patientsInRoom.size();
        int libres = Math.max(0, max - act);
        double pct = max > 0 ? (double) act / max : 0;
        int pctInt = (int)(pct * 100);

        lblNombreActuel.setText(String.valueOf(act));
        lblSurCapacite.setText("sur " + max);
        lblPct.setText(pctInt + "%");
        progressBar.setProgress(pct);
        progressBar.getStyleClass().setAll("big-prog", pct >= 1.0 ? "pf-red" : pct >= 0.7 ? "pf-orange" : "pf-green");
        lblSalleStatut.setText(act >= max ? "PLEINE" : "DISPONIBLE");
        lblSalleStatut.getStyleClass().setAll("rstatus", act >= max ? "rs-full" : "rs-ok");
        lblSideOccup.setText(pctInt + "% occupe");
        lblPlacesLibres.setText(String.valueOf(libres));

        // Resync stale DB counter in background so other screens stay accurate too
        pro.revive.utils.AppExecutor.run(() -> salleService.resyncNombreActuel(current.getIdSalle()));

        long consult = patientsInRoom.stream().filter(t -> "InConsultation".equals(t.getPatientState())).count();

        statActifs.setText(String.valueOf(act));
        statLibres.setText(String.valueOf(libres));
        statConsult.setText(String.valueOf(consult));
        lblPatientsTitle.setText("Patients dans cette Salle (" + patientsInRoom.size() + ")");

        patientRows.getChildren().clear();
        if (patientsInRoom.isEmpty()) {
            Label empty = new Label("Aucun patient dans cette salle.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 10px;");
            patientRows.getChildren().add(empty);
        } else {
            for (Triage t : patientsInRoom) patientRows.getChildren().add(buildPatientRow(t));
        }
    }

    private HBox buildPatientRow(Triage t) {
        HBox row = new HBox();
        row.getStyleClass().add("patient-row");

        Region stripe = new Region();
        stripe.getStyleClass().addAll("pr-stripe", "s" + t.getNiveauFinal());
        stripe.setPrefWidth(4); stripe.setMinWidth(4);

        VBox info = new VBox(3);
        info.getStyleClass().add("pr-info");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(t.getNomPatient() + " " + t.getPrenomPatient());
        name.getStyleClass().add("pr-name");
        Label vitals = new Label(
            "Pouls: " + t.getConstancesPouls() + " | Temp: " + t.getConstancesTemperature() + " | SpO2: " + t.getSpo2() + "%"
        );
        vitals.getStyleClass().add("pr-vitals");
        info.getChildren().addAll(name, vitals);

        VBox right = new VBox(4);
        right.getStyleClass().add("pr-right");
        right.setAlignment(Pos.TOP_RIGHT);

        Label badge = new Label("N" + t.getNiveauFinal());
        badge.getStyleClass().addAll("badge", "b" + t.getNiveauFinal());
        badge.setStyle("-fx-font-size: 10px;");
        Label state = new Label(t.getPatientState() != null ? t.getPatientState() : "N/A");
        state.getStyleClass().addAll("state-tag", "InRoom".equals(t.getPatientState()) ? "state-in" : "state-consult");
        right.getChildren().addAll(badge, state);

        row.getChildren().addAll(stripe, info, right);
        return row;
    }

    private void stopAutoRefresh() { if (autoRefresh != null) autoRefresh.stop(); }

    @FXML public void goDashboard()    { stopAutoRefresh(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList()   { stopAutoRefresh(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { stopAutoRefresh(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { stopAutoRefresh(); Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { stopAutoRefresh(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goSurveillance() { stopAutoRefresh(); Navigator.goTo("Surveillance"); }
}

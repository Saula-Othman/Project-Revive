package pro.revive.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pro.revive.Navigator;
import pro.revive.entities.Triage;
import pro.revive.services.TriageService;
import pro.revive.services.GravityCalculator;
import pro.revive.services.RoomAssignmentService;
import pro.revive.utils.UIUtils;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class TriageViewController implements Initializable {

    @FXML private Label lblUserName, lblPageTitle, lblPageSub;
    @FXML private Label lblSideTitle, lblSideNiveau;
    @FXML private Label lblPatientNom, lblPatientInfo, lblNiveauBadge, lblStateBadge, lblContagionBadge, lblSalle, lblTimer;
    @FXML private GridPane vitalsGrid;
    @FXML private Label lblScoreVal, lblNiveauTxt, lblAnalyse, lblSymptomes, lblDouleur;
    @FXML private VBox analyseBox;
    @FXML private VBox timelineBox;
    @FXML private Label lblPersonnel, lblDateTriage;

    private final TriageService          triageService = new TriageService();
    private final RoomAssignmentService  roomService   = new RoomAssignmentService();
    private Triage current;
    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        loadTriage();
        autoRefresh = new Timeline(new KeyFrame(javafx.util.Duration.seconds(30), e -> loadTriage()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML public void refresh() { loadTriage(); }

    private void loadTriage() {
        current = triageService.getById(Navigator.currentTriageId);
        if (current == null) { Navigator.goTo("Triage_List"); return; }

        lblPageTitle.setText("Triage #" + current.getIdTriage() + " — Details");
        lblPageSub.setText(current.getNomPatient() + " " + current.getPrenomPatient());
        lblSideTitle.setText("Triage #" + current.getIdTriage());
        lblSideNiveau.setText("Niveau " + current.getNiveauFinal() + " — " + GravityCalculator.levelLabel(current.getNiveauFinal()));

        lblPatientNom.setText(current.getNomPatient() + " " + current.getPrenomPatient());
        lblPatientInfo.setText("Admission #" + current.getIdAdmission() + " • Triage ID: " + current.getIdTriage());
        lblNiveauBadge.setText("Niveau " + current.getNiveauFinal());
        lblNiveauBadge.getStyleClass().setAll("badge", "b" + current.getNiveauFinal());
        lblStateBadge.setText(etatFrancais(current.getPatientState()));

        // Badge contagion
        String flag = current.getContagionFlag();
        if (flag != null && !flag.equals("aucun")) {
            lblContagionBadge.setVisible(true);
            lblContagionBadge.setManaged(true);
            if (flag.equals("confirme")) {
                lblContagionBadge.setText("◉ Contagieux — Isolement Requis");
                lblContagionBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-padding: 3px 10px; -fx-background-radius: 6px;");
            } else {
                lblContagionBadge.setText("◎ Potentiellement Contagieux");
                lblContagionBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-color: #FEF9C3; -fx-text-fill: #854D0E; -fx-padding: 3px 10px; -fx-background-radius: 6px;");
            }
        } else {
            lblContagionBadge.setVisible(false);
            lblContagionBadge.setManaged(false);
        }
        lblSalle.setText("Salle: " + (current.getNomSalle() != null ? current.getNomSalle() : "En Attente"));

        lblScoreVal.setText(String.valueOf(current.getScoreCalcule()));
        lblNiveauTxt.setText(GravityCalculator.levelLabel(current.getNiveauFinal()));
        lblAnalyse.setText(current.getAnalyseAuto() != null ? current.getAnalyseAuto() : "N/A");
        lblSymptomes.setText(current.getSymptomes() != null ? current.getSymptomes() : "Aucun symptome renseigne.");
        lblDouleur.setText(current.getScoreDouleur() + "/10");
        lblDateTriage.setText(current.getDateHeureTriage() != null ? current.getDateHeureTriage().toString() : "N/A");
        lblPersonnel.setText("Triage par: Personnel #" + current.getIdPersonnel());

        if (current.getDateHeureTriage() != null) {
            long minutes = Duration.between(current.getDateHeureTriage(), LocalDateTime.now()).toMinutes();
            if (minutes < 60) {
                lblTimer.setText(minutes + " min");
            } else {
                lblTimer.setText((minutes / 60) + "h " + (minutes % 60) + "min");
            }
        } else {
            lblTimer.setText("N/A");
        }



        buildVitalsGrid();
        buildTimeline();
    }

    // ── Vital grid builder ──────────────────────────────────────
    private void buildVitalsGrid() {
        vitalsGrid.getChildren().clear();
        vitalsGrid.setHgap(12);
        vitalsGrid.setVgap(12);

        int p     = current.getConstancesPouls();
        int taSys = current.getConstancesTaSys();
        int taDia = current.getConstancesTaDia();
        float temp = current.getConstancesTemperature();
        int spo2  = current.getSpo2();
        float gl  = current.getGlycemie();
        int gcs   = current.getGcsScore();
        int fr    = current.getFrequenceRespiratoire();

        // Icons: plain BMP Unicode — render in every Java font, colored via CSS
        // ♥ ↑ ◉ ○  ↓ ◆ ★ ≈
        addVitalCard(vitalsGrid, 0, 0, "♥", "Pouls",
                p + "", "bpm",
                GravityCalculator.getPulseStatus(p), pulseLabel(p));

        addVitalCard(vitalsGrid, 1, 0, "↑", "Tension Sys.",
                taSys + "", "mmHg",
                GravityCalculator.getTensionStatus(taSys), taSysLabel(taSys));

        addVitalCard(vitalsGrid, 2, 0, "◉", "Temperature",
                temp + "", "°C",
                GravityCalculator.getTemperatureStatus(temp), tempLabel(temp));

        addVitalCard(vitalsGrid, 3, 0, "○", "SpO2",
                spo2 + "", "%",
                GravityCalculator.getSpo2Status(spo2), spo2Label(spo2));

        addVitalCard(vitalsGrid, 0, 1, "↓", "Tension Dia.",
                taDia + "", "mmHg",
                GravityCalculator.getTensionStatus(taDia), taDiaLabel(taDia));

        addVitalCard(vitalsGrid, 1, 1, "◆", "Glycemie",
                gl + "", "g/L",
                GravityCalculator.getGlycemieStatus(gl), glycLabel(gl));

        addVitalCard(vitalsGrid, 2, 1, "★", "GCS Score",
                gcs + "", "/15",
                GravityCalculator.getGcsStatus(gcs), gcsLabel(gcs));

        addVitalCard(vitalsGrid, 3, 1, "≈", "Freq. Resp.",
                fr + "", "/min",
                GravityCalculator.getFreqRespStatus(fr), freqLabel(fr));
    }

    // ── Clinical status labels ───────────────────────────────────
    private String pulseLabel(int v)   { return v<40||v>150?"CRITIQUE":v<60?"BAS":v>100?"ÉLEVÉ":""; }
    private String taSysLabel(int v)   { return v<80||v>180?"CRITIQUE":v<90?"BAS":v>140?"ÉLEVÉ":""; }
    private String taDiaLabel(int v)   { return v<40||v>120?"CRITIQUE":v<=60?"BAS":v>90?"ÉLEVÉ":""; }
    private String tempLabel(float v)  { return v<35||v>40?"CRITIQUE":v<36?"BAS":v>=38?"ÉLEVÉE":""; }
    private String spo2Label(int v)    { return v<85?"CRITIQUE":v<92?"BAS":""; }
    private String glycLabel(float v)  { return v<0.5f||v>3f?"CRITIQUE":v<0.7f?"BAS":v>2f?"ÉLEVÉE":""; }
    private String gcsLabel(int v)     { return v<9?"CRITIQUE":v<13?"RÉDUIT":""; }
    private String freqLabel(int v)    { return v<10||v>30?"CRITIQUE":v<12?"BAS":v>20?"ÉLEVÉ":""; }

    // ── Card builder ────────────────────────────────────────────
    private void addVitalCard(GridPane grid, int col, int row,
                               String icon, String label,
                               String val,  String unit,
                               String cssStatus, String badgeText) {

        String accentColor = cssStatus.equals("crit") ? "#EF4444"
                           : cssStatus.equals("warn") ? "#F59E0B"
                           : "#1A56DB";

        VBox card = new VBox(4);
        card.getStyleClass().add("vital-card");
        if (!cssStatus.isEmpty()) card.getStyleClass().add(cssStatus);
        card.setAlignment(Pos.CENTER);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setPadding(new Insets(14, 10, 12, 10));

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 28px; -fx-text-fill: " + accentColor + ";");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("vital-lbl");

        Region sep = new Region();
        sep.setStyle("-fx-background-color: " + accentColor + "; -fx-opacity: 0.25;");
        sep.setPrefHeight(1); sep.setMinHeight(1);

        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + accentColor + ";");

        Label unitLbl = new Label(unit);
        unitLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        card.getChildren().addAll(iconLbl, lbl, sep, valLbl, unitLbl);

        if (!badgeText.isEmpty()) {
            Label badge = new Label(badgeText);
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-padding: 1 5; " +
                "-fx-background-radius: 4; -fx-background-color: " + accentColor + "22; " +
                "-fx-text-fill: " + accentColor + ";");
            card.getChildren().add(badge);
        }

        GridPane.setConstraints(card, col, row);
        grid.getChildren().add(card);
    }

    // ── Timeline builder ────────────────────────────────────────
    private void buildTimeline() {
        timelineBox.getChildren().clear();
        addTimelineStep("Admission",
            current.getDateHeureTriage() != null ? current.getDateHeureTriage().toString() : "N/A",
            true);
        addTimelineStep("Triage effectue",
            "Niveau " + current.getNiveauFinal(),
            true);
        String salle = current.getNomSalle();
        addTimelineStep("Salle assignee",
            salle != null ? salle : "En attente d'assignation",
            salle != null);
        String state = current.getPatientState();
        boolean discharged = "Discharged".equals(state) || "Cancelled".equals(state) || "LeftWithoutSeen".equals(state);
        addTimelineStep("Sortie / Fin",
            discharged ? etatFrancais(state) : "En cours de prise en charge",
            discharged);
    }

    private void addTimelineStep(String title, String sub, boolean done) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.setStyle("-fx-padding: 0 0 10 0;");

        Label dot = new Label(done ? "\u2714" : "\u25CB");
        dot.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (done ? "#22C55E" : "#94A3B8") + "; -fx-min-width: 20px;");

        VBox info = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (done ? "#1E293B" : "#94A3B8") + ";");
        Label s = new Label(sub);
        s.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
        info.getChildren().addAll(t, s);
        row.getChildren().addAll(dot, info);
        timelineBox.getChildren().add(row);
    }

    private String etatFrancais(String state) {
        return UIUtils.etatFrancais(state);
    }

    private void stopAutoRefresh() { if (autoRefresh != null) autoRefresh.stop(); }

    @FXML public void goEdit()      { stopAutoRefresh(); Navigator.goToTriageEdit(current.getIdTriage()); }
    @FXML public void goDelete()    { stopAutoRefresh(); Navigator.goToTriageDelete(current.getIdTriage()); }
    @FXML public void changerEtat() { stopAutoRefresh(); Navigator.goToTriageEdit(current.getIdTriage()); }
    @FXML public void decharger() {
        try {
            roomService.freeRoom(current.getIdTriage());
            loadTriage();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR,
                    "Echec de la decharge: " + ex.getMessage());
            a.setTitle("Erreur");
            a.showAndWait();
        }
    }
    @FXML public void goDashboard()    { stopAutoRefresh(); Navigator.goTo("Dashboard"); }
    @FXML public void goTriageList()   { stopAutoRefresh(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { stopAutoRefresh(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { stopAutoRefresh(); Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { stopAutoRefresh(); Navigator.goTo("Dashboard"); }
    @FXML public void goSurveillance() { stopAutoRefresh(); Navigator.goTo("Surveillance"); }
}

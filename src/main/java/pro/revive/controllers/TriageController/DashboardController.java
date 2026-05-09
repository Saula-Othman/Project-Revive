package pro.revive.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.util.Duration;
import pro.revive.Navigator;
import pro.revive.utils.AppExecutor;
import pro.revive.entities.Salle;
import pro.revive.entities.Triage;
import pro.revive.services.AlertService;
import pro.revive.services.EpidemiologicalDetector;
import pro.revive.services.SalleService;
import pro.revive.services.TriageService;
import pro.revive.services.WHOFeedService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DashboardController implements Initializable {

    @FXML private Label lblDate;
    @FXML private Label lblUserName;
    @FXML private Label alertCount;
    @FXML private HBox  alertBar;
    @FXML private Label alertMsg;
    @FXML private Label statTotal;
    @FXML private Label statCritical;
    @FXML private Label statWaiting;
    @FXML private Label statSalles;
    @FXML private Label patientCount;
    @FXML private VBox  patientList;
    @FXML private VBox  salleList;

    // Surveillance summary cards
    @FXML private Label lblDashThreatIcon;
    @FXML private Label lblDashThreatLevel;
    @FXML private Label lblDashThreatDesc;
    @FXML private Label lblDashSaison;
    @FXML private VBox  whoMiniBox;

    private final TriageService           triageService  = new TriageService();
    private final SalleService            salleService   = new SalleService();
    private final AlertService            alertService   = new AlertService();
    private final EpidemiologicalDetector detector       = new EpidemiologicalDetector();
    private final WHOFeedService          whoService     = new WHOFeedService();
    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        lblDate.setText("Aujourd'hui " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        loadData();
        loadSurveillanceCards();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> { loadData(); loadSurveillanceCards(); }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    public void refresh() { loadData(); loadSurveillanceCards(); }

    // ── Surveillance summary cards (background thread) ──────────
    private void loadSurveillanceCards() {
        AppExecutor.run(() -> {
            try {
                // 1. Threat level — try cache first, then recalculate
                List<WHOFeedService.WHOAlert> whoAlerts = whoService.fetchAlerts();
                EpidemiologicalDetector.ThreatLevel threat = detector.calculerNiveauMenace(whoAlerts);
                EpidemiologicalDetector.SeasonInfo saison  = detector.getSaisonActuelle();

                // 2. Top 3 WHO headlines
                List<WHOFeedService.WHOAlert> top3 = whoAlerts.size() > 3 ? whoAlerts.subList(0, 3) : whoAlerts;

                Platform.runLater(() -> {
                    // ── Threat card ──
                    String icone = threatIcon(threat.niveau);
                    lblDashThreatIcon.setText(icone);
                    lblDashThreatIcon.setStyle("-fx-font-size: 36px; -fx-text-fill: " + threat.couleur + ";");
                    lblDashThreatLevel.setText("Niveau " + threat.niveau + " — " + threat.label);
                    lblDashThreatLevel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + threat.couleur + ";");
                    lblDashThreatDesc.setText(threat.description);
                    lblDashSaison.setText(saisonEmoji(saison.nom) + " " + saison.nom);

                    // ── WHO mini-feed ──
                    whoMiniBox.getChildren().clear();
                    if (top3.isEmpty()) {
                        Label none = new Label("Aucune alerte récente.");
                        none.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-style: italic;");
                        whoMiniBox.getChildren().add(none);
                    } else {
                        for (WHOFeedService.WHOAlert a : top3) {
                            whoMiniBox.getChildren().add(buildWHOMiniItem(a));
                        }
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblDashThreatLevel.setText("Surveillance indisponible");
                    lblDashThreatDesc.setText("Erreur de chargement");
                });
            }
        });
    }

    private HBox buildWHOMiniItem(WHOFeedService.WHOAlert a) {
        HBox row = new HBox(8);
        row.setStyle("-fx-padding: 6px 8px; -fx-background-color: #F8FAFC; -fx-background-radius: 8px;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label icon = new Label(a.getSyndromeIcon());
        icon.setStyle("-fx-font-size: 14px;");

        VBox info = new VBox(1);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        // Truncate title to ~50 chars for the mini card
        String titre = a.titre != null && a.titre.length() > 52 ? a.titre.substring(0, 50) + "…" : a.titre;
        Label lblTitre = new Label(titre);
        lblTitre.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        lblTitre.setWrapText(false);

        String region = a.region != null && !a.region.isEmpty() ? "🌍 " + a.region : "";
        Label lblRegion = new Label(region);
        lblRegion.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");

        info.getChildren().addAll(lblTitre, lblRegion);

        if (a.isRecent) {
            Label badge = new Label("NOUVEAU");
            badge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: #FEE2E2; " +
                           "-fx-text-fill: #DC2626; -fx-padding: 1px 5px; -fx-background-radius: 4px;");
            badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            row.getChildren().addAll(icon, info, badge);
        } else {
            row.getChildren().addAll(icon, info);
        }
        return row;
    }

    @FXML
    public void closeAlert() {
        alertBar.setVisible(false);
        alertBar.setManaged(false);
    }

    private void loadData() {
        AppExecutor.run(() -> {
            List<Triage> triages = triageService.getData();
            List<Salle>  salles  = salleService.getData();

            alertService.checkCriticalWaiting();
            alertService.checkRoomOverflow();

            long critical    = triages.stream().filter(t -> t.getNiveauFinal() == 1).count();
            long waiting     = triages.stream().filter(t -> "WaitingRoom".equals(t.getPatientState())).count();
            long disponibles = salles.stream().filter(Salle::isAvailable).count();

            Platform.runLater(() -> {
                statTotal.setText(String.valueOf(triages.size()));
                statCritical.setText(String.valueOf(critical));
                statWaiting.setText(String.valueOf(waiting));
                statSalles.setText(String.valueOf(disponibles));
                patientCount.setText(triages.size() + " patients");
                alertCount.setText(critical + " alerte(s) critique(s)");

                if (critical > 0) {
                    alertMsg.setText("ALERTE : " + critical + " patient(s) Niveau 1 en attente — Action immediate requise !");
                    alertBar.setVisible(true);
                    alertBar.setManaged(true);
                } else {
                    alertBar.setVisible(false);
                    alertBar.setManaged(false);
                }

                patientList.getChildren().clear();
                for (Triage t : triages) patientList.getChildren().add(buildPatientCard(t));

                salleList.getChildren().clear();
                for (Salle s : salles) salleList.getChildren().add(buildSalleCard(s));
            });
        });
    }

    private HBox buildPatientCard(Triage t) {
        HBox card = new HBox();
        card.getStyleClass().add("pcard");
        card.setSpacing(0);

        // Stripe
        Region stripe = new Region();
        stripe.getStyleClass().addAll("stripe", "s" + t.getNiveauFinal());
        stripe.setPrefWidth(5);
        stripe.setMinWidth(5);

        // Info
        VBox info = new VBox(3);
        info.getStyleClass().add("pinfo");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(t.getNomPatient() + " " + t.getPrenomPatient());
        name.getStyleClass().add("pname");

        Label vitals = new Label(
            "Pouls: " + t.getConstancesPouls() + " bpm  |  " +
            "Temp: " + t.getConstancesTemperature() + " C  |  " +
            "SpO2: " + t.getSpo2() + "%"
        );
        vitals.getStyleClass().add("pvitals");

        Label symptom = new Label(t.getSymptomes() != null ? t.getSymptomes() : "");
        symptom.getStyleClass().add("psymptom");

        info.getChildren().addAll(name, vitals, symptom);

        // Right
        VBox right = new VBox(6);
        right.getStyleClass().add("pright");
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Label badge = new Label("Niveau " + t.getNiveauFinal());
        badge.getStyleClass().addAll("badge", "b" + t.getNiveauFinal());

        String salleText = t.getNomSalle() != null ? t.getNomSalle() : "En Attente";
        Label salleTag = new Label(salleText);
        salleTag.getStyleClass().add("salle-tag");

        HBox actions = new HBox(6);
        actions.getStyleClass().add("card-actions");
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button btnView = new Button("👁  Voir");
        btnView.getStyleClass().addAll("act-icon", "act-view");
        btnView.setOnAction(e -> { Navigator.currentTriageId = t.getIdTriage(); Navigator.goTo("Triage_View"); });
        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().addAll("act-icon", "act-edit");
        btnEdit.setOnAction(e -> { Navigator.currentTriageId = t.getIdTriage(); Navigator.goTo("Triage_Edit"); });
        Button btnDel = new Button("🗑  Suppr.");
        btnDel.getStyleClass().addAll("act-icon", "act-del");
        btnDel.setOnAction(e -> { Navigator.currentTriageId = t.getIdTriage(); Navigator.goTo("Triage_Delete"); });
        actions.getChildren().addAll(btnView, btnEdit, btnDel);

        right.getChildren().addAll(badge, salleTag, actions);

        card.getChildren().addAll(stripe, info, right);
        return card;
    }

    private VBox buildSalleCard(Salle s) {
        VBox card = new VBox(6);
        card.getStyleClass().add("rcard");

        HBox hdr = new HBox(8);
        hdr.getStyleClass().add("rhdr");
        hdr.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox nameBox = new VBox(2);
        Label rname = new Label(s.getNomSalle());
        rname.getStyleClass().add("rname");
        Label rtype = new Label(s.getTypeSalle() + " • Niv." + s.getNiveauGraviteCible());
        rtype.getStyleClass().add("rtype");
        nameBox.getChildren().addAll(rname, rtype);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label cnt = new Label(s.getNombreActuel() + "/" + s.getCapaciteMax());
        cnt.getStyleClass().add("rcnt");

        hdr.getChildren().addAll(nameBox, spacer, cnt);

        double progress = s.getCapaciteMax() > 0 ? (double) s.getNombreActuel() / s.getCapaciteMax() : 0;
        ProgressBar pb = new ProgressBar(progress);
        pb.setMaxWidth(Double.MAX_VALUE);
        String pbStyle = progress >= 1.0 ? "pf-red" : progress >= 0.7 ? "pf-orange" : "pf-green";
        pb.getStyleClass().addAll("prog-wrap", pbStyle);

        // BUG-6 fix: null-safe statusText so null statut doesn't NPE on toUpperCase()
        String statusText = s.getStatut() != null ? s.getStatut() : "INCONNU";
        String statusCss = "Pleine".equals(s.getStatut()) ? "rs-full"
                         : "Disponible".equals(s.getStatut()) ? "rs-ok"
                         : "rs-warn";
        Label status = new Label(statusText.toUpperCase());
        status.getStyleClass().addAll("rstatus", statusCss);

        card.getChildren().addAll(hdr, pb, status);
        return card;
    }

    private void stopAutoRefresh() { if (autoRefresh != null) autoRefresh.stop(); }

    @FXML public void goDashboard()    { stopAutoRefresh(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList()   { stopAutoRefresh(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { stopAutoRefresh(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { stopAutoRefresh(); Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { stopAutoRefresh(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goVisualAssistance() { stopAutoRefresh(); Navigator.goTo("VisualAssistance"); }
    @FXML public void goSurveillance() { stopAutoRefresh(); Navigator.goTo("Surveillance"); }

    // ── Helpers ─────────────────────────────────────────────────
    private static String threatIcon(int niveau) {
        switch (niveau) {
            case 0: return "●";   // filled circle — vert nominal
            case 1: return "◑";   // half circle — jaune signal
            case 2: return "◕";   // mostly filled — orange alerte locale
            case 3: return "⬤";   // full bold — rouge alerte régionale
            case 4: return "⬤";   // pandémie — rouge foncé
            default: return "●";
        }
    }

    private static String saisonEmoji(String nom) {
        if (nom == null) return "🌡";
        switch (nom) {
            case "Hiver":     return "❄";
            case "Printemps": return "🌸";
            case "Ete":       return "☀";
            case "Automne":   return "🍂";
            default:          return "🌡";
        }
    }
}
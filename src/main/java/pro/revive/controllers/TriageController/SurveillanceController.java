package pro.revive.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.util.Duration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import pro.revive.Navigator;
import pro.revive.services.AIAnalysisService;
import pro.revive.services.EpidemiologicalDetector;
import pro.revive.services.WHOFeedService;
import pro.revive.utils.AppExecutor;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SurveillanceController implements Initializable {

    @FXML private Label lblUserName, lblPageSub;
    @FXML private Label lblThreatLabel, lblThreatDesc, lblThreatIcon, lblThreatUpdate;
    @FXML private HBox  threatBanner;
    @FXML private Label lblThreatSide, lblThreatSubSide;
    @FXML private Label lblNbContagieux, lblNbClusters, lblNbWHO, lblSaison;
    @FXML private VBox  whoFeedBox, syndromeStatsBox, exposureBox;
    @FXML private Label lblSeasonForecast;
    @FXML private VBox  aiAnalysisBox;
    @FXML private Label lblAIAnalysis;

    private final WHOFeedService          whoService  = new WHOFeedService();
    private final EpidemiologicalDetector detector    = new EpidemiologicalDetector();
    private final AIAnalysisService       aiService   = new AIAnalysisService();
    private final pro.revive.services.TriageService triageService = new pro.revive.services.TriageService();

    private List<WHOFeedService.WHOAlert> cachedAlerts;
    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        loadAll();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(60), e -> loadAll()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML public void refresh() { loadAll(); }

    private void loadAll() {
        lblPageSub.setText("Chargement en cours...");
        // Tout charger en arrière-plan pour ne pas bloquer l'UI
        AppExecutor.run(() -> {
            List<WHOFeedService.WHOAlert> alerts = whoService.fetchAlerts();
            cachedAlerts = alerts;
            EpidemiologicalDetector.ThreatLevel threat = detector.calculerNiveauMenace(alerts);
            // nbContagieux is already computed inside calculerNiveauMenace() and cached on the result —
            // reuse it here to avoid a second identical DB query.
            int nbContagieux = threat.nbContagieux;
            Map<String, List<pro.revive.entities.Triage>> clusters = detector.detecterClusters(
                    triageService.getData());
            List<EpidemiologicalDetector.ExposureAlert> exposures = detector.detecterPropagationInterne();
            Map<String, Integer> syndromeStats = detector.getSyndromeStats24h();
            EpidemiologicalDetector.SeasonInfo saison = detector.getSaisonActuelle();

            Platform.runLater(() -> {
                renderThreatBanner(threat);
                renderStats(nbContagieux, clusters.size(), alerts.size(), saison.nom);
                renderWHOFeed(alerts, clusters.isEmpty() ? null : clusters.keySet().iterator().next());
                renderSyndromeStats(syndromeStats);
                renderExposureAlerts(exposures);
                renderSeasonForecast(saison);
                lblPageSub.setText("Derniere mise a jour : " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            });
        });
    }

    // ── Bannière niveau de menace ────────────────────────────────
    private void renderThreatBanner(EpidemiologicalDetector.ThreatLevel threat) {
        lblThreatLabel.setText("Niveau " + threat.niveau + " — " + threat.label);
        lblThreatDesc.setText(threat.description);
        lblThreatUpdate.setText("Calcule a " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        // Icône par niveau
        String[] icons = {"●", "◑", "◕", "⬤", "⬤"};
        lblThreatIcon.setText(icons[Math.min(threat.niveau, 4)]);
        threatBanner.setStyle("-fx-background-color: " + threat.couleur +
                "; -fx-background-radius: 16px; -fx-padding: 20px 28px;");

        lblThreatSide.setText("Niveau " + threat.niveau + " — " + threat.label);
        lblThreatSubSide.setText(threat.clusterDetecte ?
                threat.nbPatientsCluster + " patients — " + threat.syndromeCluster :
                "Aucun signal local");
    }

    // ── Statistiques rapides ─────────────────────────────────────
    private void renderStats(int contagieux, int clusters, int who, String saison) {
        lblNbContagieux.setText(String.valueOf(contagieux));
        lblNbClusters.setText(String.valueOf(clusters));
        lblNbWHO.setText(String.valueOf(who));
        lblSaison.setText(saison);
    }

    // ── Flux OMS ────────────────────────────────────────────────
    private void renderWHOFeed(List<WHOFeedService.WHOAlert> alerts, String localSyndrome) {
        whoFeedBox.getChildren().clear();
        if (alerts.isEmpty()) {
            Label empty = new Label("Aucune alerte OMS disponible actuellement.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8; -fx-padding: 20px;");
            whoFeedBox.getChildren().add(empty);
            return;
        }

        for (WHOFeedService.WHOAlert alert : alerts) {
            VBox card = buildWHOCard(alert, localSyndrome);
            whoFeedBox.getChildren().add(card);

            // Séparateur
            Region sep = new Region();
            sep.setStyle("-fx-background-color: #F1F5F9; -fx-pref-height: 1;");
            whoFeedBox.getChildren().add(sep);
        }
    }

    private VBox buildWHOCard(WHOFeedService.WHOAlert alert, String localSyndrome) {
        boolean isMatch = localSyndrome != null && alert.matchesSyndrome(localSyndrome);
        String bg = isMatch ? "#FFF7ED" : "white";
        String border = isMatch ? "-fx-border-color: #F97316; -fx-border-width: 0 0 0 4px;" : "";

        VBox card = new VBox(6);
        card.setStyle("-fx-background-color: " + bg + "; -fx-padding: 14px 16px;" + border);

        // Header: icône syndrome + badge NOUVEAU + badge correspondance
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(alert.getSyndromeIcon());
        icon.setStyle("-fx-font-size: 18px; -fx-text-fill: " + alert.getSyndromeColor() + ";");

        Label synBadge = new Label(alert.syndromeType);
        synBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-background-color: " +
                alert.getSyndromeColor() + "22; -fx-text-fill: " + alert.getSyndromeColor() +
                "; -fx-padding: 2 8; -fx-background-radius: 10;");

        header.getChildren().addAll(icon, synBadge);

        if (alert.isRecent) {
            Label newBadge = new Label("NOUVEAU");
            newBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: #DCFCE7;" +
                    "-fx-text-fill: #16A34A; -fx-padding: 2 8; -fx-background-radius: 10;");
            header.getChildren().add(newBadge);
        }

        if (isMatch) {
            Label matchBadge = new Label("◉ Correspondance locale");
            matchBadge.setStyle("-fx-font-size: 9px; -fx-font-weight: bold; -fx-background-color: #FED7AA;" +
                    "-fx-text-fill: #C2410C; -fx-padding: 2 8; -fx-background-radius: 10;");
            header.getChildren().add(matchBadge);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().add(spacer);

        Label date = new Label(alert.getDateFormatee());
        date.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        header.getChildren().add(date);

        // Titre
        Label titre = new Label(alert.titre);
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        titre.setWrapText(true);

        // Région
        Label region = new Label("● " + alert.region);
        region.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

        // Description tronquée
        String desc = alert.description.length() > 180 ?
                alert.description.substring(0, 177) + "..." : alert.description;
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        descLabel.setWrapText(true);

        card.getChildren().addAll(header, titre, region, descLabel);
        return card;
    }

    // ── Syndromes 24h ────────────────────────────────────────────
    private void renderSyndromeStats(Map<String, Integer> stats) {
        syndromeStatsBox.getChildren().clear();

        // Only show categories that have at least 1 patient
        Map<String, Integer> active = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : stats.entrySet()) {
            if (e.getValue() > 0) active.put(e.getKey(), e.getValue());
        }

        if (active.isEmpty()) {
            Label empty = new Label("Aucun patient avec syndrome renseigne dans les 24h.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
            syndromeStatsBox.getChildren().add(empty);
            return;
        }

        int max = active.values().stream().mapToInt(v -> v).max().orElse(1);
        String[] colors = {"#3B82F6", "#F59E0B", "#8B5CF6", "#EC4899", "#EF4444", "#64748B", "#10B981"};
        int i = 0;
        for (Map.Entry<String, Integer> entry : active.entrySet()) {
            String color = colors[i % colors.length];
            int val = entry.getValue();
            double pct = (double) val / max;

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            Label name = new Label(entry.getKey());
            name.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B; -fx-min-width: 110;");

            Region bar = new Region();
            bar.setPrefHeight(8);
            bar.setPrefWidth(Math.max(4, pct * 140));
            bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");

            Label count = new Label(String.valueOf(val));
            count.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

            row.getChildren().addAll(name, bar, count);
            syndromeStatsBox.getChildren().add(row);
            i++;
        }
    }

    // ── Alertes de propagation interne ──────────────────────────
    private void renderExposureAlerts(List<EpidemiologicalDetector.ExposureAlert> alertes) {
        exposureBox.getChildren().clear();
        if (alertes.isEmpty()) {
            Label ok = new Label("✓ Aucun patient contagieux en zone partagee.");
            ok.setStyle("-fx-font-size: 12px; -fx-text-fill: #16A34A; -fx-font-weight: bold;");
            exposureBox.getChildren().add(ok);
            return;
        }

        for (EpidemiologicalDetector.ExposureAlert exp : alertes) {
            String color = exp.contagionFlag.equals("confirme") ? "#EF4444" : "#F59E0B";
            String bg    = exp.contagionFlag.equals("confirme") ? "#FEF2F2" : "#FEFCE8";

            VBox card = new VBox(4);
            card.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 8px;" +
                    "-fx-padding: 10px; -fx-border-color: " + color +
                    "; -fx-border-width: 0 0 0 3px;");
            VBox.setMargin(card, new Insets(0, 0, 4, 0));

            Label nom = new Label(exp.nomPatient);
            nom.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");

            Label info = new Label(exp.syndrome + " • En attente depuis " + exp.minutesEnAttente + " min");
            info.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");

            Label urgence = new Label("Urgence isolement : " + exp.urgenceScore + "%");
            urgence.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

            card.getChildren().addAll(nom, info, urgence);
            exposureBox.getChildren().add(card);
        }
    }

    // ── Prévision saisonnière ────────────────────────────────────
    private void renderSeasonForecast(EpidemiologicalDetector.SeasonInfo saison) {
        StringBuilder sb = new StringBuilder();
        sb.append("Saison : ").append(saison.nom).append("\n\n");
        sb.append("Maladies attendues :\n");
        for (String m : saison.maladiesAttendues) sb.append("• ").append(m).append("\n");
        lblSeasonForecast.setText(sb.toString().trim());
    }

    // ── Analyse IA complète ──────────────────────────────────────
    @FXML public void genererAnalyseIA() {
        lblAIAnalysis.setText("Analyse en cours...");
        aiAnalysisBox.setVisible(true);
        aiAnalysisBox.setManaged(true);

        List<WHOFeedService.WHOAlert> alerts = cachedAlerts != null ? cachedAlerts : List.of();
        EpidemiologicalDetector.SeasonInfo saison = detector.getSaisonActuelle();

        AppExecutor.run(() -> {
            AIAnalysisService.SeasonalForecast forecast =
                    aiService.genererPrevisionSaisonniere(saison.nom, saison.mois, alerts);
            Platform.runLater(() -> {
                String texte = forecast.analyse + "\n\n" +
                               "Maladies prevues : " + forecast.maladiesPrevues + "\n\n" +
                               "Recommandations :\n" + forecast.recommandations;
                lblAIAnalysis.setText(texte);
            });
        });
    }

    // ── Navigation ───────────────────────────────────────────────
    private void stopAutoRefresh() { if (autoRefresh != null) autoRefresh.stop(); }

    @FXML public void goDashboard()    { stopAutoRefresh(); Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList()   { stopAutoRefresh(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { stopAutoRefresh(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { stopAutoRefresh(); Navigator.goTo("Salle_List"); }
    @FXML public void goVisualAssistance() { stopAutoRefresh(); Navigator.goTo("VisualAssistance"); }
    @FXML public void goSurveillance() { stopAutoRefresh(); Navigator.goTo("Surveillance"); }
    @FXML public void deconnexion()    { stopAutoRefresh(); Navigator.goTo("DashboardTriage"); }
}

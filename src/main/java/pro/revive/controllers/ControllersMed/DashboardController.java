package pro.revive.controllers.ControllersMed;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import pro.revive.services.ServicesMed.StatistiquesService;
import pro.revive.services.ServicesMed.TriageDataService;
import pro.revive.entities.EntitiesMed.TriagePatient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ── KPI cards ─────────────────────────────────────────────────────────
    @FXML private Label lblTotal;
    @FXML private Label lblEnCours;
    @FXML private Label lblCloturees;
    @FXML private Label lblOrdos;
    @FXML private Label lblAujourdhui;
    @FXML private Label lblSemaine;
    @FXML private Label lblDureeMoy;

    // ── KPI Triage ────────────────────────────────────────────────────────
    @FXML private Label lblKpiCritique;
    @FXML private Label lblKpiUrgent;
    @FXML private Label lblKpiModere;
    @FXML private Label lblKpiStable;

    // ── Graphiques ────────────────────────────────────────────────────────
    @FXML private PieChart  pieOrientation;
    @FXML private BarChart<String, Number> barActivite;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis   barYAxis;

    // ── Top médecins ──────────────────────────────────────────────────────
    @FXML private VBox vboxTopMedecins;

    // ── Activité récente ──────────────────────────────────────────────────
    @FXML private VBox vboxRecent;

    // ── Sidebar stats ─────────────────────────────────────────────────────
    @FXML private Label lblSideTotal;
    @FXML private Label lblSideOrdos;

    // ── Triage ────────────────────────────────────────────────────────────
    @FXML private VBox  vboxTriage;
    @FXML private Label lblTriageCritique;
    @FXML private Label lblTriageUrgent;
    @FXML private Label lblTriageModere;
    @FXML private Label lblTriageStable;

    private final StatistiquesService stats = new StatistiquesService();
    private final TriageDataService triageService = new TriageDataService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        javafx.application.Platform.runLater(this::chargerDonnees);

        // Injecter l'assistant vocal quand la scène est disponible
        if (lblTotal != null) {
            lblTotal.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getRoot() instanceof javafx.scene.layout.StackPane sp) {
                    // Vérifier qu'il n'est pas déjà injecté
                    boolean dejaPresent = sp.getChildren().stream()
                        .anyMatch(n -> n.getUserData() != null && "assistant".equals(n.getUserData()));
                    if (!dejaPresent) {
                        GlobalAssistantController assistant = new GlobalAssistantController(sp);
                        assistant.setNavigationCallback(this::naviguer);
                    }
                }
            });
        }
    }

    // ── Chargement ────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            int total      = stats.getTotalConsultations();
            int enCours    = stats.getConsultationsEnCours();
            int cloturees  = stats.getConsultationsCloturees();
            int ordos      = stats.getTotalOrdonnances();
            int aujourdhui = stats.getConsultationsAujourdhui();
            int semaine    = stats.getConsultationsCetteSemaine();
            double duree   = stats.getDureeMoyenneMinutes();

            // KPI consultations
            if (lblTotal     != null) lblTotal.setText(String.valueOf(total));
            if (lblEnCours   != null) lblEnCours.setText(String.valueOf(enCours));
            if (lblCloturees != null) lblCloturees.setText(String.valueOf(cloturees));
            if (lblOrdos     != null) lblOrdos.setText(String.valueOf(ordos));
            if (lblAujourdhui!= null) lblAujourdhui.setText(String.valueOf(aujourdhui));
            if (lblSemaine   != null) lblSemaine.setText(String.valueOf(semaine));
            if (lblDureeMoy  != null) lblDureeMoy.setText(duree > 0 ? String.format("%.0f min", duree) : "—");

            // Sidebar
            if (lblSideTotal != null) lblSideTotal.setText(total + " consultations");
            if (lblSideOrdos != null) lblSideOrdos.setText(ordos + " ordonnances");

            // KPI Triage
            int critique = triageService.countByNiveau("CRITIQUE");
            int urgent   = triageService.countByNiveau("URGENT");
            int modere   = triageService.countByNiveau("MODERE");
            int stable   = triageService.countByNiveau("STABLE");
            if (lblKpiCritique != null) lblKpiCritique.setText(String.valueOf(critique));
            if (lblKpiUrgent   != null) lblKpiUrgent.setText(String.valueOf(urgent));
            if (lblKpiModere   != null) lblKpiModere.setText(String.valueOf(modere));
            if (lblKpiStable   != null) lblKpiStable.setText(String.valueOf(stable));

        } catch (Exception e) {
            System.out.println("[Dashboard] Erreur KPI: " + e.getMessage());
        }

        // Graphiques — chacun isolé pour ne pas bloquer les autres
        try { chargerPieOrientation();  } catch (Exception e) { System.out.println("[Dashboard] Pie: " + e.getMessage()); }
        try { chargerBarActivite();     } catch (Exception e) { System.out.println("[Dashboard] Bar: " + e.getMessage()); }
        try { chargerTopMedecins();     } catch (Exception e) { System.out.println("[Dashboard] Top: " + e.getMessage()); }
        try { chargerActiviteRecente(); } catch (Exception e) { System.out.println("[Dashboard] Recent: " + e.getMessage()); }
        try { chargerTriage();          } catch (Exception e) { System.out.println("[Dashboard] Triage: " + e.getMessage()); }
    }

    private void chargerPieOrientation() {
        if (pieOrientation == null) return;
        Map<String, Integer> data = stats.getRepartitionOrientation();
        pieOrientation.getData().clear();
        if (data.isEmpty()) {
            pieOrientation.getData().add(new PieChart.Data("Aucune donnée", 1));
        } else {
            data.forEach((label, count) ->
                pieOrientation.getData().add(new PieChart.Data(label + " (" + count + ")", count)));
        }
        pieOrientation.setLegendVisible(true);
        pieOrientation.setLabelsVisible(true);
        pieOrientation.setStartAngle(90);
    }

    private void chargerBarActivite() {
        if (barActivite == null) return;
        Map<String, Integer> data = stats.getActivite7Jours();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");
        if (data.isEmpty()) {
            series.getData().add(new XYChart.Data<>("Auj.", 0));
        } else {
            data.forEach((jour, nb) -> series.getData().add(new XYChart.Data<>(jour, nb)));
        }
        barActivite.getData().clear();
        barActivite.getData().add(series);
        barActivite.setLegendVisible(false);
        javafx.application.Platform.runLater(() -> {
            barActivite.lookupAll(".bar").forEach(node ->
                node.setStyle("-fx-bar-fill: #2ec4a0;"));
        });
    }

    private void chargerTopMedecins() {
        if (vboxTopMedecins == null) return;
        vboxTopMedecins.getChildren().clear();
        Map<String, Integer> top = stats.getTopMedecins();
        if (top.isEmpty()) {
            vboxTopMedecins.getChildren().add(
                styledLabel("Aucune donnée disponible.", "#8AAAD4", 11));
            return;
        }
        int max = top.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int rank = 1;
        for (Map.Entry<String, Integer> e : top.entrySet()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 6 0;");

            // Rang
            Label lblRank = new Label(rank + ".");
            lblRank.setStyle("-fx-text-fill: #8AAAD4; -fx-font-size: 12px; -fx-min-width: 20;");

            // Nom
            Label lblNom = new Label(e.getKey());
            lblNom.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: bold;");
            HBox.setHgrow(lblNom, Priority.ALWAYS);

            // Barre de progression
            double pct = (double) e.getValue() / max;
            StackPane barContainer = new StackPane();
            barContainer.setPrefWidth(120);
            barContainer.setMaxWidth(120);
            Region barBg = new Region();
            barBg.setPrefHeight(8);
            barBg.setStyle("-fx-background-color: #E8F0FB; -fx-background-radius: 4;");
            Region barFill = new Region();
            barFill.setPrefHeight(8);
            barFill.setPrefWidth(120 * pct);
            barFill.setMaxWidth(120 * pct);
            barFill.setStyle("-fx-background-color: #0B4EA2; -fx-background-radius: 4;");
            barContainer.getChildren().addAll(barBg, barFill);
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);

            // Compteur
            Label lblCount = new Label(e.getValue() + " cons.");
            lblCount.setStyle("-fx-text-fill: #0B4EA2; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 60;");

            row.getChildren().addAll(lblRank, lblNom, barContainer, lblCount);
            vboxTopMedecins.getChildren().add(row);

            // Séparateur léger
            if (rank < top.size()) {
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: #F0F4F8;");
                vboxTopMedecins.getChildren().add(sep);
            }
            rank++;
        }
    }

    private void chargerActiviteRecente() {
        if (vboxRecent == null) return;
        vboxRecent.getChildren().clear();
        List<String[]> recents = stats.getDernieresConsultations();
        if (recents.isEmpty()) {
            vboxRecent.getChildren().add(
                styledLabel("Aucune consultation récente.", "#8AAAD4", 11));
            return;
        }
        for (String[] row : recents) {
            // row: [id, patient, medecin, orientation, statut, debut]
            HBox card = new HBox(12);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setStyle("-fx-background-color: #F8FAFF; -fx-background-radius: 8; "
                        + "-fx-border-color: #E8F0FB; -fx-border-radius: 8; "
                        + "-fx-border-width: 1; -fx-padding: 10 14;");

            // ID badge
            Label lblId = new Label(row[0]);
            lblId.setStyle("-fx-background-color: #EBF2FF; -fx-text-fill: #0B4EA2; "
                         + "-fx-background-radius: 6; -fx-padding: 3 8; "
                         + "-fx-font-size: 11px; -fx-font-weight: bold;");

            // Patient + médecin
            VBox info = new VBox(2);
            Label lblPat = new Label(row[1] != null ? row[1] : "—");
            lblPat.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 12px; -fx-font-weight: bold;");
            Label lblMed = new Label("Dr. " + (row[2] != null ? row[2] : "—"));
            lblMed.setStyle("-fx-text-fill: #8AAAD4; -fx-font-size: 10px;");
            info.getChildren().addAll(lblPat, lblMed);
            HBox.setHgrow(info, Priority.ALWAYS);

            // Date
            Label lblDate = new Label(row[5] != null ? row[5] : "—");
            lblDate.setStyle("-fx-text-fill: #8AAAD4; -fx-font-size: 10px;");

            // Statut badge
            boolean enCours = "En cours".equals(row[4]);
            Label lblStatut = new Label(row[4]);
            lblStatut.setStyle(enCours
                ? "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706; "
                + "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold;"
                : "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A; "
                + "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold;");

            card.getChildren().addAll(lblId, info, lblDate, lblStatut);
            vboxRecent.getChildren().add(card);
        }
    }

    // ── Triage ────────────────────────────────────────────────────────────

    private void chargerTriage() {
        if (vboxTriage == null) return;
        vboxTriage.getChildren().clear();

        List<TriagePatient> triages = triageService.getAllTriages();

        // Trier par niveau de gravité : CRITIQUE → URGENT → MODÉRÉ → STABLE
        triages.sort((a, b) -> {
            int pa = niveauPriorite(getNiveau(a));
            int pb = niveauPriorite(getNiveau(b));
            if (pa != pb) return Integer.compare(pa, pb);
            // À même niveau, les plus hauts scores en premier
            return Integer.compare(b.getScoreCalcule(), a.getScoreCalcule());
        });

        long critique = triages.stream().filter(t -> "CRITIQUE".equalsIgnoreCase(getNiveau(t))).count();
        long urgent   = triages.stream().filter(t -> "URGENT".equalsIgnoreCase(getNiveau(t))).count();
        long modere   = triages.stream().filter(t -> "MODERE".equalsIgnoreCase(getNiveau(t))).count();
        long stable   = triages.stream().filter(t -> "STABLE".equalsIgnoreCase(getNiveau(t))).count();

        if (lblTriageCritique != null) lblTriageCritique.setText("● CRITIQUE : " + critique);
        if (lblTriageUrgent   != null) lblTriageUrgent.setText("● URGENT : " + urgent);
        if (lblTriageModere   != null) lblTriageModere.setText("● MODÉRÉ : " + modere);
        if (lblTriageStable   != null) lblTriageStable.setText("● STABLE : " + stable);

        if (triages.isEmpty()) {
            Label empty = new Label("Aucun patient en triage.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-padding: 20px;");
            vboxTriage.getChildren().add(empty);
            return;
        }

        String currentNiveau = "";
        for (TriagePatient t : triages) {
            String niveau = getNiveau(t);
            // Ajouter un séparateur de section quand le niveau change
            if (!niveau.equalsIgnoreCase(currentNiveau)) {
                currentNiveau = niveau;
                vboxTriage.getChildren().add(buildNiveauSeparateur(niveau, t.getNiveauColor(), t.getNiveauBgColor()));
            }
            HBox card = buildTriageCard(t);
            card.setOnMouseClicked(e -> ouvrirDossierPatient(t));
            card.setStyle(card.getStyle() + "-fx-cursor: hand;");
            vboxTriage.getChildren().add(card);
        }
    }

    private int niveauPriorite(String niveau) {
        if (niveau == null) return 4;
        return switch (niveau.toUpperCase()) {
            case "CRITIQUE" -> 0;
            case "URGENT"   -> 1;
            case "MODERE"   -> 2;
            case "STABLE"   -> 3;
            default         -> 4;
        };
    }

    private HBox buildNiveauSeparateur(String niveau, String color, String bgColor) {
        HBox sep = new HBox(10);
        sep.setAlignment(Pos.CENTER_LEFT);
        sep.setStyle("-fx-padding: 10px 4px 4px 4px;");

        Region line1 = new Region();
        line1.setPrefHeight(2);
        line1.setPrefWidth(16);
        line1.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        String emoji = switch (niveau.toUpperCase()) {
            case "CRITIQUE" -> "●";
            case "URGENT"   -> "●";
            case "MODERE"   -> "●";
            case "STABLE"   -> "●";
            default         -> "●";
        };

        Label lbl = new Label(emoji + "  " + niveau);
        lbl.setStyle(
            "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";" +
            "-fx-background-color: " + bgColor + "; -fx-background-radius: 20px;" +
            "-fx-padding: 3px 12px; -fx-border-color: " + color + "55;" +
            "-fx-border-radius: 20px; -fx-border-width: 1px;"
        );

        Region line2 = new Region();
        line2.setPrefHeight(1);
        HBox.setHgrow(line2, Priority.ALWAYS);
        line2.setStyle("-fx-background-color: " + color + "33; -fx-background-radius: 1;");

        sep.getChildren().addAll(line1, lbl, line2);
        return sep;
    }

    // ── Dossier Patient Overlay ───────────────────────────────────────────

    private StackPane overlayPane; // overlay injecté dynamiquement

    private StackPane getOrCreateOverlay() {
        if (overlayPane != null) return overlayPane;
        // Chercher le StackPane racine de la scène
        javafx.scene.Node ref = lblTotal != null ? lblTotal : vboxTriage;
        if (ref == null || ref.getScene() == null) return null;
        javafx.scene.Node root = ref.getScene().getRoot();
        if (root instanceof StackPane sp) {
            overlayPane = sp;
        } else {
            // Envelopper le root dans un StackPane
            overlayPane = new StackPane();
        }
        return overlayPane;
    }

    private void ouvrirDossierPatient(TriagePatient t) {
        javafx.scene.Node ref = lblTotal != null ? lblTotal : vboxTriage;
        if (ref == null || ref.getScene() == null) return;

        // Fond semi-transparent
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.50);");
        overlay.setAlignment(Pos.CENTER);

        // Panneau dossier
        ScrollPane scroll = new ScrollPane(buildDossierContent(t));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle(
            "-fx-background-color: white; -fx-background-radius: 20px;" +
            "-fx-border-radius: 20px; -fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.40), 48, 0, 0, 12);"
        );
        scroll.setMaxWidth(860);
        scroll.setMaxHeight(620);
        scroll.setPrefWidth(860);

        overlay.getChildren().add(scroll);

        // Fermer au clic sur le fond
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) fermerOverlayDossier(overlay);
        });

        // Ajouter l'overlay par-dessus la scène
        javafx.scene.layout.Pane sceneRoot = (javafx.scene.layout.Pane) ref.getScene().getRoot();
        sceneRoot.getChildren().add(overlay);
        overlay.prefWidthProperty().bind(sceneRoot.widthProperty());
        overlay.prefHeightProperty().bind(sceneRoot.heightProperty());

        // Animation fade-in
        overlay.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(200), overlay);
        ft.setToValue(1);
        ft.play();
    }

    private void fermerOverlayDossier(StackPane overlay) {
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(Duration.millis(160), overlay);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
            if (root != null) root.getChildren().remove(overlay);
        });
        ft.play();
    }

    private VBox buildDossierContent(TriagePatient t) {
        String niveau  = getNiveau(t);
        String color   = t.getNiveauColor();
        String bgColor = t.getNiveauBgColor();

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: white; -fx-background-radius: 20px;");

        // ── Header ────────────────────────────────────────────────────────
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle(
            "-fx-background-color: linear-gradient(to right, #0B4EA2, #0E9B8A);" +
            "-fx-background-radius: 20px 20px 0 0; -fx-padding: 20px 28px;"
        );

        // Avatar
        String initiale = t.getNomComplet().trim().isEmpty() ? "?" :
            String.valueOf(t.getNomComplet().trim().charAt(0)).toUpperCase();
        Label avatar = new Label(initiale);
        avatar.setStyle(
            "-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white;" +
            "-fx-font-size: 22px; -fx-font-weight: bold;" +
            "-fx-background-radius: 50%; -fx-padding: 14px 18px;" +
            "-fx-border-color: rgba(255,255,255,0.40); -fx-border-radius: 50%; -fx-border-width: 2px;"
        );

        VBox titleBox = new VBox(4);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        Label lblNom = new Label(t.getNomComplet().trim().isEmpty() ? "Patient #" + t.getIdAdmission() : t.getNomComplet());
        lblNom.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label lblSub = new Label("Admission #" + t.getIdAdmission() + "  •  Triage #" + t.getIdTriage());
        lblSub.setStyle("-fx-text-fill: rgba(255,255,255,0.70); -fx-font-size: 12px;");
        titleBox.getChildren().addAll(lblNom, lblSub);

        // Badge niveau
        Label badgeNiveau = new Label(niveau);
        badgeNiveau.setStyle(
            "-fx-background-color: " + bgColor + "; -fx-text-fill: " + color + ";" +
            "-fx-font-weight: bold; -fx-font-size: 13px;" +
            "-fx-background-radius: 20px; -fx-padding: 8px 20px;" +
            "-fx-border-color: " + color + "; -fx-border-radius: 20px; -fx-border-width: 2px;"
        );

        // Bouton fermer
        Button btnClose = new Button("✕");
        btnClose.setStyle(
            "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;" +
            "-fx-font-size: 16px; -fx-background-radius: 50%; -fx-padding: 6px 10px;" +
            "-fx-cursor: hand; -fx-border-width: 0;"
        );

        header.getChildren().addAll(avatar, titleBox, badgeNiveau, btnClose);

        // ── Corps ─────────────────────────────────────────────────────────
        HBox body = new HBox(20);
        body.setStyle("-fx-padding: 24px 28px;");

        // Colonne gauche — constantes + score
        VBox left = new VBox(16);
        HBox.setHgrow(left, Priority.ALWAYS);

        // Titre constantes
        Label titreVitaux = new Label("Constantes Vitales");
        titreVitaux.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0B4EA2;");

        // Grid 2x4 de cards vitales
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        grid.add(vitalCard("❤️", "Pouls",          String.format("%.0f", t.getPoids()),    "bpm",   "#DC2626", "#FEF2F2", evalPouls(t.getPoids())),       0, 0);
        grid.add(vitalCard("↑",  "Tension Sys.",    String.format("%.1f", t.getTaSys()),    "mmHg",  "#7C3AED", "#FAF5FF", evalTaSys(t.getTaSys())),        1, 0);
        grid.add(vitalCard("🌡️", "Température",     String.format("%.1f", t.getTemperature()), "°C", "#EA580C", "#FFF7ED", evalTemp(t.getTemperature())),  2, 0);
        grid.add(vitalCard("O₂", "SpO2",            String.format("%.0f", t.getSpo2()),     "%",     "#0891B2", "#F0F9FF", evalSpo2(t.getSpo2())),          3, 0);
        grid.add(vitalCard("↓",  "Tension Dia.",    String.format("%.1f", t.getTaDia()),    "mmHg",  "#7C3AED", "#FAF5FF", evalTaDia(t.getTaDia())),        0, 1);
        grid.add(vitalCard("◆",  "Glycémie",        String.format("%.1f", t.getGlycemie()), "g/L",  "#D97706", "#FFFBEB", evalGlycemie(t.getGlycemie())), 1, 1);
        grid.add(vitalCard("★",  "GCS Score",       String.valueOf(t.getGcsScore()),        "/15",   "#059669", "#F0FDF4", evalGcs(t.getGcsScore())),       2, 1);
        grid.add(vitalCard("≈",  "Fréq. Resp.",     String.valueOf(t.getFrequenceRespiratoire()), "/min", "#0B4EA2", "#EFF6FF", evalFreqResp(t.getFrequenceRespiratoire())), 3, 1);

        // Score et analyse
        HBox scoreSection = new HBox(16);
        scoreSection.setAlignment(Pos.CENTER_LEFT);
        scoreSection.setStyle(
            "-fx-background-color: " + bgColor + "; -fx-background-radius: 12px;" +
            "-fx-border-color: " + color + "; -fx-border-radius: 12px; -fx-border-width: 1.5px;" +
            "-fx-padding: 16px 20px;"
        );

        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(Pos.CENTER);
        Label scoreVal = new Label(String.valueOf(t.getScoreCalcule()));
        scoreVal.setStyle("-fx-font-size: 42px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label scoreLbl = new Label("points");
        scoreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        scoreBox.getChildren().addAll(scoreVal, scoreLbl);

        VBox analyseBox = new VBox(6);
        HBox.setHgrow(analyseBox, Priority.ALWAYS);
        Label niveauLbl = new Label(niveau);
        niveauLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        String analyseText = t.getAnalyseAuto() != null && !t.getAnalyseAuto().isEmpty()
            ? t.getAnalyseAuto()
            : genererAnalyse(t);
        Label analyseTxt = new Label(analyseText);
        analyseTxt.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        analyseTxt.setWrapText(true);
        analyseBox.getChildren().addAll(niveauLbl, analyseTxt);

        scoreSection.getChildren().addAll(scoreBox, analyseBox);

        // Symptômes
        VBox sympBox = new VBox(6);
        Label sympTitre = new Label("Symptômes");
        sympTitre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #64748B;");
        Label sympTxt = new Label(t.getSymptomes() != null ? t.getSymptomes() : "Non renseigné");
        sympTxt.setStyle(
            "-fx-text-fill: #1E293B; -fx-font-size: 13px;" +
            "-fx-background-color: #F8FAFF; -fx-background-radius: 8px;" +
            "-fx-border-color: #E8F0FB; -fx-border-radius: 8px; -fx-border-width: 1px;" +
            "-fx-padding: 10px 14px;"
        );
        sympTxt.setWrapText(true);
        sympBox.getChildren().addAll(sympTitre, sympTxt);

        left.getChildren().addAll(titreVitaux, grid, scoreSection, sympBox);

        // Colonne droite — parcours patient
        VBox right = new VBox(12);
        right.setStyle("-fx-min-width: 220px; -fx-max-width: 220px;");

        Label titreParcours = new Label("Parcours Patient");
        titreParcours.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0B4EA2;");

        VBox parcours = new VBox(0);
        parcours.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12px;" +
            "-fx-border-color: #E8EDF5; -fx-border-radius: 12px; -fx-border-width: 1px;" +
            "-fx-padding: 8px;"
        );
        parcours.getChildren().addAll(
            parcoursItem("✓", "Admission",       "Enregistré", true),
            parcoursItem("✓", "Triage effectué", "Niveau " + niveau, true),
            parcoursItem("✓", "Salle assignée",  "Salle Urgence", true),
            parcoursItem("○", "Sortie / Fin",    "En cours de prise en charge", false)
        );

        // Infos complémentaires
        Label titreInfos = new Label("Informations");
        titreInfos.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0B4EA2; -fx-padding: 8 0 0 0;");

        VBox infos = new VBox(8);
        infos.setStyle(
            "-fx-background-color: #F8FAFF; -fx-background-radius: 12px;" +
            "-fx-border-color: #E8F0FB; -fx-border-radius: 12px; -fx-border-width: 1px;" +
            "-fx-padding: 14px;"
        );
        if (t.getDateHeureTriage() != null) {
            infos.getChildren().add(infoRow("🕐 Date triage", t.getDateHeureTriage()));
        }
        infos.getChildren().add(infoRow("🆔 ID Triage",    "#" + t.getIdTriage()));
        infos.getChildren().add(infoRow("🏥 Admission",    "#" + t.getIdAdmission()));
        infos.getChildren().add(infoRow("📊 Score douleur", String.format("%.0f/10", t.getScoreDouleur())));

        right.getChildren().addAll(titreParcours, parcours, titreInfos, infos);

        body.getChildren().addAll(left, right);
        root.getChildren().addAll(header, body);

        // ── Footer — Bouton Lancer Consultation ───────────────────────────
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setStyle(
            "-fx-background-color: #F8FAFF; -fx-background-radius: 0 0 20px 20px;" +
            "-fx-border-color: #E8F0FB transparent transparent transparent;" +
            "-fx-border-width: 1px 0 0 0; -fx-padding: 16px 28px;"
        );

        Label infoLbl = new Label("Admission #" + t.getIdAdmission() + "  •  " + t.getNomComplet().trim());
        infoLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLancer = new Button("🩺  Lancer la Consultation");
        btnLancer.setStyle(
            "-fx-background-color: linear-gradient(to right, #0B4EA2, #0E9B8A);" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
            "-fx-background-radius: 10px; -fx-padding: 12px 28px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(11,78,162,0.35), 10, 0, 0, 3);"
        );
        btnLancer.setOnMouseEntered(e -> btnLancer.setStyle(
            "-fx-background-color: linear-gradient(to right, #093D82, #0B7A6C);" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
            "-fx-background-radius: 10px; -fx-padding: 12px 28px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(11,78,162,0.50), 14, 0, 0, 4);" +
            "-fx-translate-y: -1;"
        ));
        btnLancer.setOnMouseExited(e -> btnLancer.setStyle(
            "-fx-background-color: linear-gradient(to right, #0B4EA2, #0E9B8A);" +
            "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;" +
            "-fx-background-radius: 10px; -fx-padding: 12px 28px; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(11,78,162,0.35), 10, 0, 0, 3);"
        ));

        footer.getChildren().addAll(infoLbl, spacer, btnLancer);
        root.getChildren().add(footer);

        // Action bouton fermer
        btnClose.setOnAction(e -> {
            javafx.scene.Node node = root;
            while (node.getParent() != null) {
                if (node.getParent() instanceof StackPane sp
                        && sp.getStyle() != null
                        && sp.getStyle().contains("rgba(0,0,0")) {
                    fermerOverlayDossier(sp);
                    return;
                }
                node = node.getParent();
            }
        });

        // Action bouton lancer consultation
        btnLancer.setOnAction(e -> {
            // Fermer l'overlay dossier
            javafx.scene.Node node = root;
            StackPane overlayRef = null;
            while (node.getParent() != null) {
                if (node.getParent() instanceof StackPane sp
                        && sp.getStyle() != null
                        && sp.getStyle().contains("rgba(0,0,0")) {
                    overlayRef = sp;
                    break;
                }
                node = node.getParent();
            }
            final StackPane overlayToClose = overlayRef;

            // Naviguer vers ConsultationList avec l'admission pré-sélectionnée
            lancerConsultationDepuisTriage(t, overlayToClose);
        });

        return root;
    }

    // ── Cards vitales ─────────────────────────────────────────────────────

    private VBox vitalCard(String icon, String label, String value, String unit,
                           String color, String bg, String etat) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(160);
        card.setStyle(
            "-fx-background-color: " + bg + "; -fx-background-radius: 12px;" +
            "-fx-border-color: " + color + "33; -fx-border-radius: 12px; -fx-border-width: 1px;" +
            "-fx-padding: 14px 10px;"
        );

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 18px; -fx-text-fill: " + color + ";");

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        HBox valBox = new HBox(3);
        valBox.setAlignment(Pos.CENTER);
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label unitLbl = new Label(unit);
        unitLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        valBox.getChildren().addAll(val, unitLbl);

        Label etatLbl = new Label(etat);
        etatLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(ico, lbl, valBox, etatLbl);
        return card;
    }

    // ── Évaluations cliniques ─────────────────────────────────────────────

    private String evalPouls(double v)     { return v < 60 ? "BRADYCARDIE" : v > 100 ? "TACHYCARDIE" : "NORMAL"; }
    private String evalTaSys(double v)     { return v < 90 ? "HYPOTENSION" : v > 140 ? "HYPERTENSION" : "NORMAL"; }
    private String evalTaDia(double v)     { return v < 60 ? "BASSE" : v > 90 ? "ÉLEVÉE" : "NORMAL"; }
    private String evalTemp(double v)      { return v < 36 ? "HYPOTHERMIE" : v > 38 ? "FIÈVRE" : "NORMAL"; }
    private String evalSpo2(double v)      { return v < 90 ? "CRITIQUE" : v < 95 ? "FAIBLE" : "NORMAL"; }
    private String evalGlycemie(double v)  { return v < 0.7 ? "HYPOGLYCÉMIE" : v > 1.1 ? "ÉLEVÉE" : "NORMAL"; }
    private String evalGcs(int v)          { return v <= 8 ? "CRITIQUE" : v <= 12 ? "RÉDUIT" : "NORMAL"; }
    private String evalFreqResp(int v)     { return v < 12 ? "BRADYPNÉE" : v > 20 ? "TACHYPNÉE" : "NORMAL"; }

    private String genererAnalyse(TriagePatient t) {
        StringBuilder sb = new StringBuilder();
        if (!evalPouls(t.getPoids()).equals("NORMAL"))       sb.append("Pouls ").append(evalPouls(t.getPoids()).toLowerCase()).append(". ");
        if (!evalTemp(t.getTemperature()).equals("NORMAL"))  sb.append("Température ").append(evalTemp(t.getTemperature()).toLowerCase()).append(". ");
        if (!evalSpo2(t.getSpo2()).equals("NORMAL"))         sb.append("SpO2 ").append(evalSpo2(t.getSpo2()).toLowerCase()).append(". ");
        if (!evalGcs(t.getGcsScore()).equals("NORMAL"))      sb.append("GCS ").append(evalGcs(t.getGcsScore()).toLowerCase()).append(". ");
        if (sb.length() == 0) sb.append("Constantes vitales dans les limites normales.");
        return sb.toString();
    }

    // ── Parcours patient ──────────────────────────────────────────────────

    private HBox parcoursItem(String check, String titre, String detail, boolean done) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 10px 12px;");

        Label ico = new Label(check);
        ico.setStyle(done
            ? "-fx-text-fill: #16A34A; -fx-font-size: 14px; -fx-font-weight: bold;"
            : "-fx-text-fill: #CBD5E1; -fx-font-size: 14px;");

        VBox txt = new VBox(2);
        Label t1 = new Label(titre);
        t1.setStyle(done
            ? "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;"
            : "-fx-font-size: 12px; -fx-text-fill: #94A3B8;");
        Label t2 = new Label(detail);
        t2.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        txt.getChildren().addAll(t1, t2);

        row.getChildren().addAll(ico, txt);

        // Séparateur
        VBox wrapper = new VBox(0);
        wrapper.getChildren().add(row);
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #F1F5F9;");
        wrapper.getChildren().add(sep);

        return row;
    }

    private VBox infoRow(String label, String value) {
        VBox box = new VBox(2);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
        box.getChildren().addAll(lbl, val);
        return box;
    }

    // ── Lancer consultation depuis triage ─────────────────────────────────

    private void lancerConsultationDepuisTriage(TriagePatient t, StackPane overlayToClose) {
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/ConsultationList.fxml");
            if (fxmlUrl == null) return;

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent newRoot = loader.load();
            ConsultationListController ctrl = loader.getController();

            // Récupérer la scène
            javafx.scene.Node ref = lblTotal != null ? lblTotal : vboxTriage;
            if (ref == null || ref.getScene() == null) return;
            Stage stage = (Stage) ref.getScene().getWindow();

            double screenW = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double screenH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            Scene scene = new Scene(newRoot, screenW, screenH);
            URL cssUrl = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            if (!stage.isMaximized()) stage.setMaximized(true);

            // Ouvrir directement le formulaire de nouvelle consultation
            // avec l'admission pré-sélectionnée — après que la scène soit affichée
            javafx.application.Platform.runLater(() ->
                ctrl.ouvrirNouvelleConsultationPourAdmission(t.getIdAdmission())
            );

        } catch (IOException e) {
            System.out.println("[Dashboard] Lancer consultation: " + e.getMessage());
        }
    }

    private String getNiveau(TriagePatient t) {
        return t.getNiveauFinal() != null ? t.getNiveauFinal() :
               t.getPatientState() != null ? t.getPatientState() : "STABLE";
    }

    private HBox buildTriageCard(TriagePatient t) {
        String niveau   = getNiveau(t);
        String color    = t.getNiveauColor();
        String bgColor  = t.getNiveauBgColor();

        HBox card = new HBox(16);
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 12px;" +
            "-fx-border-color: #E8EDF5; -fx-border-radius: 12px; -fx-border-width: 1px;" +
            "-fx-padding: 14px 18px;" +
            "-fx-effect: dropshadow(gaussian, rgba(11,78,162,0.06), 6, 0, 0, 2);"
        );

        // Bande colorée gauche
        Region stripe = new Region();
        stripe.setPrefWidth(4);
        stripe.setMinWidth(4);
        stripe.setMaxWidth(4);
        stripe.setPrefHeight(Double.MAX_VALUE);
        stripe.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4px;");

        // Avatar patient
        Label avatar = new Label(t.getNomComplet().trim().isEmpty() ? "?" :
            String.valueOf(t.getNomComplet().trim().charAt(0)).toUpperCase());
        avatar.setStyle(
            "-fx-background-color: " + bgColor + "; -fx-text-fill: " + color + ";" +
            "-fx-font-size: 16px; -fx-font-weight: bold;" +
            "-fx-background-radius: 50%; -fx-padding: 10px 14px;" +
            "-fx-border-color: " + color + "; -fx-border-radius: 50%; -fx-border-width: 1.5px;"
        );

        // Infos patient
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label lblNom = new Label(t.getNomComplet().trim().isEmpty() ? "Patient #" + t.getIdAdmission() : t.getNomComplet());
        lblNom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        HBox meta = new HBox(12);
        meta.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        if (t.getSymptomes() != null && !t.getSymptomes().isEmpty()) {
            String sym = t.getSymptomes().length() > 40 ?
                t.getSymptomes().substring(0, 40) + "..." : t.getSymptomes();
            Label lblSym = new Label("🩺 " + sym);
            lblSym.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
            meta.getChildren().add(lblSym);
        }
        if (t.getDateHeureTriage() != null) {
            Label lblDate = new Label("🕐 " + t.getDateHeureTriage());
            lblDate.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");
            meta.getChildren().add(lblDate);
        }
        info.getChildren().addAll(lblNom, meta);

        // Constantes vitales compactes
        VBox vitaux = new VBox(4);
        vitaux.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        vitaux.setStyle("-fx-min-width: 200px;");

        HBox row1 = new HBox(12);
        row1.getChildren().addAll(
            vitalLabel("❤️", String.format("%.0f bpm", t.getPoids()), "#DC2626"),
            vitalLabel("🌡️", String.format("%.1f°C", t.getTemperature()), "#EA580C"),
            vitalLabel("O₂", String.format("%.0f%%", t.getSpo2()), "#0891B2")
        );
        HBox row2 = new HBox(12);
        row2.getChildren().addAll(
            vitalLabel("TA", String.format("%.0f/%.0f", t.getTaSys(), t.getTaDia()), "#7C3AED"),
            vitalLabel("GCS", String.valueOf(t.getGcsScore()), "#059669")
        );
        vitaux.getChildren().addAll(row1, row2);

        // Badge niveau
        Label badge = new Label(niveau);
        badge.setStyle(
            "-fx-background-color: " + bgColor + "; -fx-text-fill: " + color + ";" +
            "-fx-font-weight: bold; -fx-font-size: 11px;" +
            "-fx-background-radius: 20px; -fx-padding: 5px 14px;" +
            "-fx-border-color: " + color + "; -fx-border-radius: 20px; -fx-border-width: 1.5px;"
        );

        // Score
        VBox scoreBox = new VBox(2);
        scoreBox.setAlignment(javafx.geometry.Pos.CENTER);
        Label scoreVal = new Label(String.valueOf(t.getScoreCalcule()));
        scoreVal.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label scoreLbl = new Label("score");
        scoreLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94A3B8;");
        scoreBox.getChildren().addAll(scoreVal, scoreLbl);

        card.getChildren().addAll(stripe, avatar, info, vitaux, badge, scoreBox);
        return card;
    }

    private HBox vitalLabel(String icon, String value, String color) {
        HBox box = new HBox(4);
        box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size: 11px;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        box.getChildren().addAll(ico, val);
        return box;
    }

    @FXML private void handleNavConsultations() { naviguer("ConsultationList.fxml"); }
    @FXML private void handleNavAjouter()        { naviguer("ConsultationList.fxml"); }
    @FXML private void handleNavListe()          { naviguer("ConsultationList.fxml"); }
    @FXML private void handleNavStats()          { naviguer("Statistiques.fxml"); }
    @FXML private void handleRefresh()           { chargerDonnees(); }
    @FXML private void handleDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            java.net.URL fxmlUrl = getClass().getResource("/ResourcesUser/images/fxml/Login.fxml");
            if (fxmlUrl == null) return;
            Parent root = FXMLLoader.load(fxmlUrl);
            javafx.scene.Node ref = lblTotal != null ? lblTotal : lblOrdos;
            if (ref == null || ref.getScene() == null) return;
            Stage stage = (Stage) ref.getScene().getWindow();
            java.net.URL cssUrl = getClass().getResource("/ResourcesUser/images/css/user.css");
            Scene scene = new Scene(root);
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("REVIVE — Connexion");
        } catch (IOException e) {
            System.out.println("[Dashboard] Deconnexion error: " + e.getMessage());
        }
    }

    // ── Export PDF rapport ────────────────────────────────────────────────

    @FXML private void handleExportRapport() {
        // Trouver une fenêtre valide
        javafx.scene.Node ref = lblTotal != null ? lblTotal : lblOrdos;
        if (ref == null || ref.getScene() == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("Rapport_Dashboard_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File fichier = fc.showSaveDialog(ref.getScene().getWindow());
        if (fichier == null) return;

        try {
            genererRapportPdf(fichier);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export réussi");
            ok.setHeaderText(null);
            ok.setContentText("Rapport exporté :\n" + fichier.getAbsolutePath());
            styleAlert(ok);
            ok.showAndWait();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur PDF");
            err.setHeaderText(null);
            err.setContentText("Impossible de générer le rapport :\n" + e.getMessage());
            styleAlert(err);
            err.showAndWait();
        }
    }

    private void genererRapportPdf(File fichier) throws Exception {
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, new FileOutputStream(fichier));
        doc.open();

        Font fontTitre    = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   new BaseColor(26, 107, 90));
        Font fontSousTitre= new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(100, 100, 100));
        Font fontSection  = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   new BaseColor(26, 79, 122));
        Font fontNormal   = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
        Font fontBold     = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.BLACK);
        Font fontSmall    = new Font(Font.FontFamily.HELVETICA,  9, Font.ITALIC, new BaseColor(120, 120, 120));
        Font fontKpi      = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   new BaseColor(11, 78, 162));

        LineSeparator line = new LineSeparator(1f, 100f, new BaseColor(46, 196, 160), Element.ALIGN_CENTER, -2);

        // ── En-tête ──────────────────────────────────────────────────────
        Paragraph header = new Paragraph("REVIVE — Tableau de Bord", fontTitre);
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);

        Paragraph subHeader = new Paragraph(
            "Rapport généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
            fontSousTitre);
        subHeader.setAlignment(Element.ALIGN_CENTER);
        subHeader.setSpacingAfter(6);
        doc.add(subHeader);

        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        // ── KPIs ─────────────────────────────────────────────────────────
        Paragraph kpiTitle = new Paragraph("Indicateurs Clés", fontSection);
        kpiTitle.setSpacingAfter(8);
        doc.add(kpiTitle);

        PdfPTable kpiTable = new PdfPTable(3);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(16);

        addKpiCell(kpiTable, "Total Consultations", lblTotal.getText(), fontBold, fontKpi);
        addKpiCell(kpiTable, "En cours",            lblEnCours.getText(), fontBold, fontKpi);
        addKpiCell(kpiTable, "Clôturées",           lblCloturees.getText(), fontBold, fontKpi);
        addKpiCell(kpiTable, "Ordonnances",         lblOrdos.getText(), fontBold, fontKpi);
        addKpiCell(kpiTable, "Aujourd'hui",         lblAujourdhui.getText(), fontBold, fontKpi);
        addKpiCell(kpiTable, "Cette semaine",       lblSemaine.getText(), fontBold, fontKpi);
        doc.add(kpiTable);

        // ── Durée moyenne ─────────────────────────────────────────────────
        Paragraph duree = new Paragraph("Durée moyenne des consultations : " + lblDureeMoy.getText(), fontNormal);
        duree.setSpacingAfter(12);
        doc.add(duree);

        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        // ── Répartition par orientation ───────────────────────────────────
        Paragraph orientTitle = new Paragraph("Répartition par Orientation", fontSection);
        orientTitle.setSpacingAfter(8);
        doc.add(orientTitle);

        Map<String, Integer> orientData = stats.getRepartitionOrientation();
        PdfPTable orientTable = new PdfPTable(2);
        orientTable.setWidthPercentage(60);
        orientTable.setSpacingAfter(16);
        orientTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell h1 = new PdfPCell(new Phrase("Orientation", fontBold));
        PdfPCell h2 = new PdfPCell(new Phrase("Nombre", fontBold));
        h1.setBackgroundColor(new BaseColor(235, 242, 255));
        h2.setBackgroundColor(new BaseColor(235, 242, 255));
        h1.setPadding(6); h2.setPadding(6);
        orientTable.addCell(h1); orientTable.addCell(h2);

        for (Map.Entry<String, Integer> e : orientData.entrySet()) {
            PdfPCell c1 = new PdfPCell(new Phrase(e.getKey(), fontNormal));
            PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(e.getValue()), fontNormal));
            c1.setPadding(5); c2.setPadding(5);
            orientTable.addCell(c1); orientTable.addCell(c2);
        }
        doc.add(orientTable);

        // ── Top médecins ──────────────────────────────────────────────────
        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        Paragraph topTitle = new Paragraph("Top Médecins", fontSection);
        topTitle.setSpacingAfter(8);
        doc.add(topTitle);

        Map<String, Integer> topData = stats.getTopMedecins();
        PdfPTable topTable = new PdfPTable(2);
        topTable.setWidthPercentage(70);
        topTable.setSpacingAfter(16);
        topTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell th1 = new PdfPCell(new Phrase("Médecin", fontBold));
        PdfPCell th2 = new PdfPCell(new Phrase("Consultations", fontBold));
        th1.setBackgroundColor(new BaseColor(235, 242, 255));
        th2.setBackgroundColor(new BaseColor(235, 242, 255));
        th1.setPadding(6); th2.setPadding(6);
        topTable.addCell(th1); topTable.addCell(th2);

        int rank = 1;
        for (Map.Entry<String, Integer> e : topData.entrySet()) {
            PdfPCell c1 = new PdfPCell(new Phrase(rank + ". " + e.getKey(), fontNormal));
            PdfPCell c2 = new PdfPCell(new Phrase(String.valueOf(e.getValue()), fontNormal));
            c1.setPadding(5); c2.setPadding(5);
            topTable.addCell(c1); topTable.addCell(c2);
            rank++;
        }
        doc.add(topTable);

        // ── Activité récente ──────────────────────────────────────────────
        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        Paragraph recentTitle = new Paragraph("Dernières Consultations", fontSection);
        recentTitle.setSpacingAfter(8);
        doc.add(recentTitle);

        List<String[]> recents = stats.getDernieresConsultations();
        PdfPTable recentTable = new PdfPTable(5);
        recentTable.setWidthPercentage(100);
        recentTable.setSpacingAfter(16);
        recentTable.setWidths(new float[]{0.5f, 2f, 2f, 1.5f, 1f});

        for (String h : new String[]{"ID", "Patient", "Médecin", "Orientation", "Statut"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontBold));
            c.setBackgroundColor(new BaseColor(235, 242, 255));
            c.setPadding(6);
            recentTable.addCell(c);
        }
        for (String[] row : recents) {
            for (int i = 0; i < 5; i++) {
                PdfPCell c = new PdfPCell(new Phrase(row[i] != null ? row[i] : "—", fontNormal));
                c.setPadding(5);
                recentTable.addCell(c);
            }
        }
        doc.add(recentTable);

        // ── Pied de page ──────────────────────────────────────────────────
        doc.add(new Chunk(line));
        Paragraph footer = new Paragraph(
            "REVIVE — Système de Gestion des Urgences  |  Module 3 : Consultation Médicale", fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6);
        doc.add(footer);

        doc.close();
    }

    private void addKpiCell(PdfPTable table, String label, String value,
                             Font fontLabel, Font fontValue) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(200, 216, 238));
        cell.setPadding(10);
        cell.setBackgroundColor(new BaseColor(248, 250, 255));
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", fontLabel));
        p.add(new Chunk(value, fontValue));
        cell.addElement(p);
        table.addCell(cell);
    }

    // ── Utilitaires ───────────────────────────────────────────────────────

    private Label styledLabel(String text, String color, int size) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + color + "; -fx-font-size: " + size + "px;");
        return l;
    }

    private void naviguer(String fxml) {
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/" + fxml);
            if (fxmlUrl == null) { System.out.println("FXML introuvable : " + fxml); return; }
            Parent newRoot = FXMLLoader.load(fxmlUrl);
            // Chercher une scène valide depuis n'importe quel nœud disponible
            javafx.scene.Node ref = lblTotal != null ? lblTotal
                                  : lblEnCours != null ? lblEnCours
                                  : lblOrdos;
            if (ref == null || ref.getScene() == null) return;
            Stage stage = (Stage) ref.getScene().getWindow();
            double screenW = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double screenH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            Scene scene = new Scene(newRoot, screenW, screenH);
            URL cssUrl = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            if (!stage.isMaximized()) stage.setMaximized(true);
        } catch (IOException e) {
            System.out.println("Navigation error: " + e.getMessage());
        }
    }

    private void styleAlert(Alert a) {
        URL cssUrl = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
        if (cssUrl != null) {
            try { a.getDialogPane().getStylesheets().add(cssUrl.toExternalForm()); } catch (Exception ignored) {}
        }
    }
}

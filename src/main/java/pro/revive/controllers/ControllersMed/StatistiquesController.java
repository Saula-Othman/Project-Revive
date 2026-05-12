package pro.revive.controllers.ControllersMed;

import pro.revive.services.ServicesMed.StatistiquesService;
import pro.revive.services.ServicesMed.TriageDataService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class StatistiquesController implements Initializable {

    // ── KPI ───────────────────────────────────────────────────────────────
    @FXML private Label lblTotal;
    @FXML private Label lblTauxCloture;
    @FXML private Label lblDureeMoy;
    @FXML private Label lblOrdos;
    @FXML private Label lblMoyOrdos;
    @FXML private Label lblCeMois;
    @FXML private Label lblOrientTotal;

    // ── Graphiques ────────────────────────────────────────────────────────
    @FXML private PieChart  pieOrientation;
    @FXML private BarChart<String, Number> barActivite;
    @FXML private CategoryAxis barXAxis;
    @FXML private NumberAxis   barYAxis;

    @FXML private AreaChart<String, Number> areaHeure;
    @FXML private CategoryAxis areaXAxis;
    @FXML private NumberAxis   areaYAxis;

    @FXML private BarChart<String, Number> barTriage;
    @FXML private CategoryAxis triageXAxis;
    @FXML private NumberAxis   triageYAxis;
    // ── Listes ────────────────────────────────────────────────────────────
    @FXML private VBox vboxDiagnostics;
    @FXML private VBox vboxTopMedecins;
    @FXML private VBox vboxTriageStats;

    private final StatistiquesService stats   = new StatistiquesService();
    private final TriageDataService       triageSvc = new TriageDataService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        javafx.application.Platform.runLater(this::chargerTout);

        // Injecter l'assistant vocal dès que la scène est attachée
        if (lblTotal != null) {
            lblTotal.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && newScene.getRoot() instanceof javafx.scene.layout.StackPane sp) {
                    boolean dejaPresent = sp.getChildren().stream()
                        .anyMatch(n -> "assistant".equals(n.getUserData()));
                    if (!dejaPresent) {
                        GlobalAssistantController assistant = new GlobalAssistantController(sp);
                        assistant.setNavigationCallback(this::naviguer);
                    }
                }
            });
        }
    }

    // ── Chargement principal ──────────────────────────────────────────────

    private void chargerTout() {
        try { chargerKpi();           } catch (Exception e) { System.out.println("[Stats] KPI: " + e.getMessage()); }
        try { chargerPieOrientation();} catch (Exception e) { System.out.println("[Stats] Pie: " + e.getMessage()); }
        try { chargerBarActivite();   } catch (Exception e) { System.out.println("[Stats] Bar: " + e.getMessage()); }
        try { chargerAreaHeure();     } catch (Exception e) { System.out.println("[Stats] Area: " + e.getMessage()); }
        try { chargerDiagnostics();   } catch (Exception e) { System.out.println("[Stats] Diag: " + e.getMessage()); }
        try { chargerTopMedecins();   } catch (Exception e) { System.out.println("[Stats] Top: " + e.getMessage()); }
        try { chargerTriageStats();   } catch (Exception e) { System.out.println("[Stats] Triage: " + e.getMessage()); }
    }

    // ── KPI ───────────────────────────────────────────────────────────────

    private void chargerKpi() {
        int total    = stats.getTotalConsultations();
        double taux  = stats.getTauxCloture();
        double duree = stats.getDureeMoyenneMinutes();
        int ordos    = stats.getTotalOrdonnances();
        double moy   = stats.getMoyenneOrdonnancesParConsultation();
        int mois     = stats.getConsultationsCeMois();

        if (lblTotal       != null) lblTotal.setText(String.valueOf(total));
        if (lblTauxCloture != null) lblTauxCloture.setText(String.format("%.0f%%", taux));
        if (lblDureeMoy    != null) lblDureeMoy.setText(duree > 0 ? String.format("%.0f min", duree) : "—");
        if (lblOrdos       != null) lblOrdos.setText(String.valueOf(ordos));
        if (lblMoyOrdos    != null) lblMoyOrdos.setText(String.format("%.1f", moy));
        if (lblCeMois      != null) lblCeMois.setText(String.valueOf(mois));
    }

    // ── Pie orientation ───────────────────────────────────────────────────

    private void chargerPieOrientation() {
        if (pieOrientation == null) return;
        Map<String, Integer> data = stats.getRepartitionOrientation();
        pieOrientation.getData().clear();
        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        if (lblOrientTotal != null) lblOrientTotal.setText(total + " au total");
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

    // ── Bar activité 7j ───────────────────────────────────────────────────

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
        javafx.application.Platform.runLater(() ->
            barActivite.lookupAll(".bar").forEach(n -> n.setStyle("-fx-bar-fill: #0B4EA2;")));
    }

    // ── Area chart par heure ──────────────────────────────────────────────

    private void chargerAreaHeure() {
        if (areaHeure == null) return;
        Map<String, Integer> data = stats.getConsultationsParHeure();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Consultations");
        if (data.isEmpty()) {
            for (int h = 8; h <= 20; h++) series.getData().add(new XYChart.Data<>(h + "h", 0));
        } else {
            data.forEach((h, nb) -> series.getData().add(new XYChart.Data<>(h, nb)));
        }
        areaHeure.getData().clear();
        areaHeure.getData().add(series);
        javafx.application.Platform.runLater(() -> {
            areaHeure.lookupAll(".chart-series-area-fill").forEach(n ->
                n.setStyle("-fx-fill: rgba(14,155,138,0.20);"));
            areaHeure.lookupAll(".chart-series-area-line").forEach(n ->
                n.setStyle("-fx-stroke: #0E9B8A; -fx-stroke-width: 2.5px;"));
        });
    }

    // ── Top diagnostics ───────────────────────────────────────────────────

    private void chargerDiagnostics() {
        if (vboxDiagnostics == null) return;
        vboxDiagnostics.getChildren().clear();
        Map<String, Integer> data = stats.getTopDiagnostics();
        if (data.isEmpty()) {
            Label empty = new Label("Aucun diagnostic enregistré.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            vboxDiagnostics.getChildren().add(empty);
            return;
        }
        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int rank = 1;
        String[] colors = {"#C0392B", "#D35400", "#B7950B", "#0B4EA2", "#0E9B8A"};
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            String color = colors[Math.min(rank - 1, colors.length - 1)];
            double pct   = (double) e.getValue() / max;

            VBox row = new VBox(4);
            row.setStyle("-fx-padding: 8 0 4 0;");

            // Ligne texte
            HBox top = new HBox(8);
            top.setAlignment(Pos.CENTER_LEFT);
            Label num = new Label(rank + ".");
            num.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px; -fx-min-width: 18;");
            Label diag = new Label(e.getKey());
            diag.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 12px; -fx-font-weight: bold;");
            diag.setWrapText(true);
            diag.setMaxWidth(260);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            Label cnt = new Label(e.getValue() + " cas");
            cnt.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
            top.getChildren().addAll(num, diag, sp, cnt);

            // Barre de progression
            StackPane bar = new StackPane();
            bar.setPrefHeight(6);
            Region bg = new Region();
            bg.setPrefHeight(6);
            bg.setStyle("-fx-background-color: #F1F5F9; -fx-background-radius: 3;");
            Region fill = new Region();
            fill.setPrefHeight(6);
            fill.setMaxWidth(Double.MAX_VALUE);
            fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3;");
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);
            bar.getChildren().addAll(bg, fill);
            // Animer la largeur via binding
            fill.prefWidthProperty().bind(bar.widthProperty().multiply(pct));

            row.getChildren().addAll(top, bar);

            // Séparateur
            if (rank < data.size()) {
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: #F1F5F9;");
                row.getChildren().add(sep);
            }
            vboxDiagnostics.getChildren().add(row);
            rank++;
        }
    }

    // ── Top médecins ──────────────────────────────────────────────────────

    private void chargerTopMedecins() {
        if (vboxTopMedecins == null) return;
        vboxTopMedecins.getChildren().clear();
        Map<String, Integer> top = stats.getTopMedecins();
        if (top.isEmpty()) {
            Label empty = new Label("Aucune donnée disponible.");
            empty.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
            vboxTopMedecins.getChildren().add(empty);
            return;
        }
        int max  = top.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int rank = 1;
        for (Map.Entry<String, Integer> e : top.entrySet()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 0;");

            // Médaille
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
            Label lblRank = new Label(medal);
            lblRank.setStyle("-fx-font-size: 14px; -fx-min-width: 28;");

            // Nom
            Label lblNom = new Label(e.getKey());
            lblNom.setStyle("-fx-text-fill: #1E293B; -fx-font-size: 13px; -fx-font-weight: bold;");
            HBox.setHgrow(lblNom, Priority.ALWAYS);

            // Barre
            double pct = (double) e.getValue() / max;
            StackPane barContainer = new StackPane();
            barContainer.setPrefWidth(140);
            barContainer.setMaxWidth(140);
            Region barBg = new Region();
            barBg.setPrefHeight(8);
            barBg.setStyle("-fx-background-color: #EEF4FB; -fx-background-radius: 4;");
            Region barFill = new Region();
            barFill.setPrefHeight(8);
            barFill.setStyle("-fx-background-color: #0B4EA2; -fx-background-radius: 4;");
            barFill.prefWidthProperty().bind(barContainer.widthProperty().multiply(pct));
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
            barContainer.getChildren().addAll(barBg, barFill);

            // Compteur
            Label lblCount = new Label(e.getValue() + " cons.");
            lblCount.setStyle("-fx-text-fill: #0B4EA2; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 65;");

            row.getChildren().addAll(lblRank, lblNom, barContainer, lblCount);
            vboxTopMedecins.getChildren().add(row);

            if (rank < top.size()) {
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: #F0F4F8;");
                vboxTopMedecins.getChildren().add(sep);
            }
            rank++;
        }
    }

    // ── Triage stats ──────────────────────────────────────────────────────

    private void chargerTriageStats() {
        // Compteurs
        int critique = triageSvc.countByNiveau("CRITIQUE");
        int urgent   = triageSvc.countByNiveau("URGENT");
        int modere   = triageSvc.countByNiveau("MODERE");
        int stable   = triageSvc.countByNiveau("STABLE");
        int total    = critique + urgent + modere + stable;

        // Barres de progression uniquement
        if (vboxTriageStats != null) {
            vboxTriageStats.getChildren().clear();
            addTriageBar("🔴  CRITIQUE", critique, total, "#C0392B", "#FDECEA");
            addTriageBar("🟠  URGENT",   urgent,   total, "#D35400", "#FDF0E8");
            addTriageBar("🟡  MODÉRÉ",   modere,   total, "#B7950B", "#FDF8E1");
            addTriageBar("🟢  STABLE",   stable,   total, "#1E8449", "#EAF7EE");
        }
    }

    private void addTriageBar(String label, int count, int total, String color, String bgColor) {
        double pct = total > 0 ? (double) count / total : 0;

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; -fx-font-weight: bold; -fx-min-width: 110;");

        StackPane bar = new StackPane();
        HBox.setHgrow(bar, Priority.ALWAYS);
        bar.setPrefHeight(14);
        Region bg = new Region();
        bg.setPrefHeight(14);
        bg.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 7;");
        Region fill = new Region();
        fill.setPrefHeight(14);
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 7;");
        fill.prefWidthProperty().bind(bar.widthProperty().multiply(pct));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        bar.getChildren().addAll(bg, fill);

        Label cnt = new Label(count + " (" + String.format("%.0f%%", pct * 100) + ")");
        cnt.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 70;");

        row.getChildren().addAll(lbl, bar, cnt);
        vboxTriageStats.getChildren().add(row);
    }

    // ── Navigation ────────────────────────────────────────────────────────

    @FXML private void onDashboard() {
        naviguer("dashboardMed.fxml");
    }

    @FXML private void onConsultations() {
        naviguer("ConsultationList.fxml");
    }

    @FXML private void onExportPdf() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.setInitialFileName("Statistiques_REVIVE_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        Stage stage = (Stage) lblTotal.getScene().getWindow();
        File fichier = fc.showSaveDialog(stage);
        if (fichier == null) return;

        try {
            genererPdf(fichier);
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

    private void genererPdf(File fichier) throws Exception {
        Document doc = new Document(PageSize.A4, 45, 45, 55, 55);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(fichier));
        doc.open();

        // ── Polices ───────────────────────────────────────────────────────
        Font fTitre    = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   new BaseColor(11, 78, 162));
        Font fSub      = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(100, 116, 139));
        Font fSection  = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD,   new BaseColor(11, 78, 162));
        Font fNormal   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        Font fBold     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.BLACK);
        Font fKpi      = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,   new BaseColor(11, 78, 162));
        Font fKpiLbl   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(100, 116, 139));
        Font fSmall    = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, new BaseColor(148, 163, 184));
        Font fWhite    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);

        LineSeparator teal = new LineSeparator(1.5f, 100f, new BaseColor(14, 155, 138), Element.ALIGN_CENTER, -2);
        LineSeparator light = new LineSeparator(0.5f, 100f, new BaseColor(226, 232, 240), Element.ALIGN_CENTER, -2);

        // ── En-tête ───────────────────────────────────────────────────────
        Paragraph titre = new Paragraph("REVIVE — Statistiques & Analyses", fTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);

        Paragraph dateLbl = new Paragraph(
            "Rapport généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")),
            fSub);
        dateLbl.setAlignment(Element.ALIGN_CENTER);
        dateLbl.setSpacingAfter(6);
        doc.add(dateLbl);
        doc.add(new Chunk(teal));
        doc.add(new Paragraph(" "));

        // ── KPIs ──────────────────────────────────────────────────────────
        Paragraph kpiTitre = new Paragraph("Indicateurs Clés de Performance", fSection);
        kpiTitre.setSpacingAfter(8);
        doc.add(kpiTitre);

        PdfPTable kpiTable = new PdfPTable(6);
        kpiTable.setWidthPercentage(100);
        kpiTable.setSpacingAfter(16);
        kpiTable.setWidths(new float[]{1f, 1f, 1f, 1f, 1f, 1f});

        addKpiCell(kpiTable, "Total",           lblTotal       != null ? lblTotal.getText()       : "—", fKpiLbl, fKpi);
        addKpiCell(kpiTable, "Taux clôture",    lblTauxCloture != null ? lblTauxCloture.getText() : "—", fKpiLbl, fKpi);
        addKpiCell(kpiTable, "Durée moyenne",   lblDureeMoy    != null ? lblDureeMoy.getText()    : "—", fKpiLbl, fKpi);
        addKpiCell(kpiTable, "Ordonnances",     lblOrdos       != null ? lblOrdos.getText()       : "—", fKpiLbl, fKpi);
        addKpiCell(kpiTable, "Moy. ordonnances",lblMoyOrdos    != null ? lblMoyOrdos.getText()    : "—", fKpiLbl, fKpi);
        addKpiCell(kpiTable, "Ce mois",         lblCeMois      != null ? lblCeMois.getText()      : "—", fKpiLbl, fKpi);
        doc.add(kpiTable);

        doc.add(new Chunk(light));
        doc.add(new Paragraph(" "));

        // ── Répartition par orientation ───────────────────────────────────
        Paragraph orientTitre = new Paragraph("Répartition par Orientation", fSection);
        orientTitre.setSpacingAfter(8);
        doc.add(orientTitre);

        Map<String, Integer> orientData = stats.getRepartitionOrientation();
        int orientTotal = orientData.values().stream().mapToInt(Integer::intValue).sum();

        PdfPTable orientTable = new PdfPTable(3);
        orientTable.setWidthPercentage(70);
        orientTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        orientTable.setSpacingAfter(16);
        orientTable.setWidths(new float[]{2f, 1f, 1f});

        addTableHeader(orientTable, fWhite, "Orientation", "Nombre", "Pourcentage");
        for (Map.Entry<String, Integer> e : orientData.entrySet()) {
            double pct = orientTotal > 0 ? (double) e.getValue() / orientTotal * 100 : 0;
            addTableRow(orientTable, fNormal, fBold,
                e.getKey(),
                String.valueOf(e.getValue()),
                String.format("%.1f%%", pct));
        }
        doc.add(orientTable);

        doc.add(new Chunk(light));
        doc.add(new Paragraph(" "));

        // ── Activité 7 jours ──────────────────────────────────────────────
        Paragraph actTitre = new Paragraph("Activité — 7 Derniers Jours", fSection);
        actTitre.setSpacingAfter(8);
        doc.add(actTitre);

        Map<String, Integer> actData = stats.getActivite7Jours();
        PdfPTable actTable = new PdfPTable(2);
        actTable.setWidthPercentage(50);
        actTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        actTable.setSpacingAfter(16);

        addTableHeader(actTable, fWhite, "Date", "Consultations");
        for (Map.Entry<String, Integer> e : actData.entrySet()) {
            addTableRow(actTable, fNormal, fBold, e.getKey(), String.valueOf(e.getValue()));
        }
        if (actData.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucune donnée", fNormal));
            empty.setColspan(2); empty.setPadding(6);
            actTable.addCell(empty);
        }
        doc.add(actTable);

        doc.add(new Chunk(light));
        doc.add(new Paragraph(" "));

        // ── Top médecins ──────────────────────────────────────────────────
        Paragraph topTitre = new Paragraph("Top Médecins par Consultations", fSection);
        topTitre.setSpacingAfter(8);
        doc.add(topTitre);

        Map<String, Integer> topData = stats.getTopMedecins();
        PdfPTable topTable = new PdfPTable(3);
        topTable.setWidthPercentage(70);
        topTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        topTable.setSpacingAfter(16);
        topTable.setWidths(new float[]{0.4f, 2f, 1f});

        addTableHeader(topTable, fWhite, "Rang", "Médecin", "Consultations");
        int rank = 1;
        for (Map.Entry<String, Integer> e : topData.entrySet()) {
            String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
            addTableRow(topTable, fNormal, fBold, medal, e.getKey(), String.valueOf(e.getValue()));
            rank++;
        }
        doc.add(topTable);

        doc.add(new Chunk(light));
        doc.add(new Paragraph(" "));

        // ── Top diagnostics ───────────────────────────────────────────────
        Paragraph diagTitre = new Paragraph("Top Diagnostics", fSection);
        diagTitre.setSpacingAfter(8);
        doc.add(diagTitre);

        Map<String, Integer> diagData = stats.getTopDiagnostics();
        PdfPTable diagTable = new PdfPTable(2);
        diagTable.setWidthPercentage(90);
        diagTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        diagTable.setSpacingAfter(16);
        diagTable.setWidths(new float[]{4f, 1f});

        addTableHeader(diagTable, fWhite, "Diagnostic", "Cas");
        for (Map.Entry<String, Integer> e : diagData.entrySet()) {
            addTableRow(diagTable, fNormal, fBold, e.getKey(), String.valueOf(e.getValue()));
        }
        if (diagData.isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Aucun diagnostic enregistré", fNormal));
            empty.setColspan(2); empty.setPadding(6);
            diagTable.addCell(empty);
        }
        doc.add(diagTable);

        doc.add(new Chunk(light));
        doc.add(new Paragraph(" "));

        // ── Triage ────────────────────────────────────────────────────────
        Paragraph triageTitre = new Paragraph("Répartition Triage", fSection);
        triageTitre.setSpacingAfter(8);
        doc.add(triageTitre);

        int critique = triageSvc.countByNiveau("CRITIQUE");
        int urgent   = triageSvc.countByNiveau("URGENT");
        int modere   = triageSvc.countByNiveau("MODERE");
        int stable   = triageSvc.countByNiveau("STABLE");
        int triageTotal = critique + urgent + modere + stable;

        PdfPTable triageTable = new PdfPTable(3);
        triageTable.setWidthPercentage(60);
        triageTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        triageTable.setSpacingAfter(16);

        addTableHeader(triageTable, fWhite, "Niveau", "Patients", "Pourcentage");

        String[][] triageRows = {
            {"🔴 CRITIQUE", String.valueOf(critique), triageTotal > 0 ? String.format("%.1f%%", (double)critique/triageTotal*100) : "0%"},
            {"🟠 URGENT",   String.valueOf(urgent),   triageTotal > 0 ? String.format("%.1f%%", (double)urgent/triageTotal*100)   : "0%"},
            {"🟡 MODÉRÉ",   String.valueOf(modere),   triageTotal > 0 ? String.format("%.1f%%", (double)modere/triageTotal*100)   : "0%"},
            {"🟢 STABLE",   String.valueOf(stable),   triageTotal > 0 ? String.format("%.1f%%", (double)stable/triageTotal*100)   : "0%"},
        };
        for (String[] row : triageRows) {
            addTableRow(triageTable, fNormal, fBold, row[0], row[1], row[2]);
        }
        doc.add(triageTable);

        // ── Pied de page ──────────────────────────────────────────────────
        doc.add(new Chunk(teal));
        Paragraph footer = new Paragraph(
            "REVIVE — Système de Gestion des Urgences  |  Module 3 : Statistiques & Analyses", fSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(6);
        doc.add(footer);

        doc.close();
    }

    // ── Helpers PDF ───────────────────────────────────────────────────────

    private void addKpiCell(PdfPTable table, String label, String value, Font fLabel, Font fValue) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        cell.setPadding(10);
        cell.setBackgroundColor(new BaseColor(248, 250, 255));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(value + "\n", fValue));
        p.add(new Chunk(label, fLabel));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, Font font, String... headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, font));
            cell.setBackgroundColor(new BaseColor(11, 78, 162));
            cell.setPadding(7);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void addTableRow(PdfPTable table, Font fNormal, Font fBold, String... values) {
        for (int i = 0; i < values.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(values[i] != null ? values[i] : "—",
                i == 0 ? fNormal : fBold));
            cell.setPadding(6);
            cell.setBorderColor(new BaseColor(226, 232, 240));
            if (i > 0) cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
    }

    private void styleAlert(Alert a) {
        try {
            URL css = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (css != null) a.getDialogPane().getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
    }

    private void naviguer(String fxml) {
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/" + fxml);
            if (fxmlUrl == null) return;
            Parent newRoot = FXMLLoader.load(fxmlUrl);
            Stage stage = (Stage) lblTotal.getScene().getWindow();
            double w = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double h = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            Scene scene = new Scene(newRoot, w, h);
            URL css = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            if (!stage.isMaximized()) stage.setMaximized(true);
        } catch (IOException e) {
            System.out.println("[Stats] Navigation: " + e.getMessage());
        }
    }
}

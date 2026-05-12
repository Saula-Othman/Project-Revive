package pro.revive.controllers.ControllersMed;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.OrdonnanceService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AfficherConsultationController implements Initializable {

    @FXML private Label lblSideTotal, lblSideOrdos;
    @FXML private Label lblTotal, lblEnCours, lblCloturees, lblOrdos, lblCount;
    @FXML private TextField txtRecherche;
    @FXML private ComboBox<String> cmbFiltre;
    @FXML private TableView<Consultation> tableConsultations;
    @FXML private TableColumn<Consultation, String> colId, colPatient, colMedecin,
            colDebut, colFin, colDiagnostic, colOrientation;
    @FXML private TableColumn<Consultation, Void> colActions;

    private final ConsultationService cs = new ConsultationService();
    private final OrdonnanceService   os = new OrdonnanceService();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ObservableList<Consultation> allData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbFiltre.setItems(FXCollections.observableArrayList("Toutes", "En cours", "Sortie", "Hospitalisation", "Transfert"));
        cmbFiltre.setValue("Toutes");
        configurerColonnes();
        chargerDonnees();
    }

    private void configurerColonnes() {
        colId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getIdConsultation()));
        colPatient.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNomPatient())));
        colMedecin.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getNomMedecin())));
        colDebut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateHeureDebut() != null ? c.getValue().getDateHeureDebut().format(FMT) : "—"));
        colFin.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateHeureFin() != null ? c.getValue().getDateHeureFin().format(FMT) : "En cours"));
        colDiagnostic.setCellValueFactory(c -> {
            String d = c.getValue().getDiagnostic();
            return new SimpleStringProperty(d == null ? "—" : d.length() > 35 ? d.substring(0, 35) + "…" : d);
        });

        // Orientation avec badge coloré
        colOrientation.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setGraphic(null); return; }
                String o = getTableRow().getItem().getOrientation();
                Label badge = new Label(o != null ? o : "En cours");
                String style = "-fx-background-radius: 6; -fx-padding: 3 8; -fx-font-size: 10px; -fx-font-weight: bold;";
                if      (o == null)                  badge.setStyle(style + "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;");
                else if ("Sortie".equals(o))          badge.setStyle(style + "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;");
                else if ("Hospitalisation".equals(o)) badge.setStyle(style + "-fx-background-color: #EBF2FF; -fx-text-fill: #0B4EA2;");
                else if ("Transfert".equals(o))       badge.setStyle(style + "-fx-background-color: #FEF3C7; -fx-text-fill: #D97706;");
                setGraphic(badge); setText(null);
            }
        });
        colOrientation.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOrientation()));

        // Boutons actions
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnMod = new Button("✏");
            private final Button btnSup = new Button("🗑");
            private final HBox box = new HBox(6, btnMod, btnSup);
            {
                String styleMod = "-fx-background-color: #EBF2FF; -fx-text-fill: #0B4EA2; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 11px; -fx-cursor: hand;";
                String styleSup = "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-background-radius: 6; -fx-padding: 5 10; -fx-font-size: 11px; -fx-cursor: hand;";
                btnMod.setStyle(styleMod); btnSup.setStyle(styleSup);
                box.setAlignment(Pos.CENTER);
                btnMod.setOnAction(e -> ouvrirModifier(getTableRow().getItem()));
                btnSup.setOnAction(e -> ouvrirSupprimer(getTableRow().getItem()));
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void chargerDonnees() {
        List<Consultation> liste = cs.getData();
        allData.setAll(liste);
        tableConsultations.setItems(allData);

        long enCours   = liste.stream().filter(c -> c.getDateHeureFin() == null).count();
        long cloturees = liste.stream().filter(c -> c.getDateHeureFin() != null).count();
        long nbOrdos   = liste.stream().mapToLong(c -> os.getByConsultation(c.getIdConsultation()).size()).sum();

        lblTotal.setText(String.valueOf(liste.size()));
        lblEnCours.setText(String.valueOf(enCours));
        lblCloturees.setText(String.valueOf(cloturees));
        lblOrdos.setText(String.valueOf(nbOrdos));
        lblCount.setText(liste.size() + " résultat(s)");
        lblSideTotal.setText(liste.size() + " consultations");
        lblSideOrdos.setText(nbOrdos + " ordonnances");
    }

    @FXML private void handleRefresh() { chargerDonnees(); }

    @FXML private void handleAjouter() { naviguer("AjouterConsultation.fxml"); }

    @FXML private void handleRecherche() {
        String q = txtRecherche.getText().toLowerCase().trim();
        if (q.isEmpty()) { tableConsultations.setItems(allData); lblCount.setText(allData.size() + " résultat(s)"); return; }
        var filtered = allData.filtered(c ->
                nvl(c.getNomPatient()).toLowerCase().contains(q) ||
                nvl(c.getNomMedecin()).toLowerCase().contains(q) ||
                nvl(c.getDiagnostic()).toLowerCase().contains(q));
        tableConsultations.setItems(filtered);
        lblCount.setText(filtered.size() + " résultat(s)");
    }

    @FXML private void handleFiltre() {
        String f = cmbFiltre.getValue();
        if (f == null || "Toutes".equals(f)) { tableConsultations.setItems(allData); }
        else if ("En cours".equals(f))       { tableConsultations.setItems(allData.filtered(c -> c.getDateHeureFin() == null)); }
        else                                 { tableConsultations.setItems(allData.filtered(c -> f.equals(c.getOrientation()))); }
        lblCount.setText(tableConsultations.getItems().size() + " résultat(s)");
    }

    @FXML private void handleNavOrdonnances() {}

    @FXML private void handleNavDashboard() { naviguer("dashboardMed.fxml"); }

    @FXML private void handleExportPdf() {
        // Exporte les items actuellement affichés (filtrés ou non)
        List<Consultation> liste = new ArrayList<>(tableConsultations.getItems());
        if (liste.isEmpty()) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Export PDF"); a.setHeaderText(null);
            a.setContentText("Aucune consultation à exporter.");
            a.showAndWait(); return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer la liste PDF");
        fc.setInitialFileName("Consultations_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File fichier = fc.showSaveDialog(tableConsultations.getScene().getWindow());
        if (fichier == null) return;
        try {
            exporterListePdf(fichier, liste);
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export réussi"); ok.setHeaderText(null);
            ok.setContentText("Liste exportée :\n" + fichier.getAbsolutePath());
            ok.showAndWait();
        } catch (Exception e) {
            Alert err = new Alert(Alert.AlertType.ERROR);
            err.setTitle("Erreur PDF"); err.setHeaderText(null);
            err.setContentText("Impossible de générer le PDF :\n" + e.getMessage());
            err.showAndWait();
        }
    }

    private void exporterListePdf(File fichier, java.util.Collection<Consultation> liste) throws Exception {
        Document doc = new Document(PageSize.A4.rotate(), 36, 36, 50, 50);
        PdfWriter.getInstance(doc, new FileOutputStream(fichier));
        doc.open();

        Font fontTitre  = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD,   new BaseColor(26, 107, 90));
        Font fontSub    = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, new BaseColor(100, 100, 100));
        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
        Font fontCell   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.BLACK);
        Font fontSmall  = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, new BaseColor(120, 120, 120));

        // En-tête
        Paragraph titre = new Paragraph("REVIVE — Liste des Consultations", fontTitre);
        titre.setAlignment(Element.ALIGN_CENTER);
        doc.add(titre);
        Paragraph sub = new Paragraph(
            "Exporté le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
            + "  |  " + liste.size() + " consultation(s)", fontSub);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(10);
        doc.add(sub);

        LineSeparator line = new LineSeparator(1f, 100f, new BaseColor(46, 196, 160), Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        // Tableau
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 1.8f, 1.8f, 1.5f, 1.5f, 2.5f, 1.2f});

        BaseColor headerBg = new BaseColor(11, 78, 162);
        for (String h : new String[]{"ID", "Patient", "Médecin", "Début", "Fin", "Diagnostic", "Orientation"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fontHeader));
            c.setBackgroundColor(headerBg);
            c.setPadding(7); c.setBorderColor(new BaseColor(8, 60, 130));
            table.addCell(c);
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");
        boolean alt = false;
        for (Consultation c : liste) {
            BaseColor rowBg = alt ? new BaseColor(248, 250, 255) : BaseColor.WHITE;
            String[] vals = {
                "#" + c.getIdConsultation(),
                nvl(c.getNomPatient()),
                nvl(c.getNomMedecin()),
                c.getDateHeureDebut() != null ? c.getDateHeureDebut().format(fmt) : "—",
                c.getDateHeureFin()   != null ? c.getDateHeureFin().format(fmt)   : "En cours",
                c.getDiagnostic() != null
                    ? (c.getDiagnostic().length() > 60 ? c.getDiagnostic().substring(0, 60) + "…" : c.getDiagnostic())
                    : "—",
                nvl(c.getOrientation())
            };
            for (String v : vals) {
                PdfPCell cell = new PdfPCell(new Phrase(v, fontCell));
                cell.setBackgroundColor(rowBg);
                cell.setPadding(5);
                cell.setBorderColor(new BaseColor(220, 228, 240));
                table.addCell(cell);
            }
            alt = !alt;
        }
        doc.add(table);

        // Pied de page
        doc.add(new Paragraph(" "));
        doc.add(new Chunk(line));
        Paragraph footer = new Paragraph(
            "REVIVE — Système de Gestion des Urgences  |  Module 3 : Consultation Médicale", fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
    }

    private void ouvrirModifier(Consultation c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMed/module3/fxml/ModifierConsultation.fxml"));
            Parent root = loader.load();
            ((ModifierConsultationController) loader.getController()).setConsultation(c);
            ((Stage) tableConsultations.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { System.out.println(e.getMessage()); }
    }

    private void ouvrirSupprimer(Consultation c) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMed/module3/fxml/SupprimerConsultation.fxml"));
            Parent root = loader.load();
            ((SupprimerConsultationController) loader.getController()).setConsultation(c);
            ((Stage) tableConsultations.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { System.out.println(e.getMessage()); }
    }

    private void naviguer(String fxml) {
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/" + fxml);
            if (fxmlUrl == null) return;
            Parent root = FXMLLoader.load(fxmlUrl);
            ((Stage) tableConsultations.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { System.out.println(e.getMessage()); }
    }

    private String nvl(String s) { return s != null ? s : "—"; }
}

package pro.revive.controllers.ControllersMed;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.Ordonnance;
import pro.revive.services.ServicesMed.OrdonnanceService;
import pro.revive.services.ServicesMed.PdfExportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Gestion des ordonnances d'une consultation.
 * Fonctionnalités : Ajouter, Modifier, Supprimer, Exporter PDF.
 */
public class OrdonnancesDialogController implements Initializable {

    @FXML private Label labelTitreConsultation;
    @FXML private Label labelPatient;
    @FXML private Label labelDiagnostic;

    @FXML private TableView<Ordonnance>            tableOrdonnances;
    @FXML private TableColumn<Ordonnance, Integer> colIdOrdo;
    @FXML private TableColumn<Ordonnance, String>  colMedicament;
    @FXML private TableColumn<Ordonnance, String>  colPosologie;
    @FXML private TableColumn<Ordonnance, Integer> colDuree;

    @FXML private Button btnAjouter;
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnExportPdf;
    @FXML private Button btnFermer;

    private final OrdonnanceService ordonnanceService = new OrdonnanceService();
    private final PdfExportService  pdfExportService  = new PdfExportService();
    private Consultation consultation;
    private final ObservableList<Ordonnance> ordonnancesList = FXCollections.observableArrayList();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        tableOrdonnances.setItems(ordonnancesList);
        tableOrdonnances.setPlaceholder(new Label("Aucune ordonnance pour cette consultation."));
    }

    private void configurerColonnes() {
        colIdOrdo.setCellValueFactory(new PropertyValueFactory<>("idOrdo"));
        colMedicament.setCellValueFactory(new PropertyValueFactory<>("medicament"));
        colPosologie.setCellValueFactory(new PropertyValueFactory<>("posologie"));
        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeJours"));

        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item + " j");
            }
        });

        colPosologie.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setTooltip(null); return; }
                setText(item.length() > 50 ? item.substring(0, 50) + "…" : item);
                Tooltip tip = new Tooltip(item);
                tip.setWrapText(true); tip.setMaxWidth(350);
                setTooltip(tip);
            }
        });
    }

    public void setConsultation(Consultation c) {
        this.consultation = c;
        labelTitreConsultation.setText("Consultation #" + c.getIdConsultation());
        labelPatient.setText(c.getNomPatient() != null ? c.getNomPatient() : "—");
        String diag = c.getDiagnostic();
        labelDiagnostic.setText(diag != null ? (diag.length() > 80 ? diag.substring(0, 80) + "…" : diag) : "—");
        chargerOrdonnances();
    }

    public void chargerOrdonnances() {
        try {
            ordonnancesList.setAll(ordonnanceService.getByConsultation(consultation.getIdConsultation()));
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les ordonnances :\n" + e.getMessage());
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────

    @FXML private void onAjouter() {
        ouvrirDialogueOrdonnance(null);
    }

    /** Ouvre le dialogue en mode modification pour l'ordonnance sélectionnée. */
    @FXML private void onModifier() {
        Ordonnance sel = tableOrdonnances.getSelectionModel().getSelectedItem();
        if (sel == null) {
            afficherAvertissement("Aucune sélection", "Sélectionnez une ordonnance à modifier.");
            return;
        }
        ouvrirDialogueOrdonnance(sel);
    }

    @FXML private void onSupprimer() {
        Ordonnance sel = tableOrdonnances.getSelectionModel().getSelectedItem();
        if (sel == null) { afficherAvertissement("Aucune sélection", "Sélectionnez une ordonnance à supprimer."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer");
        confirm.setHeaderText("Supprimer l'ordonnance « " + sel.getMedicament() + " » ?");
        confirm.setContentText("Cette action est irréversible.");
        styleAlert(confirm);
        if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isPresent()) {
            try { ordonnanceService.deleteEntity(sel); chargerOrdonnances(); }
            catch (Exception e) { afficherErreur("Erreur", e.getMessage()); }
        }
    }

    /**
     * Feature 4 — Export PDF professionnel via PdfExportService.
     */
    @FXML private void onExportPdf() {
        List<Ordonnance> ordonnances = ordonnanceService.getByConsultation(consultation.getIdConsultation());
        if (ordonnances.isEmpty()) {
            afficherAvertissement("Aucune ordonnance", "Il n'y a aucune ordonnance a exporter.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer l'ordonnance PDF");
        fc.setInitialFileName("Ordonnance_" + consultation.getNomPatient() + "_"
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File fichier = fc.showSaveDialog(btnExportPdf.getScene().getWindow());
        if (fichier == null) return;
        try {
            // Feature 4 — utilise PdfExportService (PDF professionnel)
            pdfExportService.exportOrdonnance(consultation, ordonnances, fichier.getAbsolutePath());
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Export reussi");
            ok.setHeaderText(null);
            ok.setContentText("Ordonnance exportee :\n" + fichier.getAbsolutePath());
            styleAlert(ok); ok.showAndWait();
        } catch (Exception e) {
            afficherErreur("Erreur PDF", "Impossible de generer le PDF :\n" + e.getMessage());
        }
    }

    @FXML private void onFermer() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        }
    }

    private Runnable onCloseCallback;

    public void setOnCloseCallback(Runnable callback) {
        this.onCloseCallback = callback;
    }

    // ── Génération PDF ────────────────────────────────────────────────────

    private void genererPdf(File fichier, List<Ordonnance> ordonnances) throws Exception {
        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter.getInstance(doc, new FileOutputStream(fichier));
        doc.open();

        // Polices
        Font fontTitre    = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD,   new BaseColor(26, 107, 90));
        Font fontSousTitre= new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, new BaseColor(100, 100, 100));
        Font fontSection  = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   new BaseColor(26, 79, 122));
        Font fontNormal   = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.BLACK);
        Font fontBold     = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   BaseColor.BLACK);
        Font fontMed      = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   new BaseColor(26, 107, 90));
        Font fontSmall    = new Font(Font.FontFamily.HELVETICA,  9, Font.ITALIC, new BaseColor(120, 120, 120));

        // ── En-tête ──────────────────────────────────────────────────────
        Paragraph header = new Paragraph("🏥  REVIVE", fontTitre);
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);

        Paragraph subHeader = new Paragraph("Système de Gestion des Urgences Hospitalières", fontSousTitre);
        subHeader.setAlignment(Element.ALIGN_CENTER);
        doc.add(subHeader);

        // Ligne de séparation
        doc.add(new Paragraph(" "));
        LineSeparator line = new LineSeparator(1f, 100f, new BaseColor(46, 196, 160), Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        // ── Titre ordonnance ─────────────────────────────────────────────
        Paragraph titre = new Paragraph("ORDONNANCE MÉDICALE", fontSection);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(10);
        doc.add(titre);

        // ── Infos consultation ────────────────────────────────────────────
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(14);
        infoTable.setWidths(new float[]{1, 1});

        ajouterCelluleInfo(infoTable, "Patient :", nvl(consultation.getNomPatient()), fontBold, fontNormal);
        ajouterCelluleInfo(infoTable, "Médecin :", nvl(consultation.getNomMedecin()), fontBold, fontNormal);
        ajouterCelluleInfo(infoTable, "Consultation #", String.valueOf(consultation.getIdConsultation()), fontBold, fontNormal);
        String dateFmt = consultation.getDateHeureDebut() != null
            ? consultation.getDateHeureDebut().format(FMT) : "—";
        ajouterCelluleInfo(infoTable, "Date :", dateFmt, fontBold, fontNormal);
        doc.add(infoTable);

        // ── Diagnostic ────────────────────────────────────────────────────
        if (consultation.getDiagnostic() != null && !consultation.getDiagnostic().isBlank()) {
            Paragraph diagLabel = new Paragraph("Diagnostic :", fontSection);
            diagLabel.setSpacingBefore(4);
            doc.add(diagLabel);
            Paragraph diagText = new Paragraph(consultation.getDiagnostic(), fontNormal);
            diagText.setIndentationLeft(16);
            diagText.setSpacingAfter(10);
            doc.add(diagText);
        }

        doc.add(new Chunk(line));
        doc.add(new Paragraph(" "));

        // ── Liste des médicaments ─────────────────────────────────────────
        Paragraph prescLabel = new Paragraph("Prescriptions :", fontSection);
        prescLabel.setSpacingAfter(8);
        doc.add(prescLabel);

        int num = 1;
        for (Ordonnance o : ordonnances) {
            // Numéro + médicament
            Paragraph med = new Paragraph(num + ".  " + o.getMedicament(), fontMed);
            med.setSpacingBefore(6);
            doc.add(med);

            // Posologie
            Paragraph pos = new Paragraph("    Posologie : " + o.getPosologie(), fontNormal);
            doc.add(pos);

            // Durée
            Paragraph dur = new Paragraph("    Durée : " + o.getDureeJours() + " jour(s)", fontNormal);
            dur.setSpacingAfter(4);
            doc.add(dur);

            // Ligne fine entre médicaments
            if (num < ordonnances.size()) {
                LineSeparator sep = new LineSeparator(0.5f, 90f, new BaseColor(200, 200, 200), Element.ALIGN_CENTER, -2);
                doc.add(new Chunk(sep));
            }
            num++;
        }

        // ── Orientation ───────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new Chunk(line));
        Paragraph orient = new Paragraph("Orientation : " + nvl(consultation.getOrientation()), fontBold);
        orient.setSpacingBefore(8);
        doc.add(orient);

        // ── Signature ─────────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph(" "));
        Paragraph sig = new Paragraph("Signature du médecin : ___________________________", fontNormal);
        sig.setAlignment(Element.ALIGN_RIGHT);
        doc.add(sig);
        Paragraph sigNom = new Paragraph("Dr. " + nvl(consultation.getNomMedecin()), fontBold);
        sigNom.setAlignment(Element.ALIGN_RIGHT);
        doc.add(sigNom);

        // ── Pied de page ──────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new Chunk(line));
        Paragraph footer = new Paragraph(
            "Document généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm"))
            + "  |  REVIVE — Système de Gestion des Urgences", fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
    }

    /** Ajoute une paire label/valeur dans le tableau d'infos. */
    private void ajouterCelluleInfo(PdfPTable table, String label, String valeur,
                                     Font fontLabel, Font fontValeur) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fontLabel));
        c1.setBorder(Rectangle.NO_BORDER); c1.setPadding(3);
        PdfPCell c2 = new PdfPCell(new Phrase(valeur, fontValeur));
        c2.setBorder(Rectangle.NO_BORDER); c2.setPadding(3);
        table.addCell(c1); table.addCell(c2);
    }

    // ── Dialogue ajout/modification ───────────────────────────────────────

    private void ouvrirDialogueOrdonnance(Ordonnance ordonnanceAModifier) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "/ResourcesMed/module3/fxml/AddOrdonnanceDialog.fxml"));
            Parent root = loader.load();
            AddOrdonnanceController ctrl = loader.getController();
            ctrl.setConsultationId(consultation.getIdConsultation());
            ctrl.setParentController(this);
            if (ordonnanceAModifier != null) {
                ctrl.setOrdonnancePourModification(ordonnanceAModifier);
            }
            Stage stage = new Stage();
            stage.setTitle(ordonnanceAModifier == null ? "Ajouter une ordonnance" : "Modifier l'ordonnance");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(btnAjouter.getScene().getWindow());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource(
                "/ResourcesMed/module3/css/revive-dark.css").toExternalForm());
            stage.setScene(scene); stage.setResizable(false); stage.showAndWait();
        } catch (IOException e) {
            afficherErreur("Erreur", "Impossible d'ouvrir le formulaire :\n" + e.getMessage());
        }
    }

    // ── Utilitaires ───────────────────────────────────────────────────────
    private String nvl(String s) { return s != null ? s : "—"; }

    private void afficherErreur(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(titre); a.setHeaderText(null); a.setContentText(message); styleAlert(a); a.showAndWait();
    }
    private void afficherAvertissement(String titre, String message) {
        Alert a = new Alert(Alert.AlertType.WARNING); a.setTitle(titre); a.setHeaderText(null); a.setContentText(message); styleAlert(a); a.showAndWait();
    }
    private void styleAlert(Alert a) {
        try { a.getDialogPane().getStylesheets().add(getClass().getResource("/ResourcesMed/module3/css/revive-dark.css").toExternalForm()); } catch(Exception ignored) {}
    }
}

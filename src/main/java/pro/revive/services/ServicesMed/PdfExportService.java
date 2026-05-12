package pro.revive.services.ServicesMed;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.Ordonnance;

import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Feature 4 — Export PDF professionnel d'ordonnance.
 * Utilise iText 5 (deja dans le projet).
 * Genere un PDF complet : en-tete hopital, infos patient, diagnostic ICD-10,
 * tableau medicaments, section labo/imagerie, signature.
 */
public class PdfExportService {

    private static final DateTimeFormatter FMT      = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Couleurs theme REVIVE
    private static final BaseColor COLOR_PRIMARY  = new BaseColor(11, 78, 162);
    private static final BaseColor COLOR_TEAL     = new BaseColor(46, 196, 160);
    private static final BaseColor COLOR_LIGHT    = new BaseColor(240, 246, 255);
    private static final BaseColor COLOR_GREEN    = new BaseColor(22, 163, 74);
    private static final BaseColor COLOR_GRAY     = new BaseColor(107, 114, 128);
    private static final BaseColor COLOR_RED      = new BaseColor(220, 38, 38);

    /**
     * Genere un PDF d'ordonnance complet.
     * @param consultation la consultation avec toutes les infos
     * @param ordonnances  liste des medicaments prescrits
     * @param outputPath   chemin du fichier PDF de sortie
     */
    public void exportOrdonnance(Consultation consultation,
                                  List<Ordonnance> ordonnances,
                                  String outputPath) throws Exception {

        Document doc = new Document(PageSize.A4, 50, 50, 60, 60);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(outputPath));
        doc.open();

        // Polices
        Font fontHospital = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD,   COLOR_PRIMARY);
        Font fontSubtitle = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, COLOR_GRAY);
        Font fontTitle    = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   COLOR_PRIMARY);
        Font fontSection  = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   COLOR_PRIMARY);
        Font fontNormal   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
        Font fontBold     = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.BLACK);
        Font fontSmall    = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, COLOR_GRAY);
        Font fontWhite    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
        Font fontGreen    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   COLOR_GREEN);
        Font fontRed      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   COLOR_RED);

        LineSeparator tealLine = new LineSeparator(1.5f, 100f, COLOR_TEAL, Element.ALIGN_CENTER, -2);
        LineSeparator grayLine = new LineSeparator(0.5f, 100f, COLOR_GRAY, Element.ALIGN_CENTER, -2);

        // ── EN-TETE HOPITAL ───────────────────────────────────────────────
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{2, 1});
        headerTable.setSpacingAfter(8);

        PdfPCell cellHospital = new PdfPCell();
        cellHospital.setBorder(Rectangle.NO_BORDER);
        cellHospital.addElement(new Paragraph("REVIVE", fontHospital));
        cellHospital.addElement(new Paragraph("Service des Urgences Medicales", fontSubtitle));
        cellHospital.addElement(new Paragraph("Module 3 — Consultation Medicale", fontSubtitle));
        headerTable.addCell(cellHospital);

        PdfPCell cellDate = new PdfPCell();
        cellDate.setBorder(Rectangle.NO_BORDER);
        cellDate.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph dateP = new Paragraph("Date : " + LocalDateTime.now().format(FMT_DATE), fontNormal);
        dateP.setAlignment(Element.ALIGN_RIGHT);
        cellDate.addElement(dateP);
        headerTable.addCell(cellDate);
        doc.add(headerTable);

        doc.add(new Chunk(tealLine));
        doc.add(new Paragraph(" "));

        // ── TITRE ─────────────────────────────────────────────────────────
        Paragraph titre = new Paragraph("ORDONNANCE MEDICALE", fontTitle);
        titre.setAlignment(Element.ALIGN_CENTER);
        titre.setSpacingAfter(12);
        doc.add(titre);

        // ── INFOS PATIENT ─────────────────────────────────────────────────
        PdfPTable patientTable = new PdfPTable(2);
        patientTable.setWidthPercentage(100);
        patientTable.setSpacingAfter(12);
        patientTable.setWidths(new float[]{1, 1});

        addInfoRow(patientTable, "Patient :",
            nvl(consultation.getNomPatient()), fontBold, fontNormal);
        addInfoRow(patientTable, "Medecin :",
            "Dr. " + nvl(consultation.getNomMedecin()), fontBold, fontNormal);
        addInfoRow(patientTable, "Consultation #",
            String.valueOf(consultation.getIdConsultation()), fontBold, fontNormal);
        addInfoRow(patientTable, "Date debut :",
            consultation.getDateHeureDebut() != null
                ? consultation.getDateHeureDebut().format(FMT) : "-", fontBold, fontNormal);
        if (consultation.getDateHeureFin() != null) {
            addInfoRow(patientTable, "Date fin :",
                consultation.getDateHeureFin().format(FMT), fontBold, fontNormal);
        }
        doc.add(patientTable);
        doc.add(new Chunk(grayLine));
        doc.add(new Paragraph(" "));

        // ── DIAGNOSTIC ────────────────────────────────────────────────────
        Paragraph diagTitle = new Paragraph("Diagnostic", fontSection);
        diagTitle.setSpacingAfter(4);
        doc.add(diagTitle);

        // Code ICD-10 si disponible
        if (consultation.getIcdCode() != null && !consultation.getIcdCode().isBlank()) {
            PdfPTable icdTable = new PdfPTable(1);
            icdTable.setWidthPercentage(100);
            icdTable.setSpacingAfter(6);
            PdfPCell icdCell = new PdfPCell();
            icdCell.setBackgroundColor(COLOR_LIGHT);
            icdCell.setBorderColor(COLOR_PRIMARY);
            icdCell.setPadding(8);
            icdCell.addElement(new Paragraph(
                "Code CIM-10 : " + consultation.getIcdCode(), fontGreen));
            icdTable.addCell(icdCell);
            doc.add(icdTable);
        }

        if (consultation.getDiagnostic() != null && !consultation.getDiagnostic().isBlank()) {
            Paragraph diagText = new Paragraph(consultation.getDiagnostic(), fontNormal);
            diagText.setIndentationLeft(10);
            diagText.setSpacingAfter(10);
            doc.add(diagText);
        }

        // ── ORIENTATION ───────────────────────────────────────────────────
        if (consultation.getOrientation() != null) {
            Paragraph orientP = new Paragraph();
            orientP.add(new Chunk("Orientation : ", fontBold));
            orientP.add(new Chunk(consultation.getOrientation(), fontNormal));
            orientP.setSpacingAfter(10);
            doc.add(orientP);
        }

        doc.add(new Chunk(grayLine));
        doc.add(new Paragraph(" "));

        // ── PRESCRIPTIONS ─────────────────────────────────────────────────
        Paragraph prescTitle = new Paragraph("Prescriptions Medicamenteuses", fontSection);
        prescTitle.setSpacingAfter(6);
        doc.add(prescTitle);

        if (ordonnances == null || ordonnances.isEmpty()) {
            doc.add(new Paragraph("Aucune prescription.", fontNormal));
        } else {
            PdfPTable medTable = new PdfPTable(3);
            medTable.setWidthPercentage(100);
            medTable.setWidths(new float[]{2.5f, 2.5f, 1f});
            medTable.setSpacingAfter(10);

            // En-tete tableau
            for (String h : new String[]{"Medicament", "Posologie", "Duree"}) {
                PdfPCell hCell = new PdfPCell(new Phrase(h, fontWhite));
                hCell.setBackgroundColor(COLOR_PRIMARY);
                hCell.setPadding(7);
                hCell.setBorderColor(COLOR_PRIMARY);
                medTable.addCell(hCell);
            }

            boolean alt = false;
            for (Ordonnance o : ordonnances) {
                BaseColor bg = alt ? new BaseColor(240, 246, 255) : BaseColor.WHITE;
                addMedCell(medTable, nvl(o.getMedicament()), fontNormal, bg);
                addMedCell(medTable, nvl(o.getPosologie()),  fontNormal, bg);
                addMedCell(medTable, o.getDureeJours() + " j", fontNormal, bg);
                alt = !alt;
            }
            doc.add(medTable);
        }

        // ── DEMANDES LABO / IMAGERIE ──────────────────────────────────────
        boolean hasAnalyses  = consultation.getAnalyses()  != null && !consultation.getAnalyses().isBlank();
        boolean hasImageries = consultation.getImageries() != null && !consultation.getImageries().isBlank();

        if (hasAnalyses || hasImageries) {
            doc.add(new Chunk(grayLine));
            doc.add(new Paragraph(" "));

            Paragraph laboTitle = new Paragraph("Demandes Laboratoire / Imagerie", fontSection);
            laboTitle.setSpacingAfter(6);
            doc.add(laboTitle);

            // Badge statut
            String statut = "Envoyee".equals(consultation.getStatutDemande())
                ? "Demande envoyee au laboratoire/radiologue" : "Demande non envoyee";
            Paragraph statutP = new Paragraph(statut,
                "Envoyee".equals(consultation.getStatutDemande()) ? fontGreen : fontRed);
            statutP.setSpacingAfter(8);
            doc.add(statutP);

            if (hasAnalyses) {
                Paragraph aTitle = new Paragraph("Analyses biologiques :", fontBold);
                aTitle.setSpacingAfter(2);
                doc.add(aTitle);
                Paragraph aText = new Paragraph(consultation.getAnalyses(), fontNormal);
                aText.setIndentationLeft(10);
                aText.setSpacingAfter(8);
                doc.add(aText);
            }
            if (hasImageries) {
                Paragraph iTitle = new Paragraph("Imageries medicales :", fontBold);
                iTitle.setSpacingAfter(2);
                doc.add(iTitle);
                Paragraph iText = new Paragraph(consultation.getImageries(), fontNormal);
                iText.setIndentationLeft(10);
                iText.setSpacingAfter(8);
                doc.add(iText);
            }
        }

        // ── SIGNATURE ─────────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new Chunk(tealLine));
        doc.add(new Paragraph(" "));

        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[]{1, 1});

        PdfPCell sigLeft = new PdfPCell();
        sigLeft.setBorder(Rectangle.NO_BORDER);
        sigLeft.addElement(new Paragraph("Signature du medecin :", fontBold));
        sigLeft.addElement(new Paragraph(" "));
        sigLeft.addElement(new Paragraph("_______________________", fontNormal));
        sigLeft.addElement(new Paragraph("Dr. " + nvl(consultation.getNomMedecin()), fontBold));
        sigTable.addCell(sigLeft);

        PdfPCell sigRight = new PdfPCell();
        sigRight.setBorder(Rectangle.NO_BORDER);
        sigRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph cachet = new Paragraph("Cachet de l'etablissement :", fontBold);
        cachet.setAlignment(Element.ALIGN_RIGHT);
        sigRight.addElement(cachet);
        sigRight.addElement(new Paragraph(" "));
        Paragraph cachetBox = new Paragraph("[ REVIVE — Service Urgences ]", fontNormal);
        cachetBox.setAlignment(Element.ALIGN_RIGHT);
        sigRight.addElement(cachetBox);
        sigTable.addCell(sigRight);
        doc.add(sigTable);

        // ── PIED DE PAGE ──────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        Paragraph footer = new Paragraph(
            "Document genere le " + LocalDateTime.now().format(FMT)
            + "  |  REVIVE — Systeme de Gestion des Urgences  |  Module 3", fontSmall);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
    }

    private void addInfoRow(PdfPTable table, String label, String value,
                             Font fontLabel, Font fontValue) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, fontLabel));
        c1.setBorder(Rectangle.NO_BORDER); c1.setPadding(3);
        PdfPCell c2 = new PdfPCell(new Phrase(value, fontValue));
        c2.setBorder(Rectangle.NO_BORDER); c2.setPadding(3);
        table.addCell(c1); table.addCell(c2);
    }

    private void addMedCell(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        cell.setBorderColor(new BaseColor(220, 228, 240));
        table.addCell(cell);
    }

    private String nvl(String s) { return s != null ? s : "—"; }
}

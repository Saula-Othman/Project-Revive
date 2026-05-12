package pro.revive.utils.UtilsLabo;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import pro.revive.entities.EntitiesLabo.Resultats;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Générateur de rapports PDF premium pour REVIVE — Labo & Imagerie.
 * Design : bannière dégradée, logo, sections colorées, watermark, footer.
 */
public class PdfGenerator {

    // ── Palette REVIVE
    private static final BaseColor BLEU_DARK    = new BaseColor(11,  78,  162);
    private static final BaseColor BLEU_MID     = new BaseColor(29, 111, 216);
    private static final BaseColor TEAL         = new BaseColor(14, 155, 138);
    private static final BaseColor ROUGE_GRAVE  = new BaseColor(220,  38,  38);
    private static final BaseColor ROUGE_LIGHT  = new BaseColor(254, 226, 226);
    private static final BaseColor VERT_PROPRE  = new BaseColor(22,  163,  74);
    private static final BaseColor VERT_LIGHT   = new BaseColor(220, 252, 231);
    private static final BaseColor GRIS_BG      = new BaseColor(241, 245, 249);
    private static final BaseColor GRIS_TEXTE   = new BaseColor( 71,  85, 105);
    private static final BaseColor GRIS_BORDER  = new BaseColor(203, 213, 225);
    private static final BaseColor BLANC        = BaseColor.WHITE;
    private static final BaseColor BLEU_LIGHT   = new BaseColor(219, 234, 254);
    private static final BaseColor ORANGE_WARN  = new BaseColor(234,  88,  12);

    // ── Fonts
    private static Font fontH1(BaseColor c)  { return new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   c); }
    private static Font fontH2(BaseColor c)  { return new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD,   c); }
    private static Font fontH3(BaseColor c)  { return new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,   c); }
    private static Font fontBody(BaseColor c){ return new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, c); }
    private static Font fontSm(BaseColor c)  { return new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, c); }
    private static Font fontSmI(BaseColor c) { return new Font(Font.FontFamily.HELVETICA,  9, Font.ITALIC, c); }
    private static Font fontBadge(BaseColor c){ return new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD,  c); }

    public static void genererRapport(Resultats r, String cheminFichier) throws Exception {
        Document doc = new Document(PageSize.A4, 45, 45, 50, 70);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(cheminFichier));

        // ── Event : footer sur chaque page
        writer.setPageEvent(new FooterEvent());

        doc.open();

        String patient = (r.getNomPatient() != null && !r.getNomPatient().isBlank())
                ? r.getNomPatient() : "Demande #" + r.getIdDemande();
        boolean grave = "Grave".equalsIgnoreCase(r.getEtat());

        // ══════════════════════════════════════════════
        // 1. BANNIÈRE HEADER avec logo
        // ══════════════════════════════════════════════
        ajouterBanniere(doc, writer);

        // ══════════════════════════════════════════════
        // 2. TITRE DU RAPPORT
        // ══════════════════════════════════════════════
        ajouterTitreRapport(doc, patient, grave);

        espaceur(doc, 6);

        // ══════════════════════════════════════════════
        // 3. BADGE ÉTAT (PROPRE / GRAVE)
        // ══════════════════════════════════════════════
        ajouterBadgeEtat(doc, grave);

        espaceur(doc, 10);

        // ══════════════════════════════════════════════
        // 4. SECTION INFORMATIONS PATIENT
        // ══════════════════════════════════════════════
        ajouterSectionTitre(doc, "  INFORMATIONS DU PATIENT", BLEU_DARK, BLEU_LIGHT);
        espaceur(doc, 4);
        ajouterTableInfos(doc, r, patient, grave);

        espaceur(doc, 12);

        // ══════════════════════════════════════════════
        // 5. SECTION COMPTE RENDU
        // ══════════════════════════════════════════════
        ajouterSectionTitre(doc, "  COMPTE RENDU MEDICAL", BLEU_DARK, BLEU_LIGHT);
        espaceur(doc, 4);
        ajouterCompteRendu(doc, r, grave);

        espaceur(doc, 12);

        // ══════════════════════════════════════════════
        // 6. SECTION IMAGE RADIO (si fichier image valide)
        // ══════════════════════════════════════════════
        if (r.getFichierJoint() != null && !r.getFichierJoint().isBlank()) {
            ajouterSectionTitre(doc, "  IMAGE RADIOLOGIQUE", TEAL, new BaseColor(204, 251, 241));
            espaceur(doc, 4);
            ajouterImageRadio(doc, r, grave);
            espaceur(doc, 12);
        }

        // ══════════════════════════════════════════════
        // 7. SECTION RECOMMANDATIONS
        // ══════════════════════════════════════════════
        ajouterSectionTitre(doc, "  RECOMMANDATIONS", grave ? ROUGE_GRAVE : VERT_PROPRE,
                grave ? ROUGE_LIGHT : VERT_LIGHT);
        espaceur(doc, 4);
        ajouterRecommandations(doc, grave);

        espaceur(doc, 16);

        // ══════════════════════════════════════════════
        // 8. SIGNATURE / VALIDATION
        // ══════════════════════════════════════════════
        ajouterSignature(doc);

        doc.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BANNIÈRE HEADER
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterBanniere(Document doc, PdfWriter writer) throws Exception {
        PdfContentByte canvas = writer.getDirectContentUnder();

        // Fond dégradé bleu → teal (simulé avec rectangle)
        float pageW = doc.getPageSize().getWidth();
        float top   = doc.getPageSize().getHeight() - doc.topMargin() + 30;

        // Rectangle bleu foncé
        canvas.setColorFill(BLEU_DARK);
        canvas.rectangle(doc.leftMargin() - 45, top - 90, pageW, 90);
        canvas.fill();

        // Bande teal en bas de la bannière
        canvas.setColorFill(TEAL);
        canvas.rectangle(doc.leftMargin() - 45, top - 94, pageW, 4);
        canvas.fill();

        // Ligne lumineuse fine
        canvas.setColorFill(new BaseColor(96, 165, 250, 120));
        canvas.rectangle(doc.leftMargin() - 45, top - 2, pageW, 2);
        canvas.fill();

        // ── Logo
        try {
            URL logoUrl = PdfGenerator.class.getResource("/ResourcesLabo/Images/logo_revive.png");
            if (logoUrl != null) {
                Image logo = Image.getInstance(logoUrl);
                logo.scaleToFit(130, 70);
                logo.setAbsolutePosition(doc.leftMargin() - 30, top - 82);
                doc.add(logo);
            }
        } catch (Exception ignored) {}

        // ── Texte dans la bannière
        PdfContentByte cb = writer.getDirectContent();

        // "REVIVE" badge
        cb.setColorFill(new BaseColor(147, 197, 253));
        cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 9);
        cb.beginText();
        cb.setTextMatrix(doc.leftMargin() + 145, top - 28);
        cb.showText("REVIVE  |  LABORATOIRE & IMAGERIE");
        cb.endText();

        // Titre principal
        cb.setColorFill(BLANC);
        cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false), 20);
        cb.beginText();
        cb.setTextMatrix(doc.leftMargin() + 145, top - 52);
        cb.showText("Rapport d'Examen Medical");
        cb.endText();

        // Sous-titre
        cb.setColorFill(new BaseColor(186, 230, 253));
        cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false), 10);
        cb.beginText();
        cb.setTextMatrix(doc.leftMargin() + 145, top - 70);
        cb.showText("Service des Urgences  |  Biologiste");
        cb.endText();

        // Date en haut à droite
        cb.setColorFill(new BaseColor(186, 230, 253));
        cb.setFontAndSize(BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false), 9);
        cb.beginText();
        cb.setTextMatrix(pageW - 160, top - 28);
        cb.showText(new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        cb.endText();

        // Espace après la bannière
        espaceur(doc, 14);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TITRE DU RAPPORT
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterTitreRapport(Document doc, String patient, boolean grave) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        Paragraph p = new Paragraph();
        p.add(new Chunk("Rapport pour : ", fontH3(GRIS_TEXTE)));
        p.add(new Chunk(patient, fontH2(BLEU_DARK)));

        PdfPCell cell = new PdfPCell(p);
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColorLeft(grave ? ROUGE_GRAVE : TEAL);
        cell.setBorderWidthLeft(4f);
        cell.setBackgroundColor(GRIS_BG);
        cell.setPadding(12);
        t.addCell(cell);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BADGE ÉTAT
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterBadgeEtat(Document doc, boolean grave) throws DocumentException {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(60);
        t.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.setWidths(new float[]{1f, 3f});

        // Icône
        PdfPCell icone = new PdfPCell(new Phrase(grave ? "CRITIQUE" : "NORMAL",
                new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BLANC)));
        icone.setBackgroundColor(grave ? ROUGE_GRAVE : VERT_PROPRE);
        icone.setPadding(7);
        icone.setBorder(Rectangle.NO_BORDER);
        icone.setHorizontalAlignment(Element.ALIGN_CENTER);
        icone.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Texte état
        String etatTxt = grave
                ? "Resultat GRAVE — Attention medicale requise"
                : "Resultat PROPRE — Dans les normes";
        PdfPCell texte = new PdfPCell(new Phrase(etatTxt,
                fontBadge(grave ? ROUGE_GRAVE : VERT_PROPRE)));
        texte.setBackgroundColor(grave ? ROUGE_LIGHT : VERT_LIGHT);
        texte.setPadding(7);
        texte.setBorder(Rectangle.NO_BORDER);
        texte.setVerticalAlignment(Element.ALIGN_MIDDLE);

        t.addCell(icone);
        t.addCell(texte);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TITRE DE SECTION
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterSectionTitre(Document doc, String titre,
                                             BaseColor couleur, BaseColor bg) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(titre, fontH3(couleur)));
        cell.setBackgroundColor(bg);
        cell.setPadding(8);
        cell.setBorder(Rectangle.LEFT | Rectangle.BOTTOM);
        cell.setBorderColorLeft(couleur);
        cell.setBorderColorBottom(couleur);
        cell.setBorderWidthLeft(3f);
        cell.setBorderWidthBottom(0.5f);
        t.addCell(cell);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TABLE INFORMATIONS PATIENT
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterTableInfos(Document doc, Resultats r,
                                           String patient, boolean grave) throws DocumentException {
        PdfPTable t = new PdfPTable(4);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.2f, 2f, 1.2f, 2f});

        // Ligne 1
        ajouterCelluleInfo(t, "Patient", patient, false);
        ajouterCelluleInfo(t, "ID Resultat", "#" + r.getIdResultat(), false);

        // Ligne 2
        String dateStr = r.getDateResultat() != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getDateResultat()) : "—";
        ajouterCelluleInfo(t, "Date", dateStr, true);
        ajouterCelluleInfo(t, "ID Demande", "#" + r.getIdDemande(), true);

        // Ligne 3 : état sur toute la largeur
        PdfPCell labelEtat = new PdfPCell(new Phrase("Etat du resultat", fontH3(GRIS_TEXTE)));
        labelEtat.setBackgroundColor(GRIS_BG);
        labelEtat.setPadding(9);
        labelEtat.setBorderColor(GRIS_BORDER);
        labelEtat.setColspan(1);

        BaseColor etatBg  = grave ? ROUGE_LIGHT : VERT_LIGHT;
        BaseColor etatFg  = grave ? ROUGE_GRAVE : VERT_PROPRE;
        String    etatTxt = grave ? "GRAVE — Anormal" : "PROPRE — Normal";
        PdfPCell valEtat = new PdfPCell(new Phrase(etatTxt, fontBadge(etatFg)));
        valEtat.setBackgroundColor(etatBg);
        valEtat.setPadding(9);
        valEtat.setBorderColor(GRIS_BORDER);
        valEtat.setColspan(3);

        t.addCell(labelEtat);
        t.addCell(valEtat);

        doc.add(t);
    }

    private static void ajouterCelluleInfo(PdfPTable t, String label,
                                            String valeur, boolean altRow) {
        BaseColor bg = altRow ? new BaseColor(248, 250, 252) : BLANC;

        PdfPCell lbl = new PdfPCell(new Phrase(label, fontH3(GRIS_TEXTE)));
        lbl.setBackgroundColor(GRIS_BG);
        lbl.setPadding(9);
        lbl.setBorderColor(GRIS_BORDER);

        PdfPCell val = new PdfPCell(new Phrase(valeur, fontBody(new BaseColor(15, 23, 42))));
        val.setBackgroundColor(bg);
        val.setPadding(9);
        val.setBorderColor(GRIS_BORDER);

        t.addCell(lbl);
        t.addCell(val);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPTE RENDU
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterCompteRendu(Document doc, Resultats r, boolean grave) throws DocumentException {
        String contenu = (r.getCompteRendu() != null && !r.getCompteRendu().isBlank())
                ? r.getCompteRendu() : "Aucun compte rendu disponible.";

        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        // Guillemets décoratifs + contenu
        Paragraph p = new Paragraph();
        p.add(new Chunk("\"  ", new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD,
                grave ? new BaseColor(252, 165, 165) : new BaseColor(134, 239, 172))));
        p.add(new Chunk(contenu, fontBody(GRIS_TEXTE)));
        p.add(new Chunk("  \"", new Font(Font.FontFamily.HELVETICA, 28, Font.BOLD,
                grave ? new BaseColor(252, 165, 165) : new BaseColor(134, 239, 172))));
        p.setLeading(16f);

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(grave ? new BaseColor(255, 248, 248) : new BaseColor(248, 255, 252));
        cell.setPadding(16);
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColorLeft(grave ? ROUGE_GRAVE : VERT_PROPRE);
        cell.setBorderWidthLeft(4f);
        t.addCell(cell);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMAGE RADIO
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterImageRadio(Document doc, Resultats r, boolean grave) throws Exception {
        String chemin = r.getFichierJoint();
        File imgFile  = new File(chemin);

        // ── Cas 1 : fichier image existant sur disque → on l'intègre dans le PDF
        if (imgFile.exists()) {
            try {
                Image img = Image.getInstance(imgFile.getAbsolutePath());

                // Cadre sombre style radiologie
                PdfPTable cadre = new PdfPTable(1);
                cadre.setWidthPercentage(100);

                // Cellule fond noir pour l'image radio
                PdfPCell cellImg = new PdfPCell();
                cellImg.setBackgroundColor(new BaseColor(15, 23, 42));   // #0F172A
                cellImg.setBorder(Rectangle.NO_BORDER);
                cellImg.setPadding(8);
                cellImg.setHorizontalAlignment(Element.ALIGN_CENTER);

                // Redimensionner l'image pour tenir dans la page
                float maxW = doc.getPageSize().getWidth() - doc.leftMargin() - doc.rightMargin() - 16;
                float maxH = 280f;
                img.scaleToFit(maxW, maxH);
                img.setAlignment(Image.ALIGN_CENTER);

                cellImg.addElement(img);
                cadre.addCell(cellImg);

                // Cellule légende sous l'image
                String etatLabel = grave ? "ANORMAL — Resultat GRAVE" : "NORMAL — Resultat PROPRE";
                BaseColor etatFg = grave ? ROUGE_GRAVE : VERT_PROPRE;
                BaseColor etatBg = grave ? ROUGE_LIGHT : VERT_LIGHT;

                PdfPCell cellLegende = new PdfPCell();
                cellLegende.setBackgroundColor(etatBg);
                cellLegende.setBorder(Rectangle.NO_BORDER);
                cellLegende.setPadding(10);

                Paragraph legende = new Paragraph();
                legende.add(new Chunk("Analyse IA : ", fontH3(GRIS_TEXTE)));
                legende.add(new Chunk(etatLabel, fontBadge(etatFg)));
                legende.add(new Chunk("\nFichier : " + imgFile.getName(), fontSm(GRIS_TEXTE)));
                cellLegende.addElement(legende);
                cadre.addCell(cellLegende);

                doc.add(cadre);
                return;

            } catch (Exception e) {
                // Si l'image ne peut pas être lue → fallback chemin texte
            }
        }

        // ── Cas 2 : chemin invalide ou URL → afficher le chemin en texte
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{0.5f, 4f});

        PdfPCell icone = new PdfPCell(new Phrase("IMG", fontH3(BLANC)));
        icone.setBackgroundColor(TEAL);
        icone.setPadding(10);
        icone.setBorder(Rectangle.NO_BORDER);
        icone.setHorizontalAlignment(Element.ALIGN_CENTER);
        icone.setVerticalAlignment(Element.ALIGN_MIDDLE);

        PdfPCell lien = new PdfPCell(new Phrase(chemin, fontBody(BLEU_DARK)));
        lien.setBackgroundColor(new BaseColor(240, 253, 250));
        lien.setPadding(10);
        lien.setBorder(Rectangle.NO_BORDER);
        lien.setVerticalAlignment(Element.ALIGN_MIDDLE);

        t.addCell(icone);
        t.addCell(lien);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECOMMANDATIONS
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterRecommandations(Document doc, boolean grave) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        Paragraph p = new Paragraph();
        p.setLeading(18f);

        if (grave) {
            p.add(new Chunk("ATTENTION — Resultat critique\n", fontH3(ROUGE_GRAVE)));
            p.add(new Chunk("  Informer immediatement le medecin traitant\n", fontBody(GRIS_TEXTE)));
            p.add(new Chunk("  Surveillance rapprochee du patient recommandee\n", fontBody(GRIS_TEXTE)));
            p.add(new Chunk("  Envisager des examens complementaires urgents\n", fontBody(GRIS_TEXTE)));
            p.add(new Chunk("  Documenter l'evolution clinique dans le dossier", fontBody(GRIS_TEXTE)));
        } else {
            p.add(new Chunk("Resultat dans les normes\n", fontH3(VERT_PROPRE)));
            p.add(new Chunk("  Aucune action urgente requise\n", fontBody(GRIS_TEXTE)));
            p.add(new Chunk("  Suivi de routine recommande (3 a 6 mois)\n", fontBody(GRIS_TEXTE)));
            p.add(new Chunk("  Maintien du protocole de soins habituel\n", fontBody(GRIS_TEXTE)));
            p.add(new Chunk("  Conserver ce rapport dans le dossier patient", fontBody(GRIS_TEXTE)));
        }

        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(grave ? ROUGE_LIGHT : VERT_LIGHT);
        cell.setPadding(14);
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColorLeft(grave ? ROUGE_GRAVE : VERT_PROPRE);
        cell.setBorderWidthLeft(4f);
        t.addCell(cell);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SIGNATURE
    // ─────────────────────────────────────────────────────────────────────────
    private static void ajouterSignature(Document doc) throws DocumentException {
        // Ligne de séparation
        doc.add(new LineSeparator(0.5f, 100f, GRIS_BORDER, Element.ALIGN_CENTER, -4));
        espaceur(doc, 8);

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);

        // Colonne gauche : cachet REVIVE
        PdfPCell gauche = new PdfPCell();
        gauche.setBorder(Rectangle.NO_BORDER);
        gauche.setPadding(8);

        Paragraph pGauche = new Paragraph();
        pGauche.add(new Chunk("REVIVE\n", fontH2(BLEU_DARK)));
        pGauche.add(new Chunk("Systeme de Gestion des Urgences Medicales\n", fontSm(GRIS_TEXTE)));
        pGauche.add(new Chunk("Laboratoire & Imagerie — Service des Urgences\n", fontSm(GRIS_TEXTE)));
        pGauche.add(new Chunk("Document genere automatiquement", fontSmI(GRIS_TEXTE)));
        gauche.addElement(pGauche);

        // Colonne droite : zone signature biologiste
        PdfPCell droite = new PdfPCell();
        droite.setBorder(Rectangle.NO_BORDER);
        droite.setPadding(8);
        droite.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph pDroite = new Paragraph();
        pDroite.setAlignment(Element.ALIGN_RIGHT);
        pDroite.add(new Chunk("Biologiste Responsable\n\n\n", fontH3(BLEU_DARK)));
        pDroite.add(new Chunk("_______________________\n", fontBody(GRIS_TEXTE)));
        pDroite.add(new Chunk("Signature & Cachet", fontSmI(GRIS_TEXTE)));
        droite.addElement(pDroite);

        t.addCell(gauche);
        t.addCell(droite);
        doc.add(t);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────
    private static void espaceur(Document doc, float hauteur) throws DocumentException {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(hauteur);
        doc.add(p);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FOOTER EVENT (numéro de page + bande colorée)
    // ─────────────────────────────────────────────────────────────────────────
    static class FooterEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float pageW = document.getPageSize().getWidth();
            float bottom = document.bottomMargin() - 40;

            // Bande footer dégradée (simulée)
            cb.setColorFill(BLEU_DARK);
            cb.rectangle(0, bottom, pageW, 28);
            cb.fill();

            cb.setColorFill(TEAL);
            cb.rectangle(0, bottom + 28, pageW, 3);
            cb.fill();

            // Texte footer
            try {
                BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
                cb.setColorFill(new BaseColor(147, 197, 253));
                cb.setFontAndSize(bf, 8);
                cb.beginText();
                cb.setTextMatrix(document.leftMargin(), bottom + 10);
                cb.showText("REVIVE  |  Systeme de Gestion des Urgences Medicales  |  Labo & Imagerie");
                cb.endText();

                // Numéro de page à droite
                cb.setColorFill(new BaseColor(186, 230, 253));
                cb.setFontAndSize(bf, 8);
                cb.beginText();
                cb.setTextMatrix(pageW - 80, bottom + 10);
                cb.showText("Page " + writer.getPageNumber());
                cb.endText();

                // Date à droite
                cb.setColorFill(new BaseColor(148, 163, 184));
                cb.setFontAndSize(bf, 7);
                cb.beginText();
                cb.setTextMatrix(pageW - 160, bottom + 10);
                cb.showText(new SimpleDateFormat("dd/MM/yyyy").format(new Date()) + "  |  ");
                cb.endText();

            } catch (Exception ignored) {}
        }
    }
}

package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.services.ServicesLabo.AnalyseBiologiqueService;
import pro.revive.services.ServicesLabo.ResultatService;
import pro.revive.utils.UtilsLabo.PdfGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public class RapportBiologisteController {

    @FXML private TextField tfRecherche;
    @FXML private VBox containerPatients;
    @FXML private VBox containerRapportDetail;
    @FXML private Button btnTelechargerPdf;
    @FXML private Label lblMessage;

    private final ResultatService service = new ResultatService();
    private List<Resultats> tousResultats;
    private Resultats resultatSelectionne;

    @FXML
    public void initialize() {
        chargerTousResultats();
    }

    private void chargerTousResultats() {
        tousResultats = service.afficher();
        tousResultats.sort((a, b) -> {
            if (a.getDateResultat() != null && b.getDateResultat() != null)
                return b.getDateResultat().compareTo(a.getDateResultat());
            if (a.getDateResultat() != null) return -1;
            if (b.getDateResultat() != null) return  1;
            return 0;
        });
        afficherListePatients(tousResultats);
    }

    private void afficherListePatients(List<Resultats> resultats) {
        containerPatients.getChildren().clear();
        if (resultats.isEmpty()) {
            Label lbl = new Label("Aucun résultat trouvé.");
            lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px; -fx-padding: 16;");
            containerPatients.getChildren().add(lbl);
            return;
        }
        for (Resultats r : resultats) {
            containerPatients.getChildren().add(creerItemPatient(r));
        }
    }

    private HBox creerItemPatient(Resultats r) {
        HBox item = new HBox(10);
        item.setStyle("-fx-padding: 12 16; -fx-cursor: hand;" +
                "-fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-width: 0 0 1 0;");

        boolean grave = "Grave".equalsIgnoreCase(r.getEtat());
        Label icon = new Label(grave ? "🔴" : "🟢");
        icon.setStyle("-fx-font-size: 14px;");

        VBox info = new VBox(2);
        String patient = (r.getNomPatient() != null && !r.getNomPatient().isBlank())
                ? r.getNomPatient() : "Demande #" + r.getIdDemande();
        Label nomLbl = new Label(patient);
        nomLbl.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label dateLbl = new Label(r.getDateResultat() != null
                ? new SimpleDateFormat("dd/MM/yyyy").format(r.getDateResultat()) : "—");
        dateLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
        info.getChildren().addAll(nomLbl, dateLbl);
        item.getChildren().addAll(icon, info);

        item.setOnMouseEntered(e -> item.setStyle(
                "-fx-padding: 12 16; -fx-cursor: hand; -fx-background-color: #EEF4FB;" +
                "-fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-width: 0 0 1 0;"));
        item.setOnMouseExited(e -> item.setStyle(
                "-fx-padding: 12 16; -fx-cursor: hand;" +
                "-fx-border-color: transparent transparent #E2E8F0 transparent; -fx-border-width: 0 0 1 0;"));
        item.setOnMouseClicked(e -> afficherRapportDetail(r));
        return item;
    }

    private void afficherRapportDetail(Resultats r) {
        resultatSelectionne = r;
        btnTelechargerPdf.setDisable(false);
        containerRapportDetail.getChildren().clear();

        String patient = (r.getNomPatient() != null && !r.getNomPatient().isBlank())
                ? r.getNomPatient() : "Patient inconnu";
        boolean grave = "Grave".equalsIgnoreCase(r.getEtat());
        boolean estAnalyse = r.getFichierJoint() == null || r.getFichierJoint().isBlank();

        // ── Wrapper principal
        VBox wrapper = new VBox(0);
        wrapper.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
                "-fx-effect: dropshadow(gaussian,rgba(11,78,162,0.18),20,0,0,6);" +
                "-fx-border-color: rgba(11,78,162,0.12); -fx-border-radius: 16; -fx-border-width: 1;");

        // ── 1. BANNIÈRE
        VBox banniere = new VBox(4);
        banniere.setStyle("-fx-background-color: linear-gradient(to right, #0B4EA2, #1D6FD8, #0E9B8A);" +
                "-fx-background-radius: 16 16 0 0; -fx-padding: 16 20;");
        Label banTitre = new Label("📄  Rapport d'Examen Médical");
        banTitre.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
        Label banSub = new Label("REVIVE  •  Laboratoire & Imagerie  •  Service des Urgences");
        banSub.setStyle("-fx-text-fill: rgba(186,230,253,0.85); -fx-font-size: 10px;");
        banniere.getChildren().addAll(banTitre, banSub);

        javafx.scene.layout.Region accent = new javafx.scene.layout.Region();
        accent.setPrefHeight(3);
        accent.setStyle("-fx-background-color: linear-gradient(to right, #0B4EA2, #0E9B8A, #0B4EA2);");

        // ── 2. BADGE ÉTAT
        HBox badgeBox = new HBox(10);
        badgeBox.setStyle("-fx-padding: 12 20 8 20; -fx-alignment: CENTER_LEFT;" +
                "-fx-background-color: " + (grave ? "#FFF5F5" : "#F0FDF4") + ";");
        Label etatIcon = new Label(grave ? "🔴" : "🟢");
        etatIcon.setStyle("-fx-font-size: 22px;");
        VBox etatInfo = new VBox(2);
        Label etatTitre = new Label(grave ? "RÉSULTAT GRAVE" : "RÉSULTAT PROPRE");
        etatTitre.setStyle("-fx-text-fill: " + (grave ? "#DC2626" : "#16A34A") + "; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label etatSub = new Label(grave ? "Attention médicale requise" : "Dans les normes attendues");
        etatSub.setStyle("-fx-text-fill: " + (grave ? "#EF4444" : "#22C55E") + "; -fx-font-size: 11px;");
        etatInfo.getChildren().addAll(etatTitre, etatSub);
        badgeBox.getChildren().addAll(etatIcon, etatInfo);

        // ── 3. INFOS PATIENT (sans IDs)
        VBox infosBox = creerSectionApercuTitre("👤  Informations du Patient", "#0B4EA2", "#EFF6FF");
        VBox infosGrid = new VBox(0);
        infosGrid.setStyle("-fx-background-color: white; -fx-padding: 0 0 4 0;");
        infosGrid.getChildren().addAll(
            creerLigneInfo("Patient", patient, false),
            creerLigneInfo("Date", r.getDateResultat() != null
                    ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getDateResultat()) : "—", true)
        );
        infosBox.getChildren().add(infosGrid);

        // ── 4. COMPTE RENDU
        VBox crBox = creerSectionApercuTitre("📝  Compte Rendu Médical", "#0B4EA2", "#EFF6FF");
        VBox crContenu = new VBox(0);
        crContenu.setStyle("-fx-background-color: " + (grave ? "#FFF8F8" : "#F8FFFC") + ";" +
                "-fx-padding: 14 16; -fx-border-color: " + (grave ? "#FCA5A5" : "#86EFAC") + "; -fx-border-width: 0 0 0 4;");
        Label guillemets = new Label("❝");
        guillemets.setStyle("-fx-text-fill: " + (grave ? "#FCA5A5" : "#86EFAC") + "; -fx-font-size: 22px;");
        Label crTexte = new Label(r.getCompteRendu() != null ? r.getCompteRendu() : "Aucun compte rendu.");
        crTexte.setWrapText(true);
        crTexte.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px; -fx-line-spacing: 3;");
        crContenu.getChildren().addAll(guillemets, crTexte);
        crBox.getChildren().add(crContenu);

        // Ajouter les sections de base au wrapper
        wrapper.getChildren().addAll(banniere, accent, badgeBox, infosBox, crBox);

        // ── 5. ANALYSE BIOLOGIQUE (uniquement pour les analyses)
        if (estAnalyse && r.getCompteRendu() != null && !r.getCompteRendu().isBlank()) {
            AnalyseBiologiqueService.ResultatAnalyse analyse =
                    AnalyseBiologiqueService.analyser(r.getCompteRendu());

            String couleurBio = AnalyseBiologiqueService.couleurNiveau(analyse.niveauAttention);
            String fondBio    = AnalyseBiologiqueService.couleurFondNiveau(analyse.niveauAttention);
            String emojiBio   = AnalyseBiologiqueService.emojiNiveau(analyse.niveauAttention);

            VBox bioBox = creerSectionApercuTitre("🔬  Analyse Biologique Intelligente", "#0369A1", "#F0F9FF");
            VBox bioContenu = new VBox(8);
            bioContenu.setStyle("-fx-background-color: " + fondBio + "; -fx-padding: 12 16;");

            Label badgeBio = new Label(emojiBio + "  Niveau d'attention : " + analyse.niveauAttention);
            badgeBio.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: " + couleurBio + ";" +
                    "-fx-background-color: white; -fx-padding: 4 12; -fx-background-radius: 20;" +
                    "-fx-border-color: " + couleurBio + "; -fx-border-radius: 20; -fx-border-width: 1;");
            bioContenu.getChildren().add(badgeBio);

            if (analyse.anomalieDetectee) {
                for (AnalyseBiologiqueService.Anomalie a : analyse.anomalies) {
                    HBox ligne = new HBox(10);
                    ligne.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 7 10;" +
                            "-fx-border-color: " + AnalyseBiologiqueService.couleurNiveau(a.niveau) + ";" +
                            "-fx-border-radius: 8; -fx-border-width: 0 0 0 3;");
                    ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label icone = new Label(a.emoji);
                    icone.setStyle("-fx-font-size: 14px;");
                    VBox infoA = new VBox(1);
                    Label nomBio = new Label(a.biomarqueur + " — " + a.statut);
                    nomBio.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: "
                            + AnalyseBiologiqueService.couleurNiveau(a.niveau) + ";");
                    Label signif = new Label(a.signification);
                    signif.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
                    signif.setWrapText(true);
                    infoA.getChildren().addAll(nomBio, signif);
                    HBox.setHgrow(infoA, javafx.scene.layout.Priority.ALWAYS);
                    ligne.getChildren().addAll(icone, infoA);
                    bioContenu.getChildren().add(ligne);
                }
            }

            VBox interpBox = new VBox(4);
            interpBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10 12;");
            Label interpTitre = new Label("📋  Interprétation :");
            interpTitre.setStyle("-fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: bold;");
            Label interpTexte = new Label(analyse.interpretation);
            interpTexte.setWrapText(true);
            interpTexte.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px; -fx-line-spacing: 2;");
            interpBox.getChildren().addAll(interpTitre, interpTexte);
            bioContenu.getChildren().add(interpBox);

            VBox aideBox = new VBox(4);
            aideBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10 12;");
            Label aideTitre = new Label("🩺  Aide à la décision :");
            aideTitre.setStyle("-fx-text-fill: #0369A1; -fx-font-size: 11px; -fx-font-weight: bold;");
            Label aideTexte = new Label(analyse.aideDecision);
            aideTexte.setWrapText(true);
            aideTexte.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px; -fx-line-spacing: 2;");
            aideBox.getChildren().addAll(aideTitre, aideTexte);
            bioContenu.getChildren().add(aideBox);

            bioBox.getChildren().add(bioContenu);
            wrapper.getChildren().add(bioBox);
        }

        // ── 6. IMAGE RADIO / FICHIER JOINT
        VBox fichierBox = creerSectionApercuTitre("🩻  Image Radiologique", "#0E9B8A", "#F0FDFA");
        String fichier = (r.getFichierJoint() != null && !r.getFichierJoint().isBlank())
                ? r.getFichierJoint() : null;

        if (fichier != null) {
            File imgFile = new File(fichier);
            boolean imageAffichee = false;
            if (imgFile.exists()) {
                try {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(
                            imgFile.toURI().toString(), 500, 220, true, true);
                    if (!img.isError()) {
                        StackPane imgContainer = new StackPane();
                        imgContainer.setStyle("-fx-background-color: #0F172A; -fx-padding: 10;");
                        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
                        iv.setFitWidth(500); iv.setFitHeight(220);
                        iv.setPreserveRatio(true); iv.setSmooth(true);
                        imgContainer.getChildren().add(iv);
                        fichierBox.getChildren().add(imgContainer);

                        HBox badgeIA = new HBox(8);
                        badgeIA.setStyle("-fx-padding: 8 16; -fx-alignment: CENTER_LEFT; -fx-background-color: #0F172A;");
                        Label iaIcon = new Label("🤖");
                        iaIcon.setStyle("-fx-font-size: 13px;");
                        Label iaLbl = new Label("Analysée par IA  •  État : " + (grave ? "ANORMAL" : "NORMAL"));
                        iaLbl.setStyle("-fx-text-fill: " + (grave ? "#EF4444" : "#22C55E") + "; -fx-font-size: 11px; -fx-font-weight: bold;");
                        badgeIA.getChildren().addAll(iaIcon, iaLbl);
                        Label cheminLbl = new Label(fichier);
                        cheminLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px;");
                        cheminLbl.setWrapText(true);
                        VBox sousTitre = new VBox(2);
                        sousTitre.setStyle("-fx-padding: 4 16 8 16; -fx-background-color: #F0FDFA;");
                        sousTitre.getChildren().addAll(badgeIA, cheminLbl);
                        fichierBox.getChildren().add(sousTitre);
                        imageAffichee = true;
                    }
                } catch (Exception ignored) {}
            }
            if (!imageAffichee) {
                HBox row = new HBox(10);
                row.setStyle("-fx-padding: 10 16; -fx-alignment: CENTER_LEFT; -fx-background-color: #F0FDFA;");
                Label fichierLbl = new Label("📁  " + fichier);
                fichierLbl.setStyle("-fx-text-fill: #0B4EA2; -fx-font-size: 12px;");
                fichierLbl.setWrapText(true);
                row.getChildren().add(fichierLbl);
                fichierBox.getChildren().add(row);
            }
        } else {
            HBox row = new HBox(10);
            row.setStyle("-fx-padding: 10 16; -fx-alignment: CENTER_LEFT; -fx-background-color: #F0FDFA;");
            Label lbl = new Label("📁  Aucun fichier joint");
            lbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-font-style: italic;");
            row.getChildren().add(lbl);
            fichierBox.getChildren().add(row);
        }

        // ── 7. RECOMMANDATIONS
        VBox recoBox = creerSectionApercuTitre(
                grave ? "⚠️  Recommandations Urgentes" : "✅  Recommandations",
                grave ? "#DC2626" : "#16A34A",
                grave ? "#FEE2E2" : "#DCFCE7");
        VBox recoContenu = new VBox(4);
        recoContenu.setStyle("-fx-padding: 12 16; -fx-background-color: " + (grave ? "#FFF5F5" : "#F0FDF4") + ";");
        String[] recos = grave
                ? new String[]{"🚨  Informer immédiatement le médecin traitant",
                               "👁  Surveillance rapprochée du patient",
                               "🔬  Envisager des examens complémentaires urgents",
                               "📋  Documenter l'évolution clinique"}
                : new String[]{"✔  Aucune action urgente requise",
                               "📅  Suivi de routine recommandé (3 à 6 mois)",
                               "📋  Maintien du protocole de soins habituel",
                               "🗂  Conserver ce rapport dans le dossier patient"};
        for (String reco : recos) {
            Label l = new Label(reco);
            l.setStyle("-fx-text-fill: " + (grave ? "#7F1D1D" : "#14532D") + "; -fx-font-size: 11px;");
            recoContenu.getChildren().add(l);
        }
        recoBox.getChildren().add(recoContenu);

        // ── 8. FOOTER
        HBox footer = new HBox();
        footer.setStyle("-fx-background-color: linear-gradient(to right, #0B4EA2, #0E9B8A);" +
                "-fx-background-radius: 0 0 16 16; -fx-padding: 10 16;");
        Label footerLbl = new Label("REVIVE  •  Système de Gestion des Urgences  •  © 2025");
        footerLbl.setStyle("-fx-text-fill: rgba(186,230,253,0.80); -fx-font-size: 10px;");
        footer.getChildren().add(footerLbl);

        wrapper.getChildren().addAll(fichierBox, recoBox, footer);
        containerRapportDetail.getChildren().add(wrapper);
    }

    private VBox creerSectionApercuTitre(String titre, String couleur, String bg) {
        VBox box = new VBox(0);
        HBox header = new HBox(8);
        header.setStyle("-fx-background-color: " + bg + "; -fx-padding: 10 16;" +
                "-fx-border-color: " + couleur + "; -fx-border-width: 0 0 0 3;");
        Label lbl = new Label(titre);
        lbl.setStyle("-fx-text-fill: " + couleur + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        header.getChildren().add(lbl);
        box.getChildren().add(header);
        return box;
    }

    private HBox creerLigneInfo(String label, String valeur, boolean alt) {
        HBox row = new HBox(0);
        row.setStyle("-fx-background-color: " + (alt ? "#F8FAFC" : "white") + ";");
        Label lbl = new Label(label);
        lbl.setMinWidth(110);
        lbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-padding: 8 12; -fx-background-color: " + (alt ? "#F1F5F9" : "#F8FAFC") + ";");
        Label val = new Label(valeur);
        val.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 11px; -fx-padding: 8 12;");
        val.setWrapText(true);
        HBox.setHgrow(val, javafx.scene.layout.Priority.ALWAYS);
        row.getChildren().addAll(lbl, val);
        return row;
    }

    @FXML
    private void handleRecherche() {
        String terme = tfRecherche.getText().trim().toLowerCase();
        if (terme.isEmpty()) { afficherListePatients(tousResultats); return; }
        List<Resultats> filtres = tousResultats.stream()
                .filter(r -> r.getNomPatient() != null && r.getNomPatient().toLowerCase().contains(terme))
                .collect(Collectors.toList());
        afficherListePatients(filtres);
        lblMessage.setText(filtres.size() + " résultat(s) trouvé(s)");
    }

    @FXML
    private void handleAfficherTous() {
        tfRecherche.clear();
        chargerTousResultats();
        lblMessage.setText("");
    }

    @FXML
    private void handleTelechargerPdf() {
        if (resultatSelectionne == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String patient = (resultatSelectionne.getNomPatient() != null && !resultatSelectionne.getNomPatient().isBlank())
                ? resultatSelectionne.getNomPatient().replace(" ", "_") : "rapport";
        fc.setInitialFileName("rapport_" + patient + ".pdf");
        Stage stage = (Stage) btnTelechargerPdf.getScene().getWindow();
        File fichier = fc.showSaveDialog(stage);
        if (fichier != null) {
            try {
                PdfGenerator.genererRapport(resultatSelectionne, fichier.getAbsolutePath());
                lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                lblMessage.setText("✅  PDF généré : " + fichier.getName());
            } catch (Exception e) {
                lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                lblMessage.setText("❌  Erreur génération PDF : " + e.getMessage());
            }
        }
    }

    @FXML private void showDashboard()  { naviguerVers("/ResourcesLabo/DashboardLabo.fxml"); }
    @FXML private void showExamens()    { naviguerVers("/ResourcesLabo/GestionExamens.fxml"); }
    @FXML private void showResultats()  { naviguerVers("/ResourcesLabo/GestionResultats.fxml"); }
    @FXML private void showRapport()    { chargerTousResultats(); }

    private void naviguerVers(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) tfRecherche.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) { btn.setScaleX(1.15); btn.setScaleY(1.15); }
    }
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) { btn.setScaleX(1.0); btn.setScaleY(1.0); }
    }
    public void onSidebarHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;" +
                "-fx-font-size: 13px; -fx-padding: 13 30; -fx-alignment: CENTER_LEFT;" +
                "-fx-cursor: hand; -fx-background-radius: 10;" +
                "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 10; -fx-border-width: 1;");
            btn.setTranslateX(4);
        }
    }
    public void onSidebarHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.72);" +
                "-fx-font-size: 13px; -fx-padding: 13 30; -fx-alignment: CENTER_LEFT;" +
                "-fx-cursor: hand; -fx-background-radius: 10;");
            btn.setTranslateX(0);
        }
    }
}

package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.services.ServicesLabo.AnalyseBiologiqueService;
import pro.revive.services.ServicesLabo.Examens_demandesService;
import pro.revive.services.ServicesLabo.GraviteService;
import pro.revive.services.ServicesLabo.RadioAIService;
import pro.revive.services.ServicesLabo.ResultatService;
import pro.revive.services.ServicesLabo.EmailService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.util.*;

public class AjouterResultatController {

    // ── Champs existants
    @FXML private ComboBox<String> cbIdDemande;
    @FXML private TextArea         taCompteRendu;
    @FXML private TextField        tfFichierJoint;
    @FXML private ComboBox<String> cbEtat;
    @FXML private Label            lblMessage;
    @FXML private Label            lblErrIdDemande;
    @FXML private Label            lblErrCompteRendu;

    // ── Champs IA Radio Thoracique
    @FXML private Button           btnParcourir;
    @FXML private Button           btnAnalyserIA;
    @FXML private ImageView        imgApercu;
    @FXML private StackPane        panneauApercu;
    @FXML private VBox             panneauResultatIA;
    @FXML private HBox             panneauChargement;
    @FXML private Label            lblIAIcone;
    @FXML private Label            lblIAEtat;
    @FXML private Label            lblIAConfiance;
    @FXML private Label            lblIAMessage;
    @FXML private Label            lblIAPct;
    @FXML private Region           barreConfiance;

    // ── Champs Score de Gravité
    @FXML private VBox             panneauScore;
    @FXML private Label            lblScoreNum;
    @FXML private Label            lblNiveauGravite;
    @FXML private Label            lblRecommandation;
    @FXML private Region           barreScore;

    // ── Panneau image simple (autres imageries — sans IA)
    @FXML private VBox             panneauImageSimple;
    @FXML private TextField        tfFichierJointSimple;
    @FXML private Button           btnParcourirSimple;
    @FXML private StackPane        panneauApercuSimple;
    @FXML private ImageView        imgApercuSimple;
    private File                   imageSimpleSélectionnée = null;

    private final ResultatService          service             = new ResultatService();
    private final Examens_demandesService  examenService       = new Examens_demandesService();
    private final List<Integer>            idsDemande          = new ArrayList<>();
    private final List<Examens_demandes>   examensDemandes     = new ArrayList<>();
    private File                           imageSélectionnée   = null;
    private String                         predictionIA        = null; // "NORMAL" ou "PNEUMONIA"
    private double                         confianceIA         = 0.0;

    // ── Référence au panneau IA complet (Radio Thoracique seulement)
    @FXML private VBox panneauIA;

    // ── Champs Analyse Biologique Intelligente
    @FXML private VBox   panneauAnalyseBio;
    @FXML private Label  lblNiveauBio;
    @FXML private VBox   containerAnomalies;
    @FXML private Label  lblInterpretationBio;
    @FXML private Label  lblAideDecisionBio;

    // ─────────────────────────────────────────────────────────────────────────
    // INITIALISATION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        chargerDemandesRealisees();

        cbEtat.getItems().addAll("Propre", "Grave");
        cbEtat.setValue("Propre");

        lblErrIdDemande.setText("");
        lblErrCompteRendu.setText("");

        // Validation compte rendu en temps réel + analyse biologique
        taCompteRendu.textProperty().addListener((obs, oldVal, newVal) -> {
            validerCompteRendu(newVal);
            // Analyse biologique automatique si c'est une ANALYSE (pas imagerie)
            int idx = cbIdDemande.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < cbIdDemande.getItems().size()) {
                String label = cbIdDemande.getItems().get(idx);
                boolean estAnalyse = label != null && label.toUpperCase().contains("ANALYSE");
                if (estAnalyse && newVal != null && newVal.trim().length() > 10) {
                    lancerAnalyseBiologique(newVal);
                } else if (!estAnalyse) {
                    masquerAnalyseBiologique();
                }
            }
        });

        // Afficher/masquer le panneau IA selon le type d'examen sélectionné
        cbIdDemande.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> {
                    mettreAJourPanneauIA(newIdx.intValue());
                    // Relancer l'analyse bio si compte rendu déjà saisi
                    String cr = taCompteRendu.getText();
                    if (cr != null && cr.trim().length() > 10) {
                        String label = newIdx.intValue() >= 0 && newIdx.intValue() < cbIdDemande.getItems().size()
                                ? cbIdDemande.getItems().get(newIdx.intValue()) : "";
                        if (label.toUpperCase().contains("ANALYSE")) {
                            lancerAnalyseBiologique(cr);
                        } else {
                            masquerAnalyseBiologique();
                        }
                    }
                });
    }

    /**
     * Gère l'affichage des panneaux selon le type d'examen sélectionné :
     * - Radio Thoracique  → panneauIA (avec analyse pneumonie IA)
     * - Autre Imagerie    → panneauImageSimple (import image seulement, sans IA)
     * - Analyse           → aucun panneau image
     */
    private void mettreAJourPanneauIA(int idx) {
        // Masquer les deux panneaux par défaut
        if (panneauIA != null)          { panneauIA.setVisible(false);          panneauIA.setManaged(false); }
        if (panneauImageSimple != null) { panneauImageSimple.setVisible(false); panneauImageSimple.setManaged(false); }

        if (idx < 0 || idx >= cbIdDemande.getItems().size()) return;

        String label = cbIdDemande.getItems().get(idx);
        String labelUp = label != null ? label.toUpperCase() : "";

        boolean estImagerie      = labelUp.contains("IMAGERIE");
        boolean estRadioThorax   = estImagerie &&
                (labelUp.contains("THORACIQUE") || labelUp.contains("THORAX") || labelUp.contains("RADIO"));

        if (estRadioThorax) {
            // Panneau complet avec serveur pneumonie IA
            panneauIA.setVisible(true);
            panneauIA.setManaged(true);
            // Réinitialiser le panneau simple
            if (tfFichierJointSimple != null) tfFichierJointSimple.clear();
            imageSimpleSélectionnée = null;
            if (panneauApercuSimple != null) { panneauApercuSimple.setVisible(false); panneauApercuSimple.setManaged(false); }
        } else if (estImagerie) {
            // Panneau simple — import image uniquement, pas d'IA
            panneauImageSimple.setVisible(true);
            panneauImageSimple.setManaged(true);
            // Réinitialiser le panneau IA
            tfFichierJoint.clear();
            imageSélectionnée = null;
            if (panneauApercu != null) { panneauApercu.setVisible(false); panneauApercu.setManaged(false); }
            masquerResultatIA();
            afficherChargement(false);
        } else {
            // Analyse biologique — aucun panneau image
            tfFichierJoint.clear();
            imageSélectionnée = null;
            if (panneauApercu != null) { panneauApercu.setVisible(false); panneauApercu.setManaged(false); }
            masquerResultatIA();
            afficherChargement(false);
            if (tfFichierJointSimple != null) tfFichierJointSimple.clear();
            imageSimpleSélectionnée = null;
            if (panneauApercuSimple != null) { panneauApercuSimple.setVisible(false); panneauApercuSimple.setManaged(false); }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMPORT IMAGE RADIO
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleParcourirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image radiologique");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images médicales", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.tiff", "*.tif"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) tfFichierJoint.getScene().getWindow();
        File fichier = fc.showOpenDialog(stage);

        if (fichier != null) {
            imageSélectionnée = fichier;
            tfFichierJoint.setText(fichier.getAbsolutePath());

            // Afficher l'aperçu
            try {
                Image img = new Image(fichier.toURI().toString());
                imgApercu.setImage(img);
                panneauApercu.setVisible(true);
                panneauApercu.setManaged(true);
            } catch (Exception e) {
                panneauApercu.setVisible(false);
                panneauApercu.setManaged(false);
            }

            // Masquer l'ancien résultat IA
            masquerResultatIA();

            // Lancer l'analyse automatiquement
            handleAnalyserIA();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // IMPORT IMAGE SIMPLE (autres imageries)
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleParcourirImageSimple() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Sélectionner une image médicale");
        fc.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Images médicales", "*.jpg", "*.jpeg", "*.png", "*.bmp", "*.tiff", "*.tif"),
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
        );

        Stage stage = (Stage) tfFichierJointSimple.getScene().getWindow();
        File fichier = fc.showOpenDialog(stage);

        if (fichier != null) {
            imageSimpleSélectionnée = fichier;
            tfFichierJointSimple.setText(fichier.getAbsolutePath());

            // Afficher l'aperçu
            try {
                Image img = new Image(fichier.toURI().toString());
                imgApercuSimple.setImage(img);
                panneauApercuSimple.setVisible(true);
                panneauApercuSimple.setManaged(true);
            } catch (Exception e) {
                panneauApercuSimple.setVisible(false);
                panneauApercuSimple.setManaged(false);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE IA
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleAnalyserIA() {
        if (imageSélectionnée == null || !imageSélectionnée.exists()) {
            afficherErreur("⚠  Veuillez d'abord importer une image radiologique.");
            return;
        }

        // Afficher le chargement
        afficherChargement(true);
        masquerResultatIA();
        btnAnalyserIA.setDisable(true);
        btnParcourir.setDisable(true);

        // Analyse dans un thread séparé (ne pas bloquer l'UI)
        Thread thread = new Thread(() -> {
            RadioAIService.AnalyseResult result = RadioAIService.analyserImage(imageSélectionnée);

            Platform.runLater(() -> {
                afficherChargement(false);
                btnAnalyserIA.setDisable(false);
                btnParcourir.setDisable(false);

                if (result.success) {
                    afficherResultatIA(result);
                } else {
                    afficherErreurIA(result.erreur);
                }
            });
        });
        thread.setDaemon(true);
        thread.start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFICHAGE RÉSULTAT IA
    // ─────────────────────────────────────────────────────────────────────────

    private void afficherResultatIA(RadioAIService.AnalyseResult result) {
        boolean grave = "Grave".equals(result.etat);

        // Stocker la prédiction pour le calcul du score
        predictionIA = result.label;   // "NORMAL" ou "PNEUMONIA"
        confianceIA  = result.confiance;

        // Icône + état
        lblIAIcone.setText(grave ? "🔴" : "🟢");
        lblIAEtat.setText(grave ? "ANORMAL — Résultat GRAVE" : "NORMAL — Résultat PROPRE");
        lblIAEtat.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: "
                + (grave ? "#DC2626" : "#16A34A") + ";");

        // Confiance
        lblIAConfiance.setText(String.format("Confiance : %.1f%%  |  Probabilité : %.4f",
                result.confiance, result.probabilite));

        // Message
        lblIAMessage.setText(result.message);

        // Pourcentage barre
        lblIAPct.setText(String.format("%.1f%%", result.confiance));
        lblIAPct.setStyle("-fx-text-fill: " + (grave ? "#DC2626" : "#16A34A")
                + "; -fx-font-size: 10px; -fx-font-weight: bold;");

        // Barre de confiance (largeur proportionnelle)
        double pct = Math.min(result.confiance / 100.0, 1.0);
        barreConfiance.setMaxWidth(Double.MAX_VALUE);
        barreConfiance.setPrefWidth(pct * 490); // largeur max approximative
        barreConfiance.setStyle(
                "-fx-background-color: linear-gradient(to right, "
                + (grave ? "#DC2626, #EF4444" : "#16A34A, #22C55E") + ");"
                + "-fx-background-radius: 4;");

        // Style du panneau résultat
        panneauResultatIA.setStyle(
                "-fx-background-color: " + (grave ? "#FFF5F5" : "#F0FDF4") + ";"
                + "-fx-background-radius: 12; -fx-padding: 12 14;"
                + "-fx-border-color: " + (grave ? "#FCA5A5" : "#86EFAC") + ";"
                + "-fx-border-radius: 12; -fx-border-width: 1.5;");

        panneauResultatIA.setVisible(true);
        panneauResultatIA.setManaged(true);

        // ── Remplir automatiquement le champ État
        cbEtat.setValue(result.etat); // "Propre" ou "Grave"
        cbEtat.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;"
                + "-fx-border-color: " + (grave ? "#EF4444" : "#22C55E") + ";"
                + "-fx-font-size: 13px; -fx-font-weight: bold;");

        // Message de succès
        lblMessage.setStyle("-fx-text-fill: " + (grave ? "#DC2626" : "#16A34A")
                + "; -fx-font-weight: bold; -fx-font-size: 12px;");
        lblMessage.setText("🤖  IA : " + (grave ? "Radiographie ANORMALE détectée" : "Radiographie NORMALE détectée")
                + " (" + String.format("%.1f%%", result.confiance) + " de confiance)");
    }

    private void afficherErreurIA(String erreur) {
        panneauResultatIA.setStyle(
                "-fx-background-color: #FFF7ED; -fx-background-radius: 12; -fx-padding: 12 14;"
                + "-fx-border-color: #FED7AA; -fx-border-radius: 12; -fx-border-width: 1.5;");
        panneauResultatIA.setVisible(true);
        panneauResultatIA.setManaged(true);

        lblIAIcone.setText("⚠️");
        lblIAEtat.setText("Serveur IA non démarré");
        lblIAEtat.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #EA580C;");
        lblIAConfiance.setText("Lancez : python app.py");
        lblIAMessage.setText(erreur);
        lblIAPct.setText("—");
        barreConfiance.setPrefWidth(0);
    }

    private void masquerResultatIA() {
        panneauResultatIA.setVisible(false);
        panneauResultatIA.setManaged(false);
    }

    private void afficherChargement(boolean visible) {
        panneauChargement.setVisible(visible);
        panneauChargement.setManaged(visible);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AFFICHAGE SCORE DE GRAVITÉ
    // ─────────────────────────────────────────────────────────────────────────

    private void afficherScore(GraviteService.ScoreResult score) {
        if (panneauScore == null) return;

        // Style du panneau selon le niveau
        String fondClair  = GraviteService.couleurFondClair(score.niveau);
        String couleurBord = GraviteService.couleurFond(score.niveau);
        panneauScore.setStyle(
                "-fx-background-color: " + fondClair + ";" +
                "-fx-background-radius: 16; -fx-padding: 16 18;" +
                "-fx-border-color: " + couleurBord + ";" +
                "-fx-border-radius: 16; -fx-border-width: 1.5;");

        // Score numérique
        lblScoreNum.setText(String.valueOf(score.score));
        lblScoreNum.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: "
                + GraviteService.couleurTexte(score.niveau) + ";");

        // Badge niveau
        lblNiveauGravite.setText(GraviteService.emoji(score.niveau) + "  " + score.niveau);
        lblNiveauGravite.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + GraviteService.couleurFond(score.niveau) + ";" +
                "-fx-background-color: " + fondClair + ";" +
                "-fx-padding: 3 10; -fx-background-radius: 20;" +
                "-fx-border-color: " + GraviteService.couleurFond(score.niveau) + ";" +
                "-fx-border-radius: 20; -fx-border-width: 1;");

        // Barre de progression
        double pct = score.score / 100.0;
        barreScore.setPrefWidth(pct * 490);
        String gradient;
        if (score.score <= 30)      gradient = "linear-gradient(to right, #16A34A, #22C55E)";
        else if (score.score <= 60) gradient = "linear-gradient(to right, #EA580C, #FB923C)";
        else if (score.score <= 80) gradient = "linear-gradient(to right, #DC2626, #EF4444)";
        else                        gradient = "linear-gradient(to right, #7F1D1D, #DC2626)";
        barreScore.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 6;");

        // Recommandation
        lblRecommandation.setText(score.recommandation);

        // Afficher le panneau avec animation
        panneauScore.setVisible(true);
        panneauScore.setManaged(true);
        panneauScore.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(400), panneauScore);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void masquerScore() {
        if (panneauScore != null) {
            panneauScore.setVisible(false);
            panneauScore.setManaged(false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ANALYSE BIOLOGIQUE INTELLIGENTE
    // ─────────────────────────────────────────────────────────────────────────

    /** Debounce timer pour éviter d'analyser à chaque frappe */
    private javafx.animation.PauseTransition debounceAnalyse;

    private void lancerAnalyseBiologique(String compteRendu) {
        // Debounce : attendre 800ms après la dernière frappe
        if (debounceAnalyse != null) debounceAnalyse.stop();
        debounceAnalyse = new javafx.animation.PauseTransition(javafx.util.Duration.millis(800));
        debounceAnalyse.setOnFinished(e -> afficherAnalyseBiologique(compteRendu));
        debounceAnalyse.play();
    }

    private void afficherAnalyseBiologique(String compteRendu) {
        if (panneauAnalyseBio == null) return;

        AnalyseBiologiqueService.ResultatAnalyse resultat =
                AnalyseBiologiqueService.analyser(compteRendu);

        // Badge niveau global
        String couleur = AnalyseBiologiqueService.couleurNiveau(resultat.niveauAttention);
        String fond    = AnalyseBiologiqueService.couleurFondNiveau(resultat.niveauAttention);
        String emoji   = AnalyseBiologiqueService.emojiNiveau(resultat.niveauAttention);

        lblNiveauBio.setText(emoji + "  " + resultat.niveauAttention);
        lblNiveauBio.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-text-fill: " + couleur + ";" +
                "-fx-background-color: " + fond + ";" +
                "-fx-padding: 4 12; -fx-background-radius: 20;" +
                "-fx-border-color: " + couleur + ";" +
                "-fx-border-radius: 20; -fx-border-width: 1;");

        // Style du panneau selon le niveau
        panneauAnalyseBio.setStyle(
                "-fx-background-color: " + fond + "; -fx-background-radius: 16;" +
                "-fx-padding: 16 18;" +
                "-fx-border-color: " + couleur + "; -fx-border-radius: 16; -fx-border-width: 1.5;");

        // Liste des anomalies
        containerAnomalies.getChildren().clear();
        if (resultat.anomalieDetectee) {
            for (AnalyseBiologiqueService.Anomalie a : resultat.anomalies) {
                HBox ligne = new HBox(10);
                ligne.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 10;" +
                        "-fx-padding: 8 12;" +
                        "-fx-border-color: " + AnalyseBiologiqueService.couleurNiveau(a.niveau) + ";" +
                        "-fx-border-radius: 10; -fx-border-width: 0 0 0 3;");
                ligne.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label icone = new Label(a.emoji);
                icone.setStyle("-fx-font-size: 16px;");

                javafx.scene.layout.VBox info = new javafx.scene.layout.VBox(2);
                Label nomBio = new Label(a.biomarqueur + " — " + a.statut);
                nomBio.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: "
                        + AnalyseBiologiqueService.couleurNiveau(a.niveau) + ";");
                Label signif = new Label(a.signification);
                signif.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");
                signif.setWrapText(true);
                info.getChildren().addAll(nomBio, signif);
                HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

                // Badge niveau anomalie
                Label badgeNiv = new Label(a.niveau);
                badgeNiv.setStyle(
                        "-fx-font-size: 9px; -fx-font-weight: bold;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-color: " + AnalyseBiologiqueService.couleurNiveau(a.niveau) + ";" +
                        "-fx-padding: 2 8; -fx-background-radius: 10;");

                ligne.getChildren().addAll(icone, info, badgeNiv);
                containerAnomalies.getChildren().add(ligne);
            }
        }

        // Interprétation
        lblInterpretationBio.setText(resultat.interpretation);

        // Aide à la décision
        lblAideDecisionBio.setText(resultat.aideDecision);

        // Afficher avec animation
        panneauAnalyseBio.setVisible(true);
        panneauAnalyseBio.setManaged(true);
        panneauAnalyseBio.setOpacity(0);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(400), panneauAnalyseBio);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void masquerAnalyseBiologique() {
        if (panneauAnalyseBio != null) {
            panneauAnalyseBio.setVisible(false);
            panneauAnalyseBio.setManaged(false);
        }
    }

    private void chargerDemandesRealisees() {
        LinkedHashMap<Integer, String> map = service.getDemandesRealisees();
        idsDemande.clear();
        cbIdDemande.getItems().clear();
        examensDemandes.clear();

        // Charger aussi les objets Examens_demandes pour le calcul du score
        List<Examens_demandes> tousExamens = examenService.getAllExamens();

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            idsDemande.add(entry.getKey());
            cbIdDemande.getItems().add(entry.getValue());
            // Trouver l'objet Examens_demandes correspondant
            Examens_demandes ed = tousExamens.stream()
                    .filter(e -> e.getIdDemande() == entry.getKey())
                    .findFirst().orElse(null);
            examensDemandes.add(ed);
        }

        if (!cbIdDemande.getItems().isEmpty()) {
            cbIdDemande.getSelectionModel().selectFirst();
            lblErrIdDemande.setText("");
            mettreAJourPanneauIA(0);
        } else {
            lblErrIdDemande.setText("ℹ  Aucun examen réalisé disponible. Marquez d'abord un examen comme « Réalisé ».");
            if (panneauIA != null) { panneauIA.setVisible(false); panneauIA.setManaged(false); }
        }
    }

    @FXML
    private void handleActualiser() {
        chargerDemandesRealisees();
        lblMessage.setStyle("-fx-text-fill: #0B4EA2; -fx-font-weight: bold; -fx-font-size: 12px;");
        lblMessage.setText("✔  Liste actualisée.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private boolean validerCompteRendu(String val) {
        if (val == null || val.trim().isEmpty()) {
            lblErrCompteRendu.setText("⚠  Le compte rendu est obligatoire.");
            mettreEnErreurTA(taCompteRendu);
            return false;
        }
        if (val.trim().matches("\\d+")) {
            lblErrCompteRendu.setText("⚠  Le compte rendu doit être une description textuelle.");
            mettreEnErreurTA(taCompteRendu);
            return false;
        }
        lblErrCompteRendu.setText("");
        retablirTA(taCompteRendu);
        return true;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENREGISTREMENT
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleAjouter() {
        lblMessage.setText("");

        int idx = cbIdDemande.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= idsDemande.size()) {
            lblErrIdDemande.setText("⚠  Veuillez sélectionner un patient / examen réalisé.");
            afficherErreur("Veuillez sélectionner un patient / examen réalisé.");
            return;
        }
        lblErrIdDemande.setText("");

        if (!validerCompteRendu(taCompteRendu.getText())) {
            afficherErreur("Veuillez corriger les erreurs avant de continuer.");
            return;
        }

        try {
            Resultats r = new Resultats();
            r.setIdDemande(idsDemande.get(idx));
            r.setCompteRendu(taCompteRendu.getText().trim());
            // Fichier joint : radio thoracique (tfFichierJoint) ou autre imagerie (tfFichierJointSimple)
            String fichierJoint = tfFichierJoint.getText().trim();
            if (fichierJoint.isEmpty() && tfFichierJointSimple != null) {
                fichierJoint = tfFichierJointSimple.getText().trim();
            }
            r.setFichierJoint(fichierJoint);
            r.setDateResultat(new java.util.Date());
            r.setEtat(cbEtat.getValue() != null ? cbEtat.getValue() : "Propre");

            // ── Calcul du score de gravité (avec prédiction IA radio si disponible)
            Examens_demandes ed = (idx < examensDemandes.size()) ? examensDemandes.get(idx) : null;
            GraviteService.ScoreResult score = GraviteService.calculerScoreAvecIA(
                    r, ed, predictionIA, confianceIA);            r.setScoreGravite(score.score);
            r.setNiveauGravite(score.niveau);
            r.setRecommandation(score.recommandation);

            // Afficher le score dans l'UI
            afficherScore(score);

            service.ajouter(r);

            // ── Envoi email (thread séparé)
            final int    idDemandeEmail   = r.getIdDemande();
            final String typeExamenEmail  = cbIdDemande.getItems().get(idx).contains(" — ")
                    ? cbIdDemande.getItems().get(idx).split(" — ")[1]
                    : cbIdDemande.getItems().get(idx);
            final String compteRenduEmail = r.getCompteRendu();
            final String fichierEmail     = r.getFichierJoint();
            final java.util.Date dateEmail = r.getDateResultat();

            Thread emailThread = new Thread(() -> {
                try {
                    new EmailService()
                        .envoyerRapportMedecin(idDemandeEmail, typeExamenEmail,
                                               compteRenduEmail, fichierEmail, dateEmail,
                                               r.getScoreGravite(), r.getNiveauGravite(),
                                               r.getRecommandation());
                } catch (Exception e) {
                    System.err.println("❌ Envoi email échoué : " + e.getMessage());
                }
            });
            emailThread.setDaemon(true);
            emailThread.start();

            // ── Retirer de la ComboBox
            String nomPatient = cbIdDemande.getItems().get(idx);
            cbIdDemande.getItems().remove(idx);
            idsDemande.remove(idx);

            if (!cbIdDemande.getItems().isEmpty()) {
                cbIdDemande.getSelectionModel().selectFirst();
            } else {
                cbIdDemande.getSelectionModel().clearSelection();
                cbIdDemande.setPromptText("— Aucun examen réalisé sans résultat —");
                lblErrIdDemande.setText("ℹ  Tous les examens réalisés ont déjà un résultat.");
            }

            // ── Réinitialiser
            taCompteRendu.clear();
            tfFichierJoint.clear();
            cbEtat.setValue("Propre");
            cbEtat.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;" +
                    "-fx-border-color: #CBD5E1; -fx-font-size: 13px;");
            lblErrCompteRendu.setText("");
            retablirTA(taCompteRendu);
            imageSélectionnée = null;
            panneauApercu.setVisible(false);
            panneauApercu.setManaged(false);
            masquerResultatIA();
            // Réinitialiser le panneau image simple
            if (tfFichierJointSimple != null) tfFichierJointSimple.clear();
            imageSimpleSélectionnée = null;
            if (panneauApercuSimple != null) { panneauApercuSimple.setVisible(false); panneauApercuSimple.setManaged(false); }
            masquerScore();
            masquerAnalyseBiologique();

            afficherSucces("✅  Résultat ajouté pour : " + nomPatient.split(" — ")[0]);
            afficherAlertSucces("Résultat ajouté avec succès !");

        } catch (Exception ex) {
            afficherErreur("Erreur lors de l'ajout : " + ex.getMessage());
            afficherAlertErreur("Erreur lors de l'ajout :\n" + ex.getMessage());
        }
    }

    @FXML
    private void handleVider() {
        if (!cbIdDemande.getItems().isEmpty())
            cbIdDemande.getSelectionModel().selectFirst();
        taCompteRendu.clear();
        tfFichierJoint.clear();
        cbEtat.setValue("Propre");
        cbEtat.setStyle("-fx-background-radius: 10; -fx-border-radius: 10;" +
                "-fx-border-color: #CBD5E1; -fx-font-size: 13px;");
        lblErrIdDemande.setText("");
        lblErrCompteRendu.setText("");
        retablirTA(taCompteRendu);
        imageSélectionnée = null;
        predictionIA      = null;
        confianceIA       = 0.0;
        panneauApercu.setVisible(false);
        panneauApercu.setManaged(false);
        masquerResultatIA();
        afficherChargement(false);
        // Réinitialiser le panneau image simple
        if (tfFichierJointSimple != null) tfFichierJointSimple.clear();
        imageSimpleSélectionnée = null;
        if (panneauApercuSimple != null) { panneauApercuSimple.setVisible(false); panneauApercuSimple.setManaged(false); }
        masquerScore();
        masquerAnalyseBiologique();
        lblMessage.setText("");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS UI
    // ─────────────────────────────────────────────────────────────────────────

    private void afficherAlertSucces(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("✅  " + message);
        alert.showAndWait();
    }

    private void afficherAlertErreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static final String STYLE_BASE_TA =
            "-fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 9 14; -fx-font-size: 13px;";

    private void mettreEnErreurTA(TextArea ta) {
        ta.setStyle(STYLE_BASE_TA + "-fx-border-color: #EF4444;");
    }
    private void retablirTA(TextArea ta) {
        ta.setStyle(STYLE_BASE_TA + "-fx-border-color: #CBD5E1;");
    }
    private void afficherErreur(String msg) {
        lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }
    private void afficherSucces(String msg) {
        lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }

    // ── Effets hover sur les boutons icônes
    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setScaleX(1.15); btn.setScaleY(1.15);
        }
    }
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setScaleX(1.0); btn.setScaleY(1.0);
        }
    }
}

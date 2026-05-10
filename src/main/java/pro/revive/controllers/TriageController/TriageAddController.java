package pro.revive.controllers.TriageController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pro.revive.Navigator;
import pro.revive.entities.Triage;
import pro.revive.services.AIAnalysisService;
import pro.revive.services.EpidemiologicalDetector;
import pro.revive.services.GravityCalculator;
import pro.revive.services.TriageService;
import pro.revive.utils.AdmissionItem;
import pro.revive.utils.AppExecutor;
import pro.revive.utils.InputValidator;
import pro.revive.utils.UIUtils;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class TriageAddController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private ComboBox<AdmissionItem> cbAdmission;
    @FXML private TextField tfTaSys, tfTaDia, tfPouls, tfTemp, tfSpo2, tfGlyc, tfGcs, tfFreqResp;
    @FXML private Slider sliderDouleur;
    @FXML private Label lblDouleurVal;
    @FXML private TextArea taSymptomes;
    @FXML private Label lblScore, lblNiveauLabel, lblAnalyse;
    @FXML private VBox  resultPanel;
    @FXML private Label lblResultId, lblResultNiveau, lblResultSalle;

    // ── Champs épidémiologiques ──────────────────────────────────
    @FXML private Button btnToggleEpi;
    @FXML private VBox   epiContextBox;
    @FXML private ComboBox<String> cbSyndrome, cbDuree;
    @FXML private Button btnVoyageOui, btnVoyageNon;
    @FXML private VBox   voyageDestBox;
    @FXML private TextField tfVoyageDest;
    private boolean epiActive  = false;
    private boolean voyageOui  = false;

    private final TriageService          service    = new TriageService();
    private final AIAnalysisService      aiService  = new AIAnalysisService();
    private final EpidemiologicalDetector detector  = new EpidemiologicalDetector();
    private Triage lastSaved = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);

        // Slider label
        sliderDouleur.valueProperty().addListener((obs, oldVal, newVal) ->
            lblDouleurVal.setText(String.valueOf(newVal.intValue()))
        );

        // ── Restrictive input controls (digits only + cap at max, red if below min on leave) ──
        InputValidator.attachDecimalAutoDotListener(tfTaSys, 2, 1, 5.0f, 30.0f);
        InputValidator.attachDecimalAutoDotListener(tfTaDia, 2, 1, 2.0f, 20.0f);
        InputValidator.attachIntRestrictiveListener(tfPouls,    20,  300);
        InputValidator.attachDecimalAutoDotListener(tfTemp, 2, 2, 30.0f, 45.0f);
        InputValidator.attachIntRestrictiveListener(tfSpo2,     50,  100);
        InputValidator.attachDecimalAutoDotListener(tfGlyc, 1, 2, 0.1f, 10.0f);
        InputValidator.attachIntRestrictiveListener(tfGcs,      3,   15);
        InputValidator.attachIntRestrictiveListener(tfFreqResp, 1,   60);

        // Text area: max 500 characters
        InputValidator.attachMaxLengthListener(taSymptomes, 500);

        // ── Initialiser les listes épidémiologiques ──────────────
        cbSyndrome.getItems().addAll("Respiratoire", "Digestif", "Neurologique",
                "Cutane", "Cardiovasculaire", "Trauma", "Autre");
        cbDuree.getItems().addAll("< 6h", "6-24h", "1-3 jours", "> 3 jours");

        // Auto-suggérer le syndrome quand les symptômes changent
        taSymptomes.textProperty().addListener((obs, o, n) -> {
            if (epiActive && n != null && !n.isEmpty()) {
                String auto = detector.autoClassifierSyndrome(n);
                if (cbSyndrome.getValue() == null) cbSyndrome.setValue(auto);
            }
        });

        loadAdmissions();
    }

    @FXML
    public void loadAdmissions() {
        cbAdmission.getItems().clear();
        List<AdmissionItem> items = service.getActiveAdmissions();
        cbAdmission.getItems().addAll(items);
    }

    @FXML
    public void calculerScore() {
        // Score only needs vitals — no admission required
        InputValidator.Result vr = InputValidator.validateNotEmpty(
            tfTaSys, tfTaDia, tfPouls, tfTemp, tfSpo2, tfGlyc, tfGcs, tfFreqResp, taSymptomes);

        if (!vr.isValid()) {
            lblScore.setText("!");
            lblNiveauLabel.setText("Corriger les erreurs");
            lblAnalyse.setText(vr.summary());
            lblScore.setStyle("-fx-font-size: 52px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
            return;
        }

        // Build a temporary triage just for score calculation (admission not needed)
        try {
            float taSys = Float.parseFloat(tfTaSys.getText().trim());
            float taDia = Float.parseFloat(tfTaDia.getText().trim());
            int pouls   = Integer.parseInt(tfPouls.getText().trim());
            float temp  = Float.parseFloat(tfTemp.getText().trim());
            int spo2    = Integer.parseInt(tfSpo2.getText().trim());
            float glyc  = Float.parseFloat(tfGlyc.getText().trim());
            int gcs     = Integer.parseInt(tfGcs.getText().trim());
            int freq    = Integer.parseInt(tfFreqResp.getText().trim());
            int douleur = (int) sliderDouleur.getValue();
            String sym  = taSymptomes.getText().trim();

            Triage t = new Triage(0, 0, taSys, taDia, pouls, temp, spo2, glyc, douleur, gcs, freq, sym);
            GravityCalculator.calculateScore(t);

            lblScore.setText(String.valueOf(t.getScoreCalcule()));
            lblNiveauLabel.setText("Niveau " + t.getNiveauFinal() + " — " + GravityCalculator.levelLabel(t.getNiveauFinal()));
            lblAnalyse.setText(t.getAnalyseAuto());
            lblScore.setStyle("-fx-font-size: 52px; -fx-font-weight: bold; -fx-text-fill: " + UIUtils.niveauColor(t.getNiveauFinal()) + ";");
        } catch (NumberFormatException ex) {
            lblScore.setText("!");
            lblNiveauLabel.setText("Erreur de saisie");
            lblAnalyse.setText("Verifiez les valeurs saisies.");
        }
    }

    @FXML
    public void enregistrer() {
        // Check admission
        if (cbAdmission.getValue() == null) {
            InputValidator.markError(cbAdmission);
            showWarning("Veuillez selectionner une admission active.");
            return;
        }

        // Check all fields non-empty
        InputValidator.Result vr = InputValidator.validateNotEmpty(
            tfTaSys, tfTaDia, tfPouls, tfTemp, tfSpo2, tfGlyc, tfGcs, tfFreqResp, taSymptomes);

        if (!vr.isValid()) {
            showWarning("Veuillez remplir tous les champs :\n\n" + vr.summary());
            return;
        }

        // Check values are within medical ranges
        InputValidator.Result rr = InputValidator.validateRanges(
            tfTaSys, tfTaDia, tfPouls, tfTemp, tfSpo2, tfGlyc, tfGcs, tfFreqResp);
        if (!rr.isValid()) {
            showWarning("Valeurs hors plage medicale :\n\n" + rr.summary());
            return;
        }

        Triage t = buildTriageFromForm();
        if (t == null) return;
        try {
            service.addEntity2(t);
            lastSaved = t;

            // ── Évaluation contagion IA (en arrière-plan) ────────
            final boolean epiSnap     = epiActive;
            final boolean voyageSnap  = voyageOui;
            final String  syndSnap    = epiActive ? cbSyndrome.getValue() : null;
            final String  dureeSnap   = epiActive ? cbDuree.getValue()    : null;
            final String  destSnap    = voyageOui ? tfVoyageDest.getText().trim() : null;
            AppExecutor.run(() -> {
                try {
                    AIAnalysisService.ContagionResult cr = aiService.evaluerContagion(t);
                    service.updateContagionFlag(t.getIdTriage(), cr.flag, cr.maladieSupspecte);
                    if (epiSnap) {
                        service.updateSurveillance(t.getIdTriage(),
                                syndSnap, dureeSnap,
                                null,  // contact cas similaires — detecte automatiquement par l'IA
                                voyageSnap, destSnap,
                                cr.flag, cr.maladieSupspecte);
                    }
                } catch (Exception ignored) {}
            });

            resultPanel.setVisible(true);
            resultPanel.setManaged(true);
            lblResultId.setText(String.valueOf(t.getIdTriage()));
            lblResultNiveau.setText("Niveau " + t.getNiveauFinal());
            lblResultSalle.setText(t.getNomSalle() != null ? t.getNomSalle() : "En Attente");
            Alert alert = new Alert(Alert.AlertType.INFORMATION,
                    "Triage enregistre avec succes !\n" +
                    "Niveau : " + t.getNiveauFinal() + " — " + pro.revive.services.GravityCalculator.levelLabel(t.getNiveauFinal()) + "\n" +
                    "Salle : " + (t.getNomSalle() != null ? t.getNomSalle() : "En Attente"));
            alert.setTitle("Triage enregistre");
            alert.showAndWait();
            // Navigate to the new triage's detail view
            Navigator.currentTriageId = t.getIdTriage();
            Navigator.goTo("Triage_View");
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur: " + e.getMessage()).showAndWait();
        }
    }

    // ── Contrôles contexte épidémiologique ───────────────────────
    @FXML public void toggleEpiContext() {
        epiActive = !epiActive;
      
        epiContextBox.setVisible(epiActive);
        epiContextBox.setManaged(epiActive);
        btnToggleEpi.setText(epiActive ? "Desactiver" : "Activer contexte epi.");
    }

    @FXML public void setVoyageOui() {
        voyageOui = true;
        btnVoyageOui.setStyle("-fx-background-color: #0B4EA2; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand;");
        btnVoyageNon.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand;");
        voyageDestBox.setVisible(true);
        voyageDestBox.setManaged(true);
    }

    @FXML public void setVoyageNon() {
        voyageOui = false;
        btnVoyageNon.setStyle("-fx-background-color: #0B4EA2; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand;");
        btnVoyageOui.setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; -fx-background-radius: 6; -fx-padding: 4 12; -fx-cursor: hand;");
        voyageDestBox.setVisible(false);
        voyageDestBox.setManaged(false);
    }

    @FXML public void effacer() {
        tfTaSys.clear(); tfTaDia.clear(); tfPouls.clear(); tfTemp.clear();
        tfSpo2.clear(); tfGlyc.clear(); tfGcs.clear(); tfFreqResp.clear();
        sliderDouleur.setValue(0);
        taSymptomes.clear();
        cbAdmission.getSelectionModel().clearSelection();
        lblScore.setText("—");
        lblNiveauLabel.setText("Calculer d'abord");
        lblAnalyse.setText("");
        resultPanel.setVisible(false);
        resultPanel.setManaged(false);
        // Reset epi context
        epiActive = false;
        voyageOui = false;
        epiContextBox.setVisible(false);
        epiContextBox.setManaged(false);
        btnToggleEpi.setText("Activer contexte epi.");
        cbSyndrome.getSelectionModel().clearSelection();
        cbDuree.getSelectionModel().clearSelection();
        voyageDestBox.setVisible(false);
        voyageDestBox.setManaged(false);
        tfVoyageDest.clear();
    }

    // ── Build Triage from form ───────────────────────────────────
    private Triage buildTriageFromForm() {
        try {
            int idAdmission = cbAdmission.getValue().getId();
            float taSys = Float.parseFloat(tfTaSys.getText().trim());
            float taDia = Float.parseFloat(tfTaDia.getText().trim());
            int pouls   = Integer.parseInt(tfPouls.getText().trim());
            float temp  = Float.parseFloat(tfTemp.getText().trim());
            int spo2    = Integer.parseInt(tfSpo2.getText().trim());
            float glyc  = Float.parseFloat(tfGlyc.getText().trim());
            int gcs     = Integer.parseInt(tfGcs.getText().trim());
            int freq    = Integer.parseInt(tfFreqResp.getText().trim());
            int douleur = (int) sliderDouleur.getValue();
            String sym  = taSymptomes.getText().trim();

            Triage t = new Triage(idAdmission, Navigator.currentPersonnelId, taSys, taDia, pouls, temp, spo2, glyc, douleur, gcs, freq, sym);
            if (epiActive) {
                t.setSyndromeCategory(cbSyndrome.getValue());
                t.setDureeSymptomes(cbDuree.getValue());
                // contactCasSimilaires est détecté automatiquement par l'IA — pas de champ manuel
                t.setVoyageRecent(voyageOui);
                t.setVoyageDestination(voyageOui ? tfVoyageDest.getText().trim() : null);
            }
            // Score is calculated inside addEntity2() — no need to call it here too.
            return t;
        } catch (NumberFormatException ex) {
            showWarning("Veuillez verifier les valeurs numeriques saisies.");
            return null;
        }
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.setTitle("Attention");
        a.showAndWait();
    }

    @FXML public void goDashboard()    { Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList()   { Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { Navigator.goTo("DashboardTriage"); }
    @FXML public void goVisualAssistance() { Navigator.goTo("VisualAssistance"); }
    @FXML public void goSurveillance() { Navigator.goTo("Surveillance"); }
}

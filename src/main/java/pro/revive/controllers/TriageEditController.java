package pro.revive.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import pro.revive.Navigator;
import pro.revive.entities.Triage;
import pro.revive.services.GravityCalculator;
import pro.revive.services.RoomAssignmentService;
import pro.revive.services.TriageService;
import pro.revive.utils.InputValidator;

import java.util.Set;

import java.net.URL;
import java.util.ResourceBundle;

public class TriageEditController implements Initializable {

    @FXML private Label lblUserName, lblPageTitle, lblPageSub, lblSideInfo;
    @FXML private Label lblCurrentNiveau, lblCurrentScore, lblCurrentSalle, lblCurrentState;
    @FXML private Label lblNiveauBig, lblNiveauBadge, lblScoreBig;
    @FXML private Label lblPatientNom, lblAdmissionId, lblDateTriage;
    @FXML private TextField tfTaSys, tfTaDia, tfPouls, tfTemp, tfSpo2, tfGlyc, tfGcs, tfFreqResp;
    @FXML private Slider sliderDouleur;
    @FXML private Label lblDouleurVal;
    @FXML private TextArea taSymptomes;
    @FXML private ComboBox<String> cbState, cbOverrideNiveau;
    @FXML private TextField tfOverrideNote;

    private final TriageService service = new TriageService();
    private final RoomAssignmentService roomService = new RoomAssignmentService();
    private Triage current;

    // BUG-3 fix: states that release the room and require freeRoom() side-effects
    private static final Set<String> TERMINAL_STATES = Set.of(
            "Discharged", "Cancelled", "LeftWithoutSeen");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);

        // États en français dans l'UI — valeurs DB conservées en anglais
        cbState.getItems().addAll("WaitingRoom", "InRoom", "InConsultation",
                "Discharged", "Cancelled", "LeftWithoutSeen", "Quarantine");
        // No translation — show raw DB values directly in the dropdown
        cbOverrideNiveau.getItems().addAll("Garder le niveau auto", "Niveau 1 - Critique",
            "Niveau 2 - Tres Urgent", "Niveau 3 - Urgent", "Niveau 4 - Standard", "Niveau 5 - Mineur");
        cbOverrideNiveau.getSelectionModel().selectFirst();

        sliderDouleur.valueProperty().addListener((obs, o, n) ->
            lblDouleurVal.setText(String.valueOf(n.intValue())));

        // ── Restrictive input controls (digits only + cap at max, red if below min on leave) ──
        InputValidator.attachDecimalAutoDotListener(tfTaSys, 2, 1, 5.0f, 30.0f);
        InputValidator.attachDecimalAutoDotListener(tfTaDia, 2, 1, 2.0f, 20.0f);
        InputValidator.attachIntRestrictiveListener(tfPouls,    20,  300);
        InputValidator.attachDecimalAutoDotListener(tfTemp, 2, 2, 30.0f, 45.0f);
        InputValidator.attachIntRestrictiveListener(tfSpo2,     50,  100);
        InputValidator.attachDecimalAutoDotListener(tfGlyc, 1, 2, 0.1f, 10.0f);
        InputValidator.attachIntRestrictiveListener(tfGcs,      3,   15);
        InputValidator.attachIntRestrictiveListener(tfFreqResp, 1,   60);

        // Text fields: max length
        InputValidator.attachMaxLengthListener(taSymptomes, 500);
        InputValidator.attachMaxLengthListener(tfOverrideNote, 200);

        loadTriage();
    }

    private void loadTriage() {
        current = service.getById(Navigator.currentTriageId);
        if (current == null) { Navigator.goTo("Triage_List"); return; }

        lblPageTitle.setText("Modifier Triage #" + current.getIdTriage());
        lblPageSub.setText(current.getNomPatient() + " " + current.getPrenomPatient());
        lblSideInfo.setText("Triage #" + current.getIdTriage());

        lblCurrentNiveau.setText("Niveau " + current.getNiveauFinal());
        lblCurrentScore.setText(String.valueOf(current.getScoreCalcule()));
        lblCurrentSalle.setText(current.getNomSalle() != null ? current.getNomSalle() : "En Attente");
        lblCurrentState.setText(current.getPatientState() != null ? current.getPatientState() : "N/A");

        lblNiveauBig.setText(String.valueOf(current.getNiveauFinal()));
        lblNiveauBadge.setText("Niveau " + current.getNiveauFinal());
        lblScoreBig.setText("Score: " + current.getScoreCalcule() + " pts");
        lblPatientNom.setText(current.getNomPatient() + " " + current.getPrenomPatient());
        lblAdmissionId.setText(String.valueOf(current.getIdAdmission()));
        lblDateTriage.setText(current.getDateHeureTriage() != null ? current.getDateHeureTriage().toString() : "N/A");

        tfTaSys.setText(String.valueOf(current.getConstancesTaSys()));
        tfTaDia.setText(String.valueOf(current.getConstancesTaDia()));
        tfPouls.setText(String.valueOf(current.getConstancesPouls()));
        tfTemp.setText(String.valueOf(current.getConstancesTemperature()));
        tfSpo2.setText(String.valueOf(current.getSpo2()));
        tfGlyc.setText(String.valueOf(current.getGlycemie()));
        tfGcs.setText(String.valueOf(current.getGcsScore()));
        tfFreqResp.setText(String.valueOf(current.getFrequenceRespiratoire()));
        sliderDouleur.setValue(current.getScoreDouleur());
        taSymptomes.setText(current.getSymptomes() != null ? current.getSymptomes() : "");
        cbState.setValue(current.getPatientState());
    }

    @FXML
    public void mettreAJour() {
        // Check state selection
        if (cbState.getValue() == null) {
            InputValidator.markError(cbState);
            showWarning("Veuillez selectionner l'etat du patient.");
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

        // If manual override selected, note is required
        int overIdx = cbOverrideNiveau.getSelectionModel().getSelectedIndex();
        if (overIdx > 0 && tfOverrideNote.getText().trim().isEmpty()) {
            InputValidator.markError(tfOverrideNote);
            showWarning("Veuillez saisir une raison pour la correction manuelle du niveau.");
            return;
        }

        try {
            current.setConstancesTaSys(Float.parseFloat(tfTaSys.getText().trim()));
            current.setConstancesTaDia(Float.parseFloat(tfTaDia.getText().trim()));
            current.setConstancesPouls(Integer.parseInt(tfPouls.getText().trim()));
            current.setConstancesTemperature(Float.parseFloat(tfTemp.getText().trim()));
            current.setSpo2(Integer.parseInt(tfSpo2.getText().trim()));
            current.setGlycemie(Float.parseFloat(tfGlyc.getText().trim()));
            current.setGcsScore(Integer.parseInt(tfGcs.getText().trim()));
            current.setFrequenceRespiratoire(Integer.parseInt(tfFreqResp.getText().trim()));
            current.setScoreDouleur((int) sliderDouleur.getValue());
            current.setSymptomes(taSymptomes.getText().trim());

            String newState = cbState.getValue();
            String oldState = current.getPatientState();
            current.setPatientState(newState);

            // Recalculate score from new vitals.
            // BUG-1 fix: TriageService.updateEntity now persists score_calcule, niveau_auto and
            // analyse_auto, so the recalculation actually reaches the DB.
            GravityCalculator.calculateScore(current);

            // BUG-2 fix: override metadata is now persisted via a dedicated applyOverride() call
            // (only when overIdx > 0), so date_override / id_personnel_override are stamped
            // exactly once — at the moment the override is recorded.
            if (overIdx > 0) {
                current.setNiveauFinal(overIdx);
                current.setOverrideNote(tfOverrideNote.getText().trim());
                current.setIdPersonnelOverride(Navigator.currentPersonnelId);
            }

            // BUG-3 fix: terminal state changes have to release the room.
            // freeRoom() handles: discharge() (sets Discharged + date_liberation + NULL id_salle)
            // + decrements room occupancy + auto-assigns next waiting patient.
            if (TERMINAL_STATES.contains(newState) && !TERMINAL_STATES.contains(oldState)) {
                service.updateEntity(current.getIdTriage(), current);
                if (overIdx > 0) {
                    service.applyOverride(current.getIdTriage(), current.getNiveauFinal(),
                            current.getOverrideNote(), current.getIdPersonnelOverride());
                }
                roomService.freeRoom(current.getIdTriage());
            } else {
                service.updateEntity(current.getIdTriage(), current);
                if (overIdx > 0) {
                    service.applyOverride(current.getIdTriage(), current.getNiveauFinal(),
                            current.getOverrideNote(), current.getIdPersonnelOverride());
                }
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION, "Triage mis a jour avec succes !");
            a.setTitle("Succes");
            a.showAndWait();
            Navigator.currentTriageId = current.getIdTriage();
            Navigator.goTo("Triage_View");

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la mise a jour : " + e.getMessage()).showAndWait();
        }
    }

    // ── UI helpers ───────────────────────────────────────────────
    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg);
        a.setTitle("Attention");
        a.showAndWait();
    }

    @FXML public void goDashboard()    { Navigator.goTo("Dashboard"); }
    @FXML public void goTriageList()   { Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { Navigator.goTo("Dashboard"); }
    @FXML public void goSurveillance() { Navigator.goTo("Surveillance"); }
}

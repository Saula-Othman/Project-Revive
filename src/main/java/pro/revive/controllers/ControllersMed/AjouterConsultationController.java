package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.Ordonnance;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.OrdonnanceService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AjouterConsultationController implements Initializable {

    @FXML private ComboBox<String>  cmbAdmission, cmbOrientation, cmbMedecin;
    @FXML private VBox              panePatient;
    @FXML private Label             lblPatient, lblMode, lblMotif;
    @FXML private TextArea          taDiagnostic;
    @FXML private VBox              paneLigneOrdo;
    @FXML private TextField         tfMedicament, tfPosologie, tfDuree;
    @FXML private TableView<Ordonnance>           tableOrdonnances;
    @FXML private TableColumn<Ordonnance, String> colOrdoMed, colOrdoPos, colOrdoDuree;
    @FXML private TableColumn<Ordonnance, Void>   colOrdoSup;
    @FXML private Label             lblMessage, lblStatut, lblStatutIcon, lblStatutDetail;
    @FXML private VBox              paneResultat;
    @FXML private Label             lblResId, lblResPatient, lblResOrientation, lblResOrdos;

    private final ConsultationService cs = new ConsultationService();
    private final OrdonnanceService   os = new OrdonnanceService();
    private final ObservableList<Ordonnance> ordos = FXCollections.observableArrayList();
    private Consultation consultationCreee = null;
    private final int ID_MEDECIN = 3;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbOrientation.setItems(FXCollections.observableArrayList("Sortie", "Hospitalisation", "Transfert"));
        cmbMedecin.setItems(FXCollections.observableArrayList("Dr. Trabelsi Leila", "Dr. Ben Ali Sami"));
        cmbMedecin.setValue("Dr. Trabelsi Leila");
        cmbAdmission.setItems(FXCollections.observableArrayList(
            "Admission #1 — Mohamed Ben Salah (Ambulance)",
            "Admission #2 — Fatma Gharbi (Propres moyens)",
            "Admission #3 — Karim Mbarki (SMUR)"));

        colOrdoMed.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicament()));
        colOrdoPos.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPosologie()));
        colOrdoDuree.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDureeJours() + " j"));
        colOrdoSup.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            { btn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
              btn.setOnAction(e -> ordos.remove(getTableRow().getItem())); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btn); }
        });
        tableOrdonnances.setItems(ordos);
    }

    @FXML private void handleAdmissionChange() {
        String s = cmbAdmission.getValue();
        if (s == null) return;
        if (s.contains("Ben Salah")) { lblPatient.setText("Mohamed Ben Salah"); lblMode.setText("Ambulance");      lblMotif.setText("Douleur thoracique"); }
        else if (s.contains("Gharbi")) { lblPatient.setText("Fatma Gharbi");    lblMode.setText("Propres moyens"); lblMotif.setText("Fièvre élevée"); }
        else                           { lblPatient.setText("Karim Mbarki");    lblMode.setText("SMUR");           lblMotif.setText("Accident de la route"); }
        panePatient.setVisible(true); panePatient.setManaged(true);
    }

    @FXML private void handleRefreshAdmissions() {
        cmbAdmission.setItems(FXCollections.observableArrayList(
            "Admission #1 — Mohamed Ben Salah (Ambulance)",
            "Admission #2 — Fatma Gharbi (Propres moyens)",
            "Admission #3 — Karim Mbarki (SMUR)"));
    }

    @FXML private void handleAfficherLigneOrdonnance() {
        paneLigneOrdo.setVisible(true); paneLigneOrdo.setManaged(true);
        tfMedicament.requestFocus();
    }

    @FXML private void handleConfirmerOrdo() {
        String med = tfMedicament.getText().trim();
        if (med.isEmpty()) { tfMedicament.setStyle(tfMedicament.getStyle() + "-fx-border-color: #EF4444;"); return; }
        int duree = 0;
        try { duree = Integer.parseInt(tfDuree.getText().trim()); } catch (NumberFormatException ignored) {}
        ordos.add(new Ordonnance(0, med, tfPosologie.getText().trim(), duree));
        tfMedicament.clear(); tfPosologie.clear(); tfDuree.clear();
        paneLigneOrdo.setVisible(false); paneLigneOrdo.setManaged(false);
    }

    @FXML private void handleAnnulerOrdo() {
        tfMedicament.clear(); tfPosologie.clear(); tfDuree.clear();
        paneLigneOrdo.setVisible(false); paneLigneOrdo.setManaged(false);
    }

    @FXML private void handleBrouillon() {
        if (taDiagnostic.getText().isBlank()) { setMsg("⚠️ Saisissez d'abord un diagnostic.", false); return; }
        if (consultationCreee == null) {
            consultationCreee = new Consultation(1, ID_MEDECIN, taDiagnostic.getText(), cmbOrientation.getValue());
            cs.addEntity(consultationCreee);
        } else {
            consultationCreee.setDiagnostic(taDiagnostic.getText());
            cs.updateEntity(consultationCreee.getIdConsultation(), consultationCreee);
        }
        setMsg("✔ Brouillon sauvegardé (ID #" + consultationCreee.getIdConsultation() + ")", true);
    }

    @FXML private void handleValider() {
        if (cmbAdmission.getValue() == null) { setMsg("⚠️ Sélectionnez une admission.", false); return; }
        if (taDiagnostic.getText().isBlank()) { setMsg("⚠️ Le diagnostic est obligatoire.", false); return; }
        if (cmbOrientation.getValue() == null) { setMsg("⚠️ L'orientation est obligatoire.", false); return; }

        if (consultationCreee == null) {
            consultationCreee = new Consultation(1, ID_MEDECIN, taDiagnostic.getText(), cmbOrientation.getValue());
            cs.addEntity(consultationCreee);
        } else {
            consultationCreee.setDiagnostic(taDiagnostic.getText());
            consultationCreee.setOrientation(cmbOrientation.getValue());
            cs.updateEntity(consultationCreee.getIdConsultation(), consultationCreee);
        }

        for (Ordonnance o : ordos) {
            o.setIdConsultation(consultationCreee.getIdConsultation());
            if (o.getIdOrdo() == 0) os.addEntity(o);
        }

        lblResId.setText("#" + consultationCreee.getIdConsultation());
        lblResPatient.setText(lblPatient.getText());
        lblResOrientation.setText(cmbOrientation.getValue());
        lblResOrdos.setText(String.valueOf(ordos.size()));
        paneResultat.setVisible(true); paneResultat.setManaged(true);

        lblStatutIcon.setText("✅"); lblStatut.setText("Clôturée");
        lblStatut.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #22C55E;");
        lblStatutDetail.setText(ordos.size() + " ordonnance(s) · " + cmbOrientation.getValue());
        setMsg("✅ Consultation #" + consultationCreee.getIdConsultation() + " enregistrée avec succès !", true);
    }

    @FXML private void handlePdf() {
        if (consultationCreee == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer l'ordonnance PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        fc.setInitialFileName("ordonnance_" + consultationCreee.getIdConsultation() + ".pdf");
        File f = fc.showSaveDialog(taDiagnostic.getScene().getWindow());
        if (f != null) System.out.println("PDF → " + f.getAbsolutePath());
    }

    @FXML private void handleNouveau() {
        consultationCreee = null; taDiagnostic.clear();
        cmbAdmission.setValue(null); cmbOrientation.setValue(null);
        ordos.clear(); paneResultat.setVisible(false); paneResultat.setManaged(false);
        panePatient.setVisible(false); panePatient.setManaged(false);
        lblStatutIcon.setText("🩺"); lblStatut.setText("En cours");
        lblStatut.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0B4EA2;");
        lblMessage.setText("");
    }

    @FXML private void handleAnnuler() { naviguer("AfficherConsultation.fxml"); }
    @FXML private void handleNavListe() { naviguer("AfficherConsultation.fxml"); }
    @FXML private void handleNavDashboard() { naviguer("dashboardMed.fxml"); }

    private void setMsg(String msg, boolean ok) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (ok ? "#16A34A;" : "#DC2626;"));
    }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ResourcesMed/module3/fxml/" + fxml));
            ((Stage) taDiagnostic.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { System.out.println("[Nav] " + e.getMessage()); }
    }
}

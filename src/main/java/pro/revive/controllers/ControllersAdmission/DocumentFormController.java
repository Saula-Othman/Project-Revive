package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.HistoriqueDAO;
import pro.revive.entities.EntitiesAdmission.HistoriquePatient;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class DocumentFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField titreField;
    @FXML private TextArea contenuArea;
    @FXML private TextField medecinField;
    @FXML private TextField etablissementField;
    @FXML private Label typeError;
    @FXML private Label dateError;
    @FXML private Label titreError;
    @FXML private Label contenuError;
    @FXML private Label formError;
    @FXML private Button saveBtn;

    private final HistoriqueDAO historiqueDAO = new HistoriqueDAO();
    private int patientId;
    private Integer admissionId;
    private boolean saved = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Module 1 (Accueil) peut uniquement créer des Compte-rendus et notes d'hospitalisation.
        // Consultations et Ordonnances sont réservées au Module 3 (Médecin).
        typeCombo.setItems(FXCollections.observableArrayList(
            "Compte-rendu",
            "Hospitalisation"
        ));
        typeCombo.setValue("Compte-rendu");
        datePicker.setValue(LocalDate.now());
    }

    public void setPatientId(int patientId, Integer admissionId) {
        this.patientId = patientId;
        this.admissionId = admissionId;
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        try {
            HistoriquePatient h = new HistoriquePatient();
            h.setPatientId(patientId);
            h.setAdmissionId(admissionId);
            h.setTypeDocument(typeCombo.getValue());
            h.setDateConsultation(datePicker.getValue());
            h.setTitre(titreField.getText().trim());
            h.setContenu(contenuArea.getText().trim());

            String medecin = medecinField.getText().trim();
            h.setMedecinNom(medecin.isEmpty() ? null : medecin);

            String etab = etablissementField.getText().trim();
            h.setEtablissement(etab.isEmpty() ? "Service des Urgences" : etab);

            h.setSource("LOCAL");

            historiqueDAO.save(h);
            saved = true;
            closeDialog();

        } catch (Exception e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validate() {
        clearErrors();
        boolean ok = true;

        if (typeCombo.getValue() == null) {
            typeError.setText("Le type est obligatoire.");
            typeError.setVisible(true); typeError.setManaged(true);
            ok = false;
        }

        if (datePicker.getValue() == null) {
            dateError.setText("La date est obligatoire.");
            dateError.setVisible(true); dateError.setManaged(true);
            ok = false;
        } else if (datePicker.getValue().isAfter(LocalDate.now())) {
            dateError.setText("La date ne peut pas être dans le futur.");
            dateError.setVisible(true); dateError.setManaged(true);
            ok = false;
        }

        if (titreField.getText().trim().isEmpty()) {
            titreError.setText("Le titre est obligatoire.");
            titreError.setVisible(true); titreError.setManaged(true);
            ok = false;
        }

        if (contenuArea.getText().trim().length() < 5) {
            contenuError.setText("Le contenu doit faire au moins 5 caractères.");
            contenuError.setVisible(true); contenuError.setManaged(true);
            ok = false;
        }

        return ok;
    }

    private void clearErrors() {
        typeError.setVisible(false);   typeError.setManaged(false);
        dateError.setVisible(false);   dateError.setManaged(false);
        titreError.setVisible(false);  titreError.setManaged(false);
        contenuError.setVisible(false); contenuError.setManaged(false);
        formError.setVisible(false);   formError.setManaged(false);
    }

    private void showError(String msg) {
        formError.setText(msg);
        formError.setVisible(true);
        formError.setManaged(true);
    }

    @FXML
    private void handleCancel() { closeDialog(); }

    private void closeDialog() {
        ((Stage) saveBtn.getScene().getWindow()).close();
    }

    public boolean isSaved() { return saved; }
}

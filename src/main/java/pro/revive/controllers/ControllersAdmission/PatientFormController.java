package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.PatientDAO;
import pro.revive.entities.EntitiesAdmission.Patient;
import pro.revive.utils.UtilesAdmission.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class PatientFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private DatePicker dateNaissancePicker;
    @FXML private ComboBox<String> sexeCombo;
    @FXML private ComboBox<String> nationaliteCombo;
    @FXML private TextField cinField;
    @FXML private ComboBox<String> groupeSanguinCombo;
    @FXML private TextField telephoneField;
    @FXML private TextField numSecuField;
    @FXML private TextArea adresseArea;
    @FXML private TextArea allergiesArea;
    @FXML private TextArea antecedentsArea;
    @FXML private TextField contactNomField;
    @FXML private TextField contactTelField;
    @FXML private Label nomError;
    @FXML private Label prenomError;
    @FXML private Label cinError;
    @FXML private Label telError;
    @FXML private Label dateNaissanceError;
    @FXML private Label secuError;
    @FXML private Label formError;
    @FXML private Button saveBtn;

    private final PatientDAO dao = new PatientDAO();
    private Patient editPatient;
    private boolean saved = false;
    private String savedName;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sexeCombo.setItems(FXCollections.observableArrayList("M", "F"));
        groupeSanguinCombo.setItems(FXCollections.observableArrayList(
            "A+","A-","B+","B-","AB+","AB-","O+","O-","Inconnu"));
        nationaliteCombo.setItems(FXCollections.observableArrayList(
            "Tunisienne","Française","Algérienne","Marocaine","Libyenne","Autre"));
        nationaliteCombo.setValue("Tunisienne");
        groupeSanguinCombo.setValue("Inconnu");

        // Contrôles de saisie
        ValidationUtil.applyLettersOnly(nomField);
        ValidationUtil.applyLettersOnly(prenomField);
        ValidationUtil.applyLettersOnly(contactNomField);
        ValidationUtil.applyPhoneFormatter(telephoneField);
        ValidationUtil.applyPhoneFormatter(contactTelField);
        ValidationUtil.applyCinFormatter(cinField);
        ValidationUtil.applySecuFormatter(numSecuField);

        // Effacer erreurs à la saisie
        nomField.textProperty().addListener((o, ov, nv) -> hideLabel(nomError));
        prenomField.textProperty().addListener((o, ov, nv) -> hideLabel(prenomError));
        if (cinError != null) cinField.textProperty().addListener((o, ov, nv) -> hideLabel(cinError));
        if (telError != null) telephoneField.textProperty().addListener((o, ov, nv) -> hideLabel(telError));
        if (dateNaissanceError != null) dateNaissancePicker.valueProperty().addListener((o, ov, nv) -> hideLabel(dateNaissanceError));
        if (secuError != null) numSecuField.textProperty().addListener((o, ov, nv) -> hideLabel(secuError));
    }

    public void setPatient(Patient p) {
        this.editPatient = p;
        dialogTitle.setText("Modifier Patient — " + p.getNomComplet());
        saveBtn.setText("Mettre à jour");
        nomField.setText(p.getNom());
        prenomField.setText(p.getPrenom());
        if (p.getDateNaissance() != null) dateNaissancePicker.setValue(p.getDateNaissance());
        if (p.getSexe() != null) sexeCombo.setValue(p.getSexe());
        if (p.getGroupeSanguin() != null) groupeSanguinCombo.setValue(p.getGroupeSanguin());
        if (p.getNationalite() != null) nationaliteCombo.setValue(p.getNationalite());
        if (p.getNumCin() != null) cinField.setText(p.getNumCin());
        if (p.getTelephone() != null) telephoneField.setText(p.getTelephone());
        if (p.getNumSecuriteSociale() != null) numSecuField.setText(p.getNumSecuriteSociale());
        if (p.getAdresse() != null) adresseArea.setText(p.getAdresse());
        if (p.getAllergies() != null) allergiesArea.setText(p.getAllergies());
        if (p.getAntecedents() != null) antecedentsArea.setText(p.getAntecedents());
        if (p.getContactUrgenceNom() != null) contactNomField.setText(p.getContactUrgenceNom());
        if (p.getContactUrgenceTel() != null) contactTelField.setText(p.getContactUrgenceTel());
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        Patient p = editPatient != null ? editPatient : new Patient();
        p.setNom(nomField.getText().trim().toUpperCase());
        p.setPrenom(capitalize(prenomField.getText().trim()));
        p.setDateNaissance(dateNaissancePicker.getValue());
        p.setSexe(sexeCombo.getValue());
        p.setGroupeSanguin(groupeSanguinCombo.getValue());
        p.setNationalite(nationaliteCombo.getValue());
        p.setNumCin(cinField.getText().trim());
        p.setTelephone(telephoneField.getText().trim());
        p.setNumSecuriteSociale(numSecuField.getText().trim());
        p.setAdresse(adresseArea.getText().trim());
        p.setAllergies(allergiesArea.getText().trim());
        p.setAntecedents(antecedentsArea.getText().trim());
        p.setContactUrgenceNom(contactNomField.getText().trim());
        p.setContactUrgenceTel(contactTelField.getText().trim());

        try {
            if (editPatient != null) {
                dao.update(p);
            } else {
                int id = dao.save(p);
                if (id < 0) { showFormError("Erreur: impossible d'enregistrer le patient."); return; }
            }
            savedName = p.getPrenom() + " " + p.getNom();
            saved = true;
            closeDialog();
        } catch (Exception e) {
            showFormError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleCancel() { closeDialog(); }

    private boolean validate() {
        boolean valid = true;
        hideLabel(nomError);
        hideLabel(prenomError);
        hideFormError();
        if (cinError != null) hideLabel(cinError);
        if (telError != null) hideLabel(telError);
        if (dateNaissanceError != null) hideLabel(dateNaissanceError);
        if (secuError != null) hideLabel(secuError);

        // Nom : obligatoire, lettres uniquement
        String nom = nomField.getText().trim();
        if (nom.isEmpty()) {
            showLabel(nomError, "Le nom est obligatoire");
            valid = false;
        } else if (!ValidationUtil.isValidName(nom)) {
            showLabel(nomError, "Lettres uniquement (pas de chiffres)");
            valid = false;
        }

        // Prénom : obligatoire, lettres uniquement
        String prenom = prenomField.getText().trim();
        if (prenom.isEmpty()) {
            showLabel(prenomError, "Le prénom est obligatoire");
            valid = false;
        } else if (!ValidationUtil.isValidName(prenom)) {
            showLabel(prenomError, "Lettres uniquement (pas de chiffres)");
            valid = false;
        }

        // Sexe obligatoire
        if (sexeCombo.getValue() == null) {
            showFormError("Veuillez sélectionner le sexe.");
            valid = false;
        }

        // Date de naissance : pas dans le futur
        LocalDate dateNaissance = dateNaissancePicker.getValue();
        if (dateNaissance != null && !ValidationUtil.isDateNaissanceValide(dateNaissance)) {
            showLabel(dateNaissanceError, "La date de naissance ne peut pas être dans le futur");
            valid = false;
        }

        // CIN : 8 chiffres (optionnel)
        String cin = cinField.getText().trim();
        if (!ValidationUtil.isValidCin(cin)) {
            showLabel(cinError, "CIN: 8 chiffres requis");
            valid = false;
        }

        // Téléphone : valide si rempli (pas de 0 ou 1 en début)
        String tel = telephoneField.getText().trim();
        if (!ValidationUtil.isValidPhone(tel)) {
            showLabel(telError, "N° invalide — les numéros tunisiens commencent par 2, 5, 7 ou 9");
            telephoneField.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;");
            valid = false;
        } else {
            telephoneField.setStyle("");
        }

        // Numéro de sécurité sociale : chiffres uniquement (optionnel)
        String secu = numSecuField.getText().trim();
        if (!ValidationUtil.isValidNumSecu(secu)) {
            showLabel(secuError, "N° sécu : chiffres uniquement");
            valid = false;
        }

        return valid;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    private void showLabel(Label lbl, String msg) {
        if (lbl == null) return;
        lbl.setText(msg); lbl.setVisible(true); lbl.setManaged(true);
    }
    private void hideLabel(Label lbl) {
        if (lbl == null) return;
        lbl.setVisible(false); lbl.setManaged(false);
    }
    private void showFormError(String msg) {
        formError.setText(msg); formError.setVisible(true); formError.setManaged(true);
    }
    private void hideFormError() { formError.setVisible(false); formError.setManaged(false); }
    private void closeDialog() { ((Stage) saveBtn.getScene().getWindow()).close(); }

    public boolean isSaved() { return saved; }
    public String getSavedPatientName() { return savedName; }
}

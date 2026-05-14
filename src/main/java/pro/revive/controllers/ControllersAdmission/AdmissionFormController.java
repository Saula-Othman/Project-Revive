package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.AdmissionDAO;
import pro.revive.daoAdmission.HistoriqueDAO;
import pro.revive.daoAdmission.NotificationDAO;
import pro.revive.daoAdmission.PatientDAO;
import pro.revive.entities.EntitiesAdmission.Admission;
import pro.revive.entities.EntitiesAdmission.HistoriquePatient;
import pro.revive.entities.EntitiesAdmission.Patient;
import pro.revive.services.ServicesAdmission.MdiaService;
import pro.revive.utils.UtilesAdmission.ValidationUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class AdmissionFormController implements Initializable {

    @FXML private Label dialogTitle;
    @FXML private ComboBox<Patient> patientCombo;
    @FXML private VBox patientInfoBox;
    @FXML private Label infoNom;
    @FXML private Label infoDN;
    @FXML private Label infoGS;
    @FXML private Label infoAllergies;
    @FXML private Label infoAdmissions;
    @FXML private VBox mdiaBox;
    @FXML private Button mdiaImportBtn;
    @FXML private VBox mdiaResultBox;
    @FXML private Label mdiaStatus;
    @FXML private ComboBox<String> modeArriveeCombo;
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private TextArea motifArea;
    @FXML private TextArea notesArea;
    @FXML private Label motifError;
    @FXML private CheckBox patientInconnuCheck;
    @FXML private VBox ambInfoBox;
    @FXML private TextArea ambInfoArea;
    @FXML private CheckBox alertTriageCheck;
    @FXML private Label formError;
    @FXML private Button saveBtn;

    // Historique import section
    @FXML private VBox historiqueImportBox;
    @FXML private Button openHistoriqueBtn;
    @FXML private VBox importedInfoBox;
    @FXML private Label importedInfoLabel;

    // Patient inconnu fields
    @FXML private TextField inconnu_nomField;
    @FXML private TextField inconnu_prenomField;
    @FXML private TextField inconnu_ageField;
    @FXML private ComboBox<String> inconnu_sexeCombo;
    @FXML private Label inconnu_nomError;
    @FXML private Label inconnu_prenomError;

    private final PatientDAO patientDAO = new PatientDAO();
    private final AdmissionDAO admissionDAO = new AdmissionDAO();
    private final HistoriqueDAO historiqueDAO = new HistoriqueDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private final MdiaService mdiaService = new MdiaService();
    private Admission editAdmission;
    private boolean saved = false;
    private Patient preSelectedPatient;
    private final List<HistoriquePatient> documentsAImporter = new ArrayList<>();
    private Integer linkedAmbulanceId; // ambulance liée à cette admission (lecture seule)

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        modeArriveeCombo.setItems(FXCollections.observableArrayList("Propres moyens", "Ambulance", "SMUR"));
        modeArriveeCombo.setValue("Propres moyens");

        prioriteCombo.setItems(FXCollections.observableArrayList(
            "Non \u00e9valu\u00e9", "Critique", "Urgent", "Mod\u00e9r\u00e9", "Peu urgent", "Normal"
        ));
        prioriteCombo.setValue("Non \u00e9valu\u00e9");

        // Init inconnu sex combo if present
        if (inconnu_sexeCombo != null) {
            inconnu_sexeCombo.setItems(FXCollections.observableArrayList("M", "F", "Inconnu"));
            inconnu_sexeCombo.setValue("Inconnu");
        }

        loadPatients();
        patientCombo.setOnAction(e -> onPatientSelected());

        patientInconnuCheck.setOnAction(e -> {
            boolean inconnu = patientInconnuCheck.isSelected();
            ambInfoBox.setVisible(inconnu);
            ambInfoBox.setManaged(inconnu);
            patientCombo.setDisable(inconnu);
            if (inconnu) {
                patientInfoBox.setVisible(false);
                patientInfoBox.setManaged(false);
                mdiaBox.setVisible(false);
                mdiaBox.setManaged(false);
                modeArriveeCombo.setValue("Ambulance");
                if (historiqueImportBox != null) {
                    historiqueImportBox.setVisible(false);
                    historiqueImportBox.setManaged(false);
                }
            } else {
                // Re-enable patient selection
                onPatientSelected();
            }
        });
    }

    private void loadPatients() {
        try {
            List<Patient> patients = patientDAO.findAll();
            patientCombo.setItems(FXCollections.observableArrayList(patients));

            patientCombo.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(Patient p, boolean empty) {
                    super.updateItem(p, empty);
                    if (empty || p == null) { setText(null); return; }
                    String cin = (p.getNumCin() != null && !p.getNumCin().isEmpty()) ? " \u2014 CIN: " + p.getNumCin() : "";
                    setText(p.getNomComplet() + cin);
                }
            });
            patientCombo.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Patient p, boolean empty) {
                    super.updateItem(p, empty);
                    setText(empty || p == null ? null : p.getNomComplet());
                }
            });

            if (preSelectedPatient != null) {
                // Find patient in list by ID
                for (Patient p : patients) {
                    if (p.getId() == preSelectedPatient.getId()) {
                        patientCombo.setValue(p);
                        break;
                    }
                }
                onPatientSelected();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onPatientSelected() {
        Patient p = patientCombo.getValue();
        if (p == null || patientInconnuCheck.isSelected()) {
            patientInfoBox.setVisible(false);
            patientInfoBox.setManaged(false);
            mdiaBox.setVisible(false);
            mdiaBox.setManaged(false);
            documentsAImporter.clear();
            showImportedSummary();
            return;
        }

        documentsAImporter.clear();
        showImportedSummary();

        infoNom.setText(p.getNomComplet());
        infoDN.setText(p.getDateNaissance() != null ? p.getDateNaissance().toString() : "\u2014");
        infoGS.setText(p.getGroupeSanguin() != null ? p.getGroupeSanguin() : "Inconnu");
        String allergies = (p.getAllergies() != null && !p.getAllergies().isEmpty()) ? p.getAllergies() : "Aucune connue";
        infoAllergies.setText(allergies);
        patientInfoBox.setVisible(true);
        patientInfoBox.setManaged(true);

        try {
            int admCount = patientDAO.countAdmissions(p.getId());
            infoAdmissions.setText(admCount + " visite(s)");

            // Show MDIA import if patient has prior admissions
            if (admCount > 0 && mdiaService.patientExisteDansMdia(p.getNumSecuriteSociale(), p.getNumCin())) {
                mdiaBox.setVisible(true);
                mdiaBox.setManaged(true);
                mdiaResultBox.setVisible(false);
                mdiaResultBox.setManaged(false);
                mdiaImportBtn.setDisable(false);
                mdiaImportBtn.setText("Importer Dossier MDIA");
            } else {
                mdiaBox.setVisible(false);
                mdiaBox.setManaged(false);
            }

            // Show historique import section if patient has prior admissions
            if (admCount > 0 && historiqueImportBox != null) {
                historiqueImportBox.setVisible(true);
                historiqueImportBox.setManaged(true);
            } else if (historiqueImportBox != null) {
                historiqueImportBox.setVisible(false);
                historiqueImportBox.setManaged(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleMdiaImport() {
        Patient p = patientCombo.getValue();
        if (p == null) return;

        mdiaImportBtn.setDisable(true);
        mdiaImportBtn.setText("Importation...");
        mdiaStatus.setText("Connexion \u00e0 l'API MDIA en cours...");
        mdiaResultBox.setVisible(true);
        mdiaResultBox.setManaged(true);

        new Thread(() -> {
            try {
                List<HistoriquePatient> historique = mdiaService.importerDossierMdia(
                    p.getNumSecuriteSociale(), p.getNumCin(), p.getId()
                );
                Platform.runLater(() -> {
                    if (historique.isEmpty()) {
                        mdiaStatus.setText("Aucun document MDIA disponible pour ce patient.");
                        mdiaStatus.setStyle("-fx-text-fill: #64748b;");
                        mdiaImportBtn.setDisable(false);
                        mdiaImportBtn.setText("R\u00e9essayer");
                        return;
                    }
                    documentsAImporter.addAll(historique);
                    mdiaStatus.setText("\u2713 " + historique.size() + " document(s) disponible(s) dans le dossier patient.");
                    mdiaStatus.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    mdiaImportBtn.setText("\u2713 Dossier pr\u00eat");
                    showImportedSummary();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    mdiaStatus.setText("\u2717 Erreur MDIA: " + e.getMessage());
                    mdiaStatus.setStyle("-fx-text-fill: #dc2626;");
                    mdiaImportBtn.setDisable(false);
                    mdiaImportBtn.setText("R\u00e9essayer");
                });
            }
        }).start();
    }

    @FXML
    private void handleOuvrirHistorique() {
        Patient p = patientCombo.getValue();
        if (p == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/HistoriqueSelectDialog.fxml"));
            Parent content = loader.load();
            HistoriqueSelectController ctrl = loader.getController();
            ctrl.setPatientId(p.getId(), p.getNomComplet());

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Historique — " + p.getNomComplet());
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

            if (ctrl.hasImported() && !ctrl.getImportedDocuments().isEmpty()) {
                documentsAImporter.addAll(ctrl.getImportedDocuments());
                showImportedSummary();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEffacerImport() {
        documentsAImporter.clear();
        if (importedInfoBox != null) {
            importedInfoBox.setVisible(false);
            importedInfoBox.setManaged(false);
        }
        if (mdiaImportBtn != null) {
            mdiaImportBtn.setDisable(false);
            mdiaImportBtn.setText("Importer MDIA");
        }
    }

    @FXML
    private void handleNewPatient() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/PatientForm.fxml"));
            Parent content = loader.load();
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Nouveau Patient");
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            loadPatients();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSave() {
        if (!validate()) return;

        try {
            Admission a = editAdmission != null ? editAdmission : new Admission();

            if (patientInconnuCheck.isSelected()) {
                // Validate inconnu fields
                if (!validateInconnu()) return;

                String nom = (inconnu_nomField != null && !inconnu_nomField.getText().trim().isEmpty())
                    ? inconnu_nomField.getText().trim().toUpperCase() : "INCONNU";
                String prenom = (inconnu_prenomField != null && !inconnu_prenomField.getText().trim().isEmpty())
                    ? inconnu_prenomField.getText().trim() : "Patient";
                String sexe = (inconnu_sexeCombo != null && inconnu_sexeCombo.getValue() != null
                    && !inconnu_sexeCombo.getValue().equals("Inconnu"))
                    ? inconnu_sexeCombo.getValue() : "M";

                if (editAdmission != null && editAdmission.getPatientId() > 0) {
                    // MODE MODIFICATION : mettre à jour le patient existant (compléter patient inconnu ambulance)
                    try {
                        Patient existing = patientDAO.findById(editAdmission.getPatientId());
                        if (existing != null) {
                            existing.setNom(nom);
                            existing.setPrenom(prenom);
                            existing.setSexe(sexe);
                            if (inconnu_ageField != null && !inconnu_ageField.getText().trim().isEmpty()) {
                                String ageNote = "Age estimé à l'admission: " + inconnu_ageField.getText().trim();
                                existing.setAntecedents(existing.getAntecedents() != null && !existing.getAntecedents().isEmpty()
                                    ? existing.getAntecedents() + "\n" + ageNote : ageNote);
                            }
                            patientDAO.update(existing);
                        }
                    } catch (Exception ignored) {}
                    a.setPatientId(editAdmission.getPatientId());
                } else {
                    // MODE CREATION : creer nouveau patient inconnu
                    Patient inconnu = new Patient();
                    inconnu.setNom(nom);
                    inconnu.setPrenom(prenom);
                    inconnu.setSexe(sexe);
                    int pid = patientDAO.save(inconnu);
                    if (pid < 0) { showFormError("Erreur: impossible de cr\u00e9er le patient inconnu."); return; }
                    a.setPatientId(pid);
                }
                a.setPatientInconnu(true);
                String ambInfo = (ambInfoArea != null && !ambInfoArea.getText().trim().isEmpty())
                    ? ambInfoArea.getText().trim() : "";
                String ageInfo = (inconnu_ageField != null && !inconnu_ageField.getText().trim().isEmpty())
                    ? " | Age estim\u00e9: " + inconnu_ageField.getText().trim() : "";
                a.setNotes("PATIENT INCONNU" + ageInfo + (ambInfo.isEmpty() ? "" : " | " + ambInfo));
            } else {
                Patient p = patientCombo.getValue();
                if (p == null) { showFormError("Veuillez s\u00e9lectionner un patient."); return; }
                a.setPatientId(p.getId());
            }

            a.setModeArrivee(modeArriveeCombo.getValue());
            a.setMotifAdmission(motifArea.getText().trim());
            a.setPrioriteInitiale(prioriteCombo.getValue());
            a.setAgentAccueilId(MainController.getPersonnelId());

            // notes field (don't overwrite inconnu notes)
            if (!patientInconnuCheck.isSelected() && notesArea != null) {
                a.setNotes(notesArea.getText().trim());
            }

            int admId;
            if (editAdmission != null) {
                admissionDAO.update(a);
                admId = editAdmission.getId();
            } else {
                admId = admissionDAO.save(a);
                if (admId < 0) { showFormError("Erreur: l'admission n'a pas \u00e9t\u00e9 enregistr\u00e9e."); return; }
                // Create historique entry for new admission
                try { historiqueDAO.createHistoriqueForAdmission(a.getPatientId(), admId, a.getMotifAdmission()); }
                catch (Exception ignored) {}
            }

            try {
                enregistrerDocumentsImportes(a.getPatientId(), admId);
            } catch (Exception e) {
                showFormError("Admission enregistr\u00e9e, mais import dossier impossible: " + e.getMessage());
                e.printStackTrace();
                return;
            }

            // Notifications to Module 2
            try {
                if (patientInconnuCheck.isSelected() && alertTriageCheck != null && alertTriageCheck.isSelected()) {
                    notifDAO.sendNotification("MODULE_1", "MODULE_2", "ALERTE_AMBULANCE",
                        "Ambulance critique en approche",
                        "Patient inconnu \u2014 " + (a.getNotes() != null ? a.getNotes() : ""),
                        a.getPatientId(), admId);
                } else if (editAdmission == null) {
                    notifDAO.sendNotification("MODULE_1", "MODULE_2", "PATIENT_ADMIS",
                        "Nouveau patient admis",
                        "Mode: " + a.getModeArrivee() + " \u2014 Motif: " + a.getMotifAdmission(),
                        a.getPatientId(), admId);
                }
            } catch (Exception ignored) {}

            saved = true;
            closeDialog();

        } catch (Exception e) {
            showFormError("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validate() {
        hideFormError();
        boolean valid = true;
        if (motifError != null) { motifError.setVisible(false); motifError.setManaged(false); }

        // Patient obligatoire
        if (!patientInconnuCheck.isSelected() && patientCombo.getValue() == null) {
            showFormError("Veuillez s\u00e9lectionner un patient ou cocher 'Patient inconnu'.");
            valid = false;
        }
        // Mode d'arrivée obligatoire
        if (modeArriveeCombo.getValue() == null || modeArriveeCombo.getValue().trim().isEmpty()) {
            showFormError("Le mode d'arrivée est obligatoire.");
            valid = false;
        }

        // Priorité obligatoire
        if (prioriteCombo.getValue() == null || prioriteCombo.getValue().trim().isEmpty()) {
            showFormError("La priorité est obligatoire.");
            valid = false;
        }

        // Motif obligatoire et min 5 caractères
        String motif = motifArea.getText().trim();
        if (motif.isEmpty()) {
            if (motifError != null) {
                motifError.setText("Le motif d\'admission est obligatoire.");
                motifError.setVisible(true); motifError.setManaged(true);
            } else showFormError("Le motif d\'admission est obligatoire.");
            valid = false;
        } else if (motif.length() < 5) {
            if (motifError != null) {
                motifError.setText("Le motif doit contenir au moins 5 caractères.");
                motifError.setVisible(true); motifError.setManaged(true);
            } else showFormError("Le motif doit contenir au moins 5 caractères.");
            valid = false;
        }

        return valid;
    }

    private boolean validateInconnu() {
        boolean ok = true;

        // Nom obligatoire
        if (inconnu_nomField != null && inconnu_nomField.getText().trim().isEmpty()) {
            if (inconnu_nomError != null) {
                inconnu_nomError.setText("Le nom provisoire est obligatoire");
                inconnu_nomError.setVisible(true); inconnu_nomError.setManaged(true);
            }
            ok = false;
        }

        // Prénom obligatoire
        if (inconnu_prenomField != null && inconnu_prenomField.getText().trim().isEmpty()) {
            if (inconnu_prenomError != null) {
                inconnu_prenomError.setText("Le prénom provisoire est obligatoire");
                inconnu_prenomError.setVisible(true); inconnu_prenomError.setManaged(true);
            }
            ok = false;
        }

        // Age : numérique uniquement si rempli
        if (inconnu_ageField != null && !inconnu_ageField.getText().trim().isEmpty()) {
            String age = inconnu_ageField.getText().trim().replaceAll("\\D", "");
            if (age.isEmpty() || Integer.parseInt(age) < 0 || Integer.parseInt(age) > 130) {
                inconnu_ageField.setStyle("-fx-border-color: #dc2626;");
                if (!ok || true) showFormError("Age invalide (entier entre 0 et 130).");
                ok = false;
            } else {
                inconnu_ageField.setStyle("");
            }
        }

        // Sexe obligatoire
        if (inconnu_sexeCombo != null && inconnu_sexeCombo.getValue() == null) {
            showFormError("Veuillez indiquer le sexe apparent.");
            ok = false;
        }

        if (inconnu_nomField != null && !inconnu_nomField.getText().trim().isEmpty()
                && !ValidationUtil.isValidName(inconnu_nomField.getText().trim())) {
            if (inconnu_nomError != null) {
                inconnu_nomError.setText("Lettres uniquement");
                inconnu_nomError.setVisible(true); inconnu_nomError.setManaged(true);
            }
            ok = false;
        }
        if (inconnu_prenomField != null && !inconnu_prenomField.getText().trim().isEmpty()
                && !ValidationUtil.isValidName(inconnu_prenomField.getText().trim())) {
            if (inconnu_prenomError != null) {
                inconnu_prenomError.setText("Lettres uniquement");
                inconnu_prenomError.setVisible(true); inconnu_prenomError.setManaged(true);
            }
            ok = false;
        }

        return ok;
    }

    @FXML private void handleCancel() { closeDialog(); }

    private void showImportedSummary() {
        boolean hasDocuments = !documentsAImporter.isEmpty();
        if (importedInfoBox != null) {
            importedInfoBox.setVisible(hasDocuments);
            importedInfoBox.setManaged(hasDocuments);
        }
        if (importedInfoLabel != null) {
            long copyCount = documentsAImporter.stream().filter(this::shouldCopyImportedDocument).count();
            if (copyCount == 0 && hasDocuments) {
                importedInfoLabel.setText("Admissions pr\u00e9c\u00e9dentes d\u00e9j\u00e0 disponibles dans l'historique. Aucune copie en double ne sera cr\u00e9\u00e9e.");
            } else {
                importedInfoLabel.setText(copyCount
                    + " document(s) seront copi\u00e9s dans le dossier de cette admission.");
            }
        }
    }

    private boolean shouldCopyImportedDocument(HistoriquePatient source) {
        return !("Compte-rendu".equals(source.getTypeDocument()) && source.getAdmissionId() != null);
    }

    private void enregistrerDocumentsImportes(int patientId, int admissionId) throws Exception {
        if (documentsAImporter.isEmpty()) return;

        for (HistoriquePatient source : documentsAImporter) {
            if (!shouldCopyImportedDocument(source)) {
                continue;
            }
            HistoriquePatient copie = new HistoriquePatient();
            copie.setPatientId(patientId);
            copie.setAdmissionId(admissionId);
            copie.setDateConsultation(source.getDateConsultation() != null ? source.getDateConsultation() : LocalDate.now());
            copie.setTypeDocument(source.getTypeDocument() != null ? source.getTypeDocument() : "Document importe");
            copie.setTitre(source.getTitre() != null ? source.getTitre() : "Dossier patient");
            copie.setContenu(source.getContenu());
            copie.setMedecinNom(source.getMedecinNom());
            copie.setEtablissement(source.getEtablissement());
            copie.setSource("LOCAL");
            historiqueDAO.save(copie);
        }
    }

    public void setPreSelectedPatient(Patient p) { this.preSelectedPatient = p; }

    public void setAdmission(Admission a) {
        this.editAdmission = a;
        dialogTitle.setText("Modifier Admission");
        saveBtn.setText("Mettre \u00e0 jour");
        if (a.getModeArrivee() != null) modeArriveeCombo.setValue(a.getModeArrivee());
        if (a.getMotifAdmission() != null) motifArea.setText(a.getMotifAdmission());
        if (a.getPrioriteInitiale() != null) prioriteCombo.setValue(a.getPrioriteInitiale());
        if (a.getNotes() != null && notesArea != null) notesArea.setText(a.getNotes());

        // Afficher le label ambulance si cette admission est liée à une ambulance
        if (a.getAmbulanceId() != null) {
            setLinkedAmbulanceId(a.getAmbulanceId());
        }

        // Si c'est une admission avec patient inconnu, pré-remplir les champs inconnu
        if (a.isPatientInconnu()) {
            patientInconnuCheck.setSelected(true);
            ambInfoBox.setVisible(true); ambInfoBox.setManaged(true);
            patientCombo.setDisable(true);
            patientInfoBox.setVisible(false); patientInfoBox.setManaged(false);
            mdiaBox.setVisible(false); mdiaBox.setManaged(false);

            // Pré-remplir depuis le patient existant en DB
            if (a.getPatientId() > 0) {
                try {
                    Patient existing = patientDAO.findById(a.getPatientId());
                    if (existing != null) {
                        if (inconnu_nomField != null) inconnu_nomField.setText(
                            existing.getNom() != null && !existing.getNom().equals("INCONNU") ? existing.getNom() : "");
                        if (inconnu_prenomField != null) inconnu_prenomField.setText(
                            existing.getPrenom() != null && !existing.getPrenom().equals("Patient") ? existing.getPrenom() : "");
                        if (inconnu_sexeCombo != null && existing.getSexe() != null)
                            inconnu_sexeCombo.setValue(existing.getSexe());
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Setter appelé depuis AdmissionCardsController pour indiquer l'ambulance liée.
     * Affiche un label informatif dans le formulaire.
     */
    public void setLinkedAmbulanceId(Integer ambulanceId) {
        this.linkedAmbulanceId = ambulanceId;
        if (ambulanceId != null && ambInfoBox != null) {
            // Afficher la section ambulance avec une note informative
            ambInfoBox.setVisible(true);
            ambInfoBox.setManaged(true);
            // Ajouter ou mettre à jour le label d'info ambulance dans la section ambInfoBox
            // On cherche si un label de liaison existe déjà, sinon on l'insère en haut
            boolean labelExists = ambInfoBox.getChildren().stream()
                .anyMatch(node -> node instanceof javafx.scene.control.Label
                    && ((javafx.scene.control.Label) node).getId() != null
                    && ((javafx.scene.control.Label) node).getId().equals("ambLinkLabel"));
            if (!labelExists) {
                javafx.scene.control.Label ambLabel = new javafx.scene.control.Label(
                    "Li\u00e9 \u00e0 une ambulance - informations ci-dessous (lecture seule)");
                ambLabel.setId("ambLinkLabel");
                ambLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0D629C; " +
                    "-fx-background-color: #DDF7E7; -fx-padding: 6 10 6 10; -fx-background-radius: 4px;");
                ambInfoBox.getChildren().add(0, ambLabel);
            }
        }
    }

    private void showFormError(String msg) {
        formError.setText(msg);
        formError.setVisible(true);
        formError.setManaged(true);
    }

    private void hideFormError() {
        formError.setVisible(false);
        formError.setManaged(false);
    }

    private void closeDialog() { ((Stage) saveBtn.getScene().getWindow()).close(); }

    public boolean isSaved() { return saved; }
}

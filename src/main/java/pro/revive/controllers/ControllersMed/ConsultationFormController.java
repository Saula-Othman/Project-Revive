package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.AdmissionService;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.DiagnosticSuggestionService;
import pro.revive.services.ServicesMed.PersonnelService;
import pro.revive.services.ServicesMed.SpeechToTextService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur du formulaire de consultation.
 * Fonctionne en deux modes :
 *   - Création  : appelé sans consultation préchargée (setConsultationPourModification non appelé)
 *   - Modification : appelé avec setConsultationPourModification(c)
 */
public class ConsultationFormController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────
    @FXML private Label labelTitre;

    // Sélection admission
    @FXML private ComboBox<String>  comboAdmission;

    // Infos patient (lecture seule)
    @FXML private Label labelNomPatient;
    @FXML private Label labelPrenomPatient;
    @FXML private Label labelDateNaissance;
    @FXML private Label labelMotif;

    // Sélection médecin
    @FXML private ComboBox<String>  comboMedecin;

    // Dates
    @FXML private DatePicker dateDebut;
    @FXML private TextField  heureDebut;   // format HH:mm
    @FXML private DatePicker dateFin;
    @FXML private TextField  heureFin;     // format HH:mm (optionnel)

    // Diagnostic & orientation
    @FXML private TextArea   textDiagnostic;
    @FXML private TextField  fieldSymptomes;
    @FXML private Button     btnSuggererIA;
    @FXML private Label      lblIaStatut;
    @FXML private ToggleGroup toggleOrientation;
    @FXML private RadioButton radioSortie;
    @FXML private RadioButton radioHospitalisation;
    @FXML private RadioButton radioTransfert;

    // ── Labels d'erreur inline ────────────────────────────────────────────
    @FXML private Label errAdmission;
    @FXML private Label errMedecin;
    @FXML private Label errDateDebut;
    @FXML private Label errHeureDebut;
    @FXML private Label errDateFin;
    @FXML private Label errHeureFin;
    @FXML private Label errSymptomes;
    @FXML private Label errDiagnostic;
    @FXML private Label errOrientation;
    @FXML private Label lblDiagCount;

    // ── Dictée vocale ─────────────────────────────────────────────────────
    @FXML private Button          btnMicro;
    @FXML private ComboBox<String> cbLanguage;

    // Boutons
    @FXML private Button btnEnregistrer;
    @FXML private Button btnAnnuler;

    // ── Services & données ────────────────────────────────────────────────
    private final ConsultationService         consultationService = new ConsultationService();
    private final DiagnosticSuggestionService iaService           = new DiagnosticSuggestionService();
    private final SpeechToTextService         speechService       = new SpeechToTextService();

    /** true si l'enregistrement vocal est actif */
    private boolean isRecording = false;

    /** Map médecin : "Prénom Nom" -> id_personnel */
    private final Map<String, Integer> medecinMap = new LinkedHashMap<>();

    /** Consultation en cours de modification (null = création). */
    private Consultation consultationAModifier = null;

    private static final DateTimeFormatter FMT_HEURE = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerAdmissions();
        chargerMedecins();
        presaisirDateHeure();
        configurerDatePickers();
        initialiserMicro();
        brancherValidationTempsReel();

        comboAdmission.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int idAdmission = AdmissionService.parseIdFromLabel(newVal);
                if (idAdmission > 0) {
                    afficherInfosPatient(idAdmission);
                } else {
                    viderInfosPatient();
                }
            } else {
                viderInfosPatient();
            }
        });
    }

    // ── Validation en temps réel ──────────────────────────────────────────

    /**
     * Branche un listener sur chaque champ pour valider immédiatement
     * pendant la saisie, sans attendre le clic sur Enregistrer.
     */
    private void brancherValidationTempsReel() {

        // Admission — obligatoire
        comboAdmission.valueProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                showError(comboAdmission, errAdmission, "Veuillez sélectionner une admission.");
            } else {
                showOk(comboAdmission, errAdmission);
            }
        });

        // Médecin — obligatoire
        comboMedecin.valueProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                showError(comboMedecin, errMedecin, "Veuillez sélectionner un médecin.");
            } else {
                showOk(comboMedecin, errMedecin);
            }
        });

        // Date début — obligatoire
        dateDebut.valueProperty().addListener((obs, o, n) -> {
            if (n == null) {
                showError(dateDebut, errDateDebut, "La date de début est obligatoire.");
            } else if (n.isAfter(LocalDate.now())) {
                showError(dateDebut, errDateDebut, "La date ne peut pas être dans le futur.");
            } else {
                showOk(dateDebut, errDateDebut);
                // Revérifier cohérence date fin si elle est déjà renseignée
                validerCoherenceDates();
            }
        });

        // Heure début — format HH:mm obligatoire
        heureDebut.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                showError(heureDebut, errHeureDebut, "L'heure de début est obligatoire.");
            } else if (!n.matches("\\d{2}:\\d{2}")) {
                showError(heureDebut, errHeureDebut, "Format requis : HH:mm (ex: 08:30).");
            } else {
                int h = Integer.parseInt(n.split(":")[0]);
                int m = Integer.parseInt(n.split(":")[1]);
                if (h > 23 || m > 59) {
                    showError(heureDebut, errHeureDebut, "Heure invalide (00:00 – 23:59).");
                } else {
                    showOk(heureDebut, errHeureDebut);
                }
            }
        });

        // Date fin — optionnelle mais si renseignée doit être >= date début
        dateFin.valueProperty().addListener((obs, o, n) -> {
            if (n != null) {
                validerCoherenceDates();
                // Si date fin renseignée, heure fin devient obligatoire
                validerHeureFin();
            } else {
                clearField(dateFin, errDateFin);
                clearField(heureFin, errHeureFin);
            }
        });

        // Heure fin — obligatoire seulement si date fin renseignée
        heureFin.textProperty().addListener((obs, o, n) -> {
            if (dateFin.getValue() != null) {
                validerHeureFin();
            } else if (n != null && !n.isBlank()) {
                // Heure fin saisie sans date fin
                showWarn(heureFin, errHeureFin, "Renseignez aussi la date de fin.");
            } else {
                clearField(heureFin, errHeureFin);
            }
        });

        // Symptômes — non obligatoire mais avertissement si vide (pour l'IA)
        fieldSymptomes.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank() && n.trim().length() < 5) {
                showWarn(fieldSymptomes, errSymptomes, "Décrivez les symptômes plus précisément.");
            } else {
                clearField(fieldSymptomes, errSymptomes);
            }
        });

        // Diagnostic — obligatoire, min 10 caractères, max 500
        textDiagnostic.textProperty().addListener((obs, o, n) -> {
            int len = n == null ? 0 : n.trim().length();
            // Compteur de caractères
            if (lblDiagCount != null) {
                lblDiagCount.setText(len + " / 500");
                if (len > 450) {
                    lblDiagCount.getStyleClass().setAll("char-counter-warn");
                } else if (len >= 500) {
                    lblDiagCount.getStyleClass().setAll("char-counter-max");
                } else {
                    lblDiagCount.getStyleClass().setAll("char-counter");
                }
            }
            // Validation
            if (len == 0) {
                showError(textDiagnostic, errDiagnostic, "Le diagnostic est obligatoire.");
            } else if (len < 10) {
                showError(textDiagnostic, errDiagnostic, "Minimum 10 caractères requis (" + len + "/10).");
            } else if (len > 500) {
                showError(textDiagnostic, errDiagnostic, "Maximum 500 caractères atteint.");
                // Tronquer automatiquement
                textDiagnostic.setText(n.substring(0, 500));
            } else {
                showOk(textDiagnostic, errDiagnostic);
            }
        });

        // Orientation — obligatoire, vérifiée au changement de sélection
        toggleOrientation.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n == null) {
                showErrorRadio(errOrientation, "Veuillez choisir une orientation.");
            } else {
                hideLabel(errOrientation);
                radioSortie.setStyle("");
                radioHospitalisation.setStyle("");
                radioTransfert.setStyle("");
            }
        });
    }

    /** Vérifie que date fin >= date début. */
    private void validerCoherenceDates() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin   = dateFin.getValue();
        if (debut != null && fin != null) {
            if (fin.isBefore(debut)) {
                showError(dateFin, errDateFin, "La date de fin doit être après la date de début.");
            } else if (fin.isAfter(LocalDate.now())) {
                showError(dateFin, errDateFin, "La date de fin ne peut pas être dans le futur.");
            } else {
                showOk(dateFin, errDateFin);
            }
        }
    }

    /** Vérifie le format de l'heure de fin si date fin est renseignée. */
    private void validerHeureFin() {
        String val = heureFin.getText();
        if (val == null || val.isBlank()) {
            showError(heureFin, errHeureFin, "L'heure de fin est requise si une date de fin est saisie.");
        } else if (!val.matches("\\d{2}:\\d{2}")) {
            showError(heureFin, errHeureFin, "Format requis : HH:mm (ex: 14:30).");
        } else {
            int h = Integer.parseInt(val.split(":")[0]);
            int m = Integer.parseInt(val.split(":")[1]);
            if (h > 23 || m > 59) {
                showError(heureFin, errHeureFin, "Heure invalide (00:00 – 23:59).");
            } else {
                showOk(heureFin, errHeureFin);
            }
        }
    }

    // ── Helpers visuels ───────────────────────────────────────────────────

    /** Affiche un message d'erreur rouge sous le champ et colore sa bordure. */
    private void showError(Control field, Label errLabel, String message) {
        field.getStyleClass().removeAll("field-valid", "field-neutral", "field-invalid");
        field.getStyleClass().add("field-invalid");
        if (errLabel != null) {
            errLabel.setText("  " + message);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
            errLabel.getStyleClass().setAll("validation-error");
        }
    }

    /** Affiche un avertissement orange (non bloquant). */
    private void showWarn(Control field, Label errLabel, String message) {
        field.getStyleClass().removeAll("field-valid", "field-neutral", "field-invalid");
        if (errLabel != null) {
            errLabel.setText("  " + message);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
            errLabel.getStyleClass().setAll("validation-warning");
        }
    }

    /** Affiche une bordure verte — champ valide. */
    private void showOk(Control field, Label errLabel) {
        field.getStyleClass().removeAll("field-valid", "field-neutral", "field-invalid");
        field.getStyleClass().add("field-valid");
        hideLabel(errLabel);
    }

    /** Efface l'état de validation d'un champ. */
    private void clearField(Control field, Label errLabel) {
        field.getStyleClass().removeAll("field-valid", "field-neutral", "field-invalid");
        hideLabel(errLabel);
    }

    /** Erreur sur les radio buttons (pas de Control à colorer). */
    private void showErrorRadio(Label errLabel, String message) {
        String style = "-fx-border-color: #EF4444; -fx-border-width: 1.5px; " +
                       "-fx-border-radius: 6px; -fx-padding: 3px 8px;";
        radioSortie.setStyle(style);
        radioHospitalisation.setStyle(style);
        radioTransfert.setStyle(style);
        if (errLabel != null) {
            errLabel.setText("  " + message);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
            errLabel.getStyleClass().setAll("validation-error");
        }
    }

    /** Cache un label d'erreur. */
    private void hideLabel(Label lbl) {
        if (lbl != null) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    /** Initialise le ComboBox de langue et l'état du bouton micro. */
    private void initialiserMicro() {
        if (cbLanguage != null) {
            cbLanguage.setItems(javafx.collections.FXCollections.observableArrayList("fr-FR", "ar-TN"));
            cbLanguage.setValue("fr-FR");
        }
        if (btnMicro != null) {
            btnMicro.setText("▶ Démarrer");
            btnMicro.setStyle(styleMicroNormal());
        }
    }

    /** Limite les DatePicker : pas de date future, pas avant 1 an en arrière. */
    private void configurerDatePickers() {
        LocalDate today    = LocalDate.now();
        LocalDate minDate  = today.minusYears(1);

        // Date début : entre -1 an et aujourd'hui
        dateDebut.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(today) || date.isBefore(minDate));
                if (date.isAfter(today) || date.isBefore(minDate))
                    setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #CBD5E1;");
            }
        });

        // Date fin : entre -1 an et aujourd'hui
        dateFin.setDayCellFactory(picker -> new javafx.scene.control.DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.isAfter(today) || date.isBefore(minDate));
                if (date.isAfter(today) || date.isBefore(minDate))
                    setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #CBD5E1;");
            }
        });
    }

    /** Charge la liste des admissions actives dans la ComboBox. */
    private void chargerAdmissions() {
        try {
            List<String> admissions = AdmissionService.getAllActiveAdmissions();
            comboAdmission.setItems(FXCollections.observableArrayList(admissions));
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les admissions :\n" + e.getMessage());
        }
    }

    /** Charge la liste des médecins urgentistes dans la ComboBox. */
    private void chargerMedecins() {
        try {
            Map<Integer, String> medecins = PersonnelService.getMedecins();
            medecinMap.clear();
            List<String> noms = new ArrayList<>();
            medecins.forEach((id, nom) -> {
                medecinMap.put(nom, id);
                noms.add(nom);
            });
            comboMedecin.setItems(FXCollections.observableArrayList(noms));
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible de charger les médecins :\n" + e.getMessage());
        }
    }

    /** Pré-remplit la date/heure de début avec l'instant courant. */
    private void presaisirDateHeure() {
        LocalDateTime now = LocalDateTime.now();
        dateDebut.setValue(now.toLocalDate());
        heureDebut.setText(now.format(FMT_HEURE));
    }

    /** Affiche les informations du patient pour l'admission sélectionnée. */
    private void afficherInfosPatient(int idAdmission) {
        try {
            Map<String, Object> details = AdmissionService.getAdmissionDetails(idAdmission);
            if (details.isEmpty()) {
                viderInfosPatient();
                return;
            }
            
            labelNomPatient.setText(str(details.get("nom")));
            labelPrenomPatient.setText(str(details.get("prenom")));
            Object dn = details.get("dateNaissance");
            labelDateNaissance.setText(dn != null ? dn.toString() : "—");
            labelMotif.setText(str(details.get("motif")));
        } catch (Exception e) {
            viderInfosPatient();
        }
    }

    private void viderInfosPatient() {
        labelNomPatient.setText("—");
        labelPrenomPatient.setText("—");
        labelDateNaissance.setText("—");
        labelMotif.setText("—");
    }

    // ── Mode modification ─────────────────────────────────────────────────

    /**
     * Pré-sélectionne une admission dans le formulaire (depuis le triage).
     * Doit être appelé après initialize().
     */
    public void setAdmissionPreselect(int idAdmission) {
        for (String item : comboAdmission.getItems()) {
            if (AdmissionService.parseIdFromLabel(item) == idAdmission) {
                comboAdmission.setValue(item);
                comboAdmission.setDisable(true); // verrouiller l'admission
                break;
            }
        }
    }

    /**
     * Pré-remplit le formulaire avec les données d'une consultation existante.
     * Doit être appelé après initialize().
     */
    public void setConsultationPourModification(Consultation c) {
        this.consultationAModifier = c;
        labelTitre.setText("Modifier la consultation #" + c.getIdConsultation());
        btnEnregistrer.setText("Mettre à jour");

        // Admission : cherche l'entrée correspondante dans la ComboBox
        for (String item : comboAdmission.getItems()) {
            if (AdmissionService.parseIdFromLabel(item) == c.getIdAdmission()) {
                comboAdmission.setValue(item);
                break;
            }
        }
        // En mode modification, l'admission ne doit pas être changée
        comboAdmission.setDisable(true);

        // Médecin
        String nomMedecin = PersonnelService.getNomMedecinById(c.getIdPersonnelMedecin());
        if (!nomMedecin.isEmpty()) comboMedecin.setValue(nomMedecin);

        // Date/heure début
        if (c.getDateHeureDebut() != null) {
            dateDebut.setValue(c.getDateHeureDebut().toLocalDate());
            heureDebut.setText(c.getDateHeureDebut().format(FMT_HEURE));
        }

        // Date/heure fin (optionnelle)
        if (c.getDateHeureFin() != null) {
            dateFin.setValue(c.getDateHeureFin().toLocalDate());
            heureFin.setText(c.getDateHeureFin().format(FMT_HEURE));
        }

        // Diagnostic
        textDiagnostic.setText(c.getDiagnostic() != null ? c.getDiagnostic() : "");

        // Orientation
        if (c.getOrientation() != null) {
            switch (c.getOrientation()) {
                case "Sortie"          -> radioSortie.setSelected(true);
                case "Hospitalisation" -> radioHospitalisation.setSelected(true);
                case "Transfert"       -> radioTransfert.setSelected(true);
            }
        }
    }

    // ── Dictée vocale ─────────────────────────────────────────────────────

    /**
     * Démarre ou arrête la dictée vocale.
     * Le texte reconnu est inséré dans le champ qui a le focus (TextField ou TextArea).
     */
    @FXML
    private void toggleMicro() {
        if (isRecording) {
            // ── Arrêter ───────────────────────────────────────────────────
            speechService.stopListening();
            isRecording = false;
            if (btnMicro != null) {
                btnMicro.setText("▶ Démarrer");
                btnMicro.setStyle(styleMicroNormal());
            }
            if (lblIaStatut != null) {
                lblIaStatut.setText("Dictée arrêtée.");
                lblIaStatut.setStyle("-fx-text-fill: #64748B; -fx-font-size: 11px;");
            }
        } else {
            // ── Vérifier disponibilité Vosk avant de démarrer ─────────────
            if (!SpeechToTextService.isVoskDisponible()) {
                if (lblIaStatut != null) {
                    lblIaStatut.setText("⚠ Vosk non disponible.");
                    lblIaStatut.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
                }
                return;
            }

            String langue = (cbLanguage != null && cbLanguage.getValue() != null)
                ? cbLanguage.getValue() : "ar-TN";

            isRecording = true;
            if (btnMicro != null) {
                btnMicro.setText("■ Arrêter");
                btnMicro.setStyle(styleMicroActif());
            }
            if (lblIaStatut != null) {
                lblIaStatut.setText("Dictée en cours (" + langue + ")...");
                lblIaStatut.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px; -fx-font-weight: bold;");
            }

            // Démarrer l'écoute avec callback succès et erreur
            speechService.startListening(langue,
                // ── Succès : insérer dans le champ focalisé ───────────────
                texteReconnu -> {
                    Node focused = btnMicro.getScene() != null
                        ? btnMicro.getScene().getFocusOwner() : null;
                    if (focused instanceof TextField tf) {
                        tf.insertText(tf.getCaretPosition(), texteReconnu);
                    } else if (focused instanceof TextArea ta) {
                        ta.insertText(ta.getCaretPosition(), texteReconnu);
                    } else if (fieldSymptomes != null) {
                        fieldSymptomes.appendText(texteReconnu);
                    }
                },
                // ── Erreur : afficher et réinitialiser le bouton ──────────
                errMsg -> {
                    isRecording = false;
                    if (btnMicro != null) {
                        btnMicro.setText("▶ Démarrer");
                        btnMicro.setStyle(styleMicroNormal());
                    }
                    if (lblIaStatut != null) {
                        lblIaStatut.setText("Erreur : " + errMsg);
                        lblIaStatut.setStyle("-fx-text-fill: #C0392B; -fx-font-size: 11px;");
                    }
                }
            );
        }
    }

    /** Style bouton micro au repos */
    private String styleMicroNormal() {
        return "-fx-background-color: white; -fx-text-fill: #0B4EA2;" +
               "-fx-border-color: #0B4EA2; -fx-border-width: 1.5px;" +
               "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
               "-fx-font-size: 14px; -fx-padding: 6px 14px; -fx-cursor: hand;";
    }

    /** Style bouton micro actif (rouge pulsant) */
    private String styleMicroActif() {
        return "-fx-background-color: #C0392B; -fx-text-fill: white;" +
               "-fx-border-color: #C0392B; -fx-border-width: 1.5px;" +
               "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
               "-fx-font-size: 12px; -fx-font-weight: bold;" +
               "-fx-padding: 6px 14px; -fx-cursor: hand;" +
               "-fx-effect: dropshadow(gaussian, rgba(192,57,43,0.45), 10, 0, 0, 2);";
    }

    // ── Suggestion IA ─────────────────────────────────────────────────────

    /**
     * Appelle Groq IA en arriere-plan et remplit le champ diagnostic.
     * Utilise le motif de consultation + symptomes pour une suggestion plus precise.
     * Le bouton est desactive pendant l'appel pour eviter les doubles clics.
     */
    @FXML
    private void handleSuggestionDiagnostic() {
        String symptomes = fieldSymptomes != null ? fieldSymptomes.getText().trim() : "";
        if (symptomes.isEmpty()) {
            afficherErreur("Symptomes manquants",
                "Veuillez saisir les symptomes du patient avant de demander une suggestion.");
            return;
        }

        // Recuperer le motif de consultation depuis l'admission selectionnee
        String motifConsultation = labelMotif != null ? labelMotif.getText() : null;
        if (motifConsultation != null && motifConsultation.equals("\u2014")) {
            motifConsultation = null; // Ignorer le placeholder "—"
        }

        // Desactiver le bouton + afficher indicateur
        if (btnSuggererIA  != null) btnSuggererIA.setDisable(true);
        if (lblIaStatut    != null) {
            lblIaStatut.setText("Analyse en cours (IA Groq)...");
            lblIaStatut.setStyle("-fx-text-fill: #0ea5a0; -fx-font-size: 11px;");
        }
        if (textDiagnostic != null) textDiagnostic.setDisable(true);

        // Appel asynchrone — ne bloque pas le thread JavaFX
        final String motifFinal = motifConsultation;
        Thread thread = new Thread(() -> {
            try {
                String suggestion = iaService.suggerer(symptomes, motifFinal);

                Platform.runLater(() -> {
                    textDiagnostic.setText(suggestion);
                    textDiagnostic.setDisable(false);
                    if (btnSuggererIA != null) btnSuggererIA.setDisable(false);
                    if (lblIaStatut   != null) {
                        lblIaStatut.setText("Suggestion IA appliquee. Verifiez et adaptez.");
                        lblIaStatut.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    textDiagnostic.setDisable(false);
                    if (btnSuggererIA != null) btnSuggererIA.setDisable(false);
                    if (lblIaStatut   != null) {
                        lblIaStatut.setText("Erreur : " + e.getMessage());
                        lblIaStatut.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
                    }
                    System.err.println("[DiagnosticIA] Erreur : " + e.getMessage());
                });
            }
        });
        thread.setDaemon(true);
        thread.setName("diagnostic-ia");
        thread.start();
    }

    // ── Actions boutons ───────────────────────────────────────────────────

    /** Valide et enregistre (création ou modification). */
    @FXML
    private void onEnregistrer() {
        if (!validerSaisie()) return;

        try {
            Consultation c = construireConsultation();

            if (consultationAModifier == null) {
                // ── Création ──
                consultationService.addEntity(c);
                afficherInfo("Succès", "Consultation créée avec succès (ID : " + c.getIdConsultation() + ").");
            } else {
                // ── Modification ──
                c.setIdConsultation(consultationAModifier.getIdConsultation());
                consultationService.updateEntity(c.getIdConsultation(), c);
                afficherInfo("Succès", "Consultation #" + c.getIdConsultation() + " mise à jour.");
            }
            fermer();
        } catch (Exception e) {
            afficherErreur("Erreur d'enregistrement", "Une erreur est survenue :\n" + e.getMessage());
        }
    }

    /** Ferme la fenêtre sans enregistrer. */
    @FXML
    private void onAnnuler() {
        fermer();
    }

    // ── Validation au clic Enregistrer ───────────────────────────────────

    private boolean validerSaisie() {
        boolean valid = true;

        // Admission
        if (comboAdmission.getValue() == null || comboAdmission.getValue().isBlank()) {
            showError(comboAdmission, errAdmission, "Veuillez sélectionner une admission.");
            valid = false;
        }

        // Médecin
        if (comboMedecin.getValue() == null || comboMedecin.getValue().isBlank()) {
            showError(comboMedecin, errMedecin, "Veuillez sélectionner un médecin.");
            valid = false;
        }

        // Date début
        if (dateDebut.getValue() == null) {
            showError(dateDebut, errDateDebut, "La date de début est obligatoire.");
            valid = false;
        } else if (dateDebut.getValue().isAfter(LocalDate.now())) {
            showError(dateDebut, errDateDebut, "La date ne peut pas être dans le futur.");
            valid = false;
        }

        // Heure début
        String hd = heureDebut.getText();
        if (hd == null || hd.isBlank()) {
            showError(heureDebut, errHeureDebut, "L'heure de début est obligatoire.");
            valid = false;
        } else if (!hd.matches("\\d{2}:\\d{2}")) {
            showError(heureDebut, errHeureDebut, "Format requis : HH:mm (ex: 08:30).");
            valid = false;
        } else {
            int h = Integer.parseInt(hd.split(":")[0]);
            int m = Integer.parseInt(hd.split(":")[1]);
            if (h > 23 || m > 59) {
                showError(heureDebut, errHeureDebut, "Heure invalide (00:00 – 23:59).");
                valid = false;
            }
        }

        // Date fin + heure fin (optionnelles mais cohérentes)
        if (dateFin.getValue() != null || (heureFin.getText() != null && !heureFin.getText().isBlank())) {
            if (dateFin.getValue() == null) {
                showError(dateFin, errDateFin, "Renseignez la date de fin.");
                valid = false;
            } else if (dateDebut.getValue() != null && dateFin.getValue().isBefore(dateDebut.getValue())) {
                showError(dateFin, errDateFin, "La date de fin doit être après la date de début.");
                valid = false;
            } else if (dateFin.getValue().isAfter(LocalDate.now())) {
                showError(dateFin, errDateFin, "La date de fin ne peut pas être dans le futur.");
                valid = false;
            }
            String hf = heureFin.getText();
            if (hf == null || hf.isBlank()) {
                showError(heureFin, errHeureFin, "L'heure de fin est requise.");
                valid = false;
            } else if (!hf.matches("\\d{2}:\\d{2}")) {
                showError(heureFin, errHeureFin, "Format requis : HH:mm (ex: 14:30).");
                valid = false;
            } else {
                int h = Integer.parseInt(hf.split(":")[0]);
                int m = Integer.parseInt(hf.split(":")[1]);
                if (h > 23 || m > 59) {
                    showError(heureFin, errHeureFin, "Heure invalide (00:00 – 23:59).");
                    valid = false;
                }
            }
        }

        // Diagnostic
        String diag = textDiagnostic.getText() == null ? "" : textDiagnostic.getText().trim();
        if (diag.isEmpty()) {
            showError(textDiagnostic, errDiagnostic, "Le diagnostic est obligatoire.");
            valid = false;
        } else if (diag.length() < 10) {
            showError(textDiagnostic, errDiagnostic, "Minimum 10 caractères requis (" + diag.length() + "/10).");
            valid = false;
        } else if (diag.length() > 500) {
            showError(textDiagnostic, errDiagnostic, "Maximum 500 caractères atteint.");
            valid = false;
        }

        // Orientation
        if (toggleOrientation.getSelectedToggle() == null) {
            showErrorRadio(errOrientation, "Veuillez choisir une orientation.");
            valid = false;
        }

        return valid;
    }

    private void setFieldError(Control field, String tooltip) {
        showError(field, null, tooltip);
        Tooltip t = new Tooltip("  " + tooltip);
        t.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
        Tooltip.install(field, t);
        field.setOnMouseClicked(e -> resetFieldStyle(field));
        if (field instanceof TextField tf)
            tf.textProperty().addListener((obs, o, n) -> resetFieldStyle(field));
        if (field instanceof TextArea ta)
            ta.textProperty().addListener((obs, o, n) -> resetFieldStyle(field));
        if (field instanceof ComboBox<?> cb)
            cb.valueProperty().addListener((obs, o, n) -> resetFieldStyle(field));
        if (field instanceof DatePicker dp)
            dp.valueProperty().addListener((obs, o, n) -> resetFieldStyle(field));
    }

    private void resetFieldStyle(Control field) {
        clearField(field, null);
        Tooltip.install(field, null);
    }

    // ── Construction de l'entité ──────────────────────────────────────────

    /** Construit un objet Consultation à partir des champs du formulaire. */
    private Consultation construireConsultation() {
        Consultation c = new Consultation();

        // Admission
        int idAdmission = AdmissionService.parseIdFromLabel(comboAdmission.getValue());
        c.setIdAdmission(idAdmission);

        // Médecin
        String nomMedecin = comboMedecin.getValue();
        int idMedecin = medecinMap.getOrDefault(nomMedecin, 0);
        c.setIdPersonnelMedecin(idMedecin);

        // Date/heure début
        LocalDate d = dateDebut.getValue();
        String[] hParts = heureDebut.getText().split(":");
        c.setDateHeureDebut(LocalDateTime.of(d.getYear(), d.getMonth(), d.getDayOfMonth(),
            Integer.parseInt(hParts[0]), Integer.parseInt(hParts[1])));

        // Date/heure fin (optionnelle)
        if (dateFin.getValue() != null && heureFin.getText() != null && !heureFin.getText().isBlank()) {
            LocalDate df = dateFin.getValue();
            String[] hfParts = heureFin.getText().split(":");
            c.setDateHeureFin(LocalDateTime.of(df.getYear(), df.getMonth(), df.getDayOfMonth(),
                Integer.parseInt(hfParts[0]), Integer.parseInt(hfParts[1])));
        }

        // Diagnostic
        c.setDiagnostic(textDiagnostic.getText().trim());

        // Orientation
        RadioButton selected = (RadioButton) toggleOrientation.getSelectedToggle();
        c.setOrientation(selected.getText());

        return c;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────

    private Runnable onCloseCallback;
    public void setOnCloseCallback(Runnable cb) { this.onCloseCallback = cb; }

    private void fermer() {
        // Arrêter la dictée vocale si active
        if (isRecording) {
            speechService.stopListening();
            isRecording = false;
        }
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else {
            ((Stage) btnAnnuler.getScene().getWindow()).close();
        }
    }

    private String str(Object o) {
        return o != null ? o.toString() : "—";
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void afficherInfo(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Amélioration du style
        alert.getDialogPane().setPrefWidth(450);
        alert.getDialogPane().setPrefHeight(200);
        
        // Ajouter une icône personnalisée
        Label contentLabel = new Label(message);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #1e293b; -fx-wrap-text: true;");
        contentLabel.setWrapText(true);
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f0fdf4; -fx-border-color: #22c55e; -fx-border-width: 2; -fx-border-radius: 8;");
        
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 32px;");
        
        Label titleLabel = new Label(titre);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #16a34a;");
        
        content.getChildren().addAll(icon, titleLabel, contentLabel);
        content.setAlignment(Pos.TOP_CENTER);
        
        alert.getDialogPane().setContent(content);
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/ResourcesMed/module3/css/revive-dark.css").toExternalForm());
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-padding: 0;");
        } catch (Exception ignored) {}
    }
}

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
import javafx.scene.Node;
import javafx.scene.control.*;
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

        comboAdmission.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int idAdmission = AdmissionService.parseIdFromLabel(newVal);
                if (idAdmission > 0) afficherInfosPatient(idAdmission);
            } else {
                viderInfosPatient();
            }
        });
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

    // ── Validation visuelle inline ────────────────────────────────────────

    private boolean validerSaisie() {
        boolean valid = true;

        // Reset tous les styles
        resetFieldStyle(comboAdmission);
        resetFieldStyle(comboMedecin);
        resetFieldStyle(dateDebut);
        resetFieldStyle(heureDebut);
        resetFieldStyle(textDiagnostic);

        // Admission
        if (comboAdmission.getValue() == null || comboAdmission.getValue().isBlank()) {
            setFieldError(comboAdmission, "Admission obligatoire");
            valid = false;
        }

        // Médecin
        if (comboMedecin.getValue() == null || comboMedecin.getValue().isBlank()) {
            setFieldError(comboMedecin, "Médecin obligatoire");
            valid = false;
        }

        // Date début
        if (dateDebut.getValue() == null) {
            setFieldError(dateDebut, "Date obligatoire");
            valid = false;
        }

        // Heure début
        if (heureDebut.getText() == null || !heureDebut.getText().matches("\\d{2}:\\d{2}")) {
            setFieldError(heureDebut, "Format HH:mm requis");
            valid = false;
        }

        // Diagnostic
        if (textDiagnostic.getText() == null || textDiagnostic.getText().trim().isEmpty()) {
            setFieldError(textDiagnostic, "Diagnostic obligatoire");
            valid = false;
        }

        // Orientation
        if (toggleOrientation.getSelectedToggle() == null) {
            radioSortie.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-padding: 4px 8px;");
            radioHospitalisation.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-padding: 4px 8px;");
            radioTransfert.setStyle("-fx-border-color: #EF4444; -fx-border-width: 2px; -fx-border-radius: 6px; -fx-padding: 4px 8px;");
            valid = false;
        } else {
            radioSortie.setStyle("");
            radioHospitalisation.setStyle("");
            radioTransfert.setStyle("");
        }

        // Heure fin si date fin renseignée
        if (dateFin.getValue() != null || (heureFin.getText() != null && !heureFin.getText().isBlank())) {
            if (dateFin.getValue() == null) {
                setFieldError(dateFin, "Date fin obligatoire");
                valid = false;
            }
            if (heureFin.getText() == null || !heureFin.getText().matches("\\d{2}:\\d{2}")) {
                setFieldError(heureFin, "Format HH:mm requis");
                valid = false;
            }
        }

        return valid;
    }

    private void setFieldError(Control field, String tooltip) {
        field.setStyle(
            "-fx-border-color: #EF4444; -fx-border-width: 2px; " +
            "-fx-border-radius: 8px; -fx-background-radius: 8px; " +
            "-fx-background-color: #FFF5F5;"
        );
        Tooltip t = new Tooltip("⚠ " + tooltip);
        t.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
        Tooltip.install(field, t);

        // Retirer l'erreur dès que l'utilisateur interagit
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
        field.setStyle("");
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
        styleAlert(alert);
        alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/ResourcesMed/module3/css/revive-dark.css").toExternalForm());
            alert.getDialogPane().setStyle("-fx-background-color: #1e293b;");
        } catch (Exception ignored) {}
    }
}

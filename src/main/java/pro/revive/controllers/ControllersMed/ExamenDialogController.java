package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.ConsultationService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controleur du dialog "Demande d'examens".
 * Permet au medecin de saisir les analyses biologiques et imageries
 * pour une consultation donnee, avec validation en temps reel.
 */
public class ExamenDialogController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────────────
    @FXML private Label   labelTitre;
    @FXML private Label   labelPatient;
    @FXML private Label   labelConsultation;
    @FXML private Label   labelStatut;
    @FXML private HBox    boxStatut;
    @FXML private TextArea textAnalyses;
    @FXML private TextArea textImageries;

    // ── Labels de validation ──────────────────────────────────────────────
    @FXML private Label errGlobal;
    @FXML private Label errAnalyses;
    @FXML private Label errImageries;
    @FXML private Label lblAnalysesCount;
    @FXML private Label lblImageriesCount;

    // ── Constantes ────────────────────────────────────────────────────────
    private static final int MAX_CHARS  = 300;
    private static final int MIN_CHARS  = 3;

    /**
     * Regex : au moins une lettre (évite les saisies purement numériques
     * ou les suites de caractères aléatoires sans espace ni virgule).
     * Accepte : lettres, chiffres, espaces, virgules, tirets, parenthèses.
     */
    private static final String PATTERN_VALIDE =
            "^[a-zA-ZÀ-ÿ0-9 ,;.()\\-/+]+$";

    private final ConsultationService cs = new ConsultationService();
    private Consultation consultation;

    // ── Initialisation ────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        brancherValidationTempsReel();
    }

    public void setConsultation(Consultation c) {
        this.consultation = c;

        labelPatient.setText(nvl(c.getNomPatient()));
        labelConsultation.setText("Consultation #" + c.getIdConsultation());

        if (c.getAnalyses()  != null) textAnalyses.setText(c.getAnalyses());
        if (c.getImageries() != null) textImageries.setText(c.getImageries());

        mettreAJourStatutAffichage(c.getStatutDemande());
    }

    private void mettreAJourStatutAffichage(String statut) {
        if ("Envoyee".equals(statut)) {
            labelStatut.setText("Demande envoyee");
            labelStatut.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #16a34a;");
            boxStatut.setStyle("-fx-background-color: #f0fdf4; -fx-background-radius: 8;"
                + "-fx-border-color: #bbf7d0; -fx-border-radius: 8; -fx-padding: 10 14;");
        } else {
            labelStatut.setText("Non envoyee");
            labelStatut.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
            boxStatut.setStyle("-fx-background-color: #f9fafb; -fx-background-radius: 8;"
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-padding: 10 14;");
        }
    }

    // ── Validation en temps réel ──────────────────────────────────────────

    private void brancherValidationTempsReel() {

        // Analyses biologiques
        textAnalyses.textProperty().addListener((obs, o, n) -> {
            mettreAJourCompteur(lblAnalysesCount, n, MAX_CHARS);
            validerChamp(textAnalyses, errAnalyses, n, "analyses biologiques");
            cacherErreurGlobale();
        });

        // Imageries médicales
        textImageries.textProperty().addListener((obs, o, n) -> {
            mettreAJourCompteur(lblImageriesCount, n, MAX_CHARS);
            validerChamp(textImageries, errImageries, n, "imageries médicales");
            cacherErreurGlobale();
        });
    }

    /**
     * Met à jour le compteur de caractères et change sa couleur.
     */
    private void mettreAJourCompteur(Label compteur, String texte, int max) {
        if (compteur == null) return;
        int len = texte == null ? 0 : texte.length();
        compteur.setText(len + " / " + max);
        compteur.getStyleClass().setAll(
            len >= max       ? "char-counter-max"  :
            len >= max * 0.85 ? "char-counter-warn" :
                               "char-counter"
        );
    }

    /**
     * Valide un champ TextArea en temps réel :
     * - vide → neutre (pas d'erreur, l'autre champ peut suffire)
     * - trop court (< MIN_CHARS) → erreur
     * - trop long (> MAX_CHARS) → erreur + troncature automatique
     * - caractères invalides → erreur
     * - contenu valide → bordure verte
     */
    private void validerChamp(TextArea champ, Label errLabel, String texte, String nomChamp) {
        String val = texte == null ? "" : texte.trim();

        // Vide → neutre, pas d'erreur (l'autre champ peut suffire)
        if (val.isEmpty()) {
            setNeutre(champ, errLabel);
            return;
        }

        // Trop long → tronquer automatiquement
        if (texte != null && texte.length() > MAX_CHARS) {
            champ.setText(texte.substring(0, MAX_CHARS));
            showErreur(champ, errLabel, "Maximum " + MAX_CHARS + " caractères atteint.");
            return;
        }

        // Trop court
        if (val.length() < MIN_CHARS) {
            showErreur(champ, errLabel,
                "Saisie trop courte — décrivez les " + nomChamp + " (min. " + MIN_CHARS + " car.).");
            return;
        }

        // Caractères invalides (suite aléatoire sans lettre lisible)
        if (!val.matches(PATTERN_VALIDE)) {
            showErreur(champ, errLabel,
                "Caractères non autorisés. Utilisez des lettres, chiffres, virgules.");
            return;
        }

        // Détection de saisie aléatoire : pas d'espace ni virgule sur > 15 car.
        if (val.length() > 15 && !val.contains(" ") && !val.contains(",")) {
            showAvertissement(champ, errLabel,
                "Saisie suspecte — séparez les examens par des virgules (ex: NFS, CRP).");
            return;
        }

        // Tout est bon
        showOk(champ, errLabel);
    }

    // ── Validation au clic Envoyer ────────────────────────────────────────

    private boolean validerAvantEnvoi() {
        String analyses  = textAnalyses.getText()  == null ? "" : textAnalyses.getText().trim();
        String imageries = textImageries.getText() == null ? "" : textImageries.getText().trim();

        // Au moins un champ doit être rempli
        if (analyses.isEmpty() && imageries.isEmpty()) {
            showErreurGlobale("Veuillez renseigner au moins un champ : analyses ou imageries.");
            return false;
        }

        boolean valid = true;

        // Valider chaque champ rempli
        if (!analyses.isEmpty()) {
            if (analyses.length() < MIN_CHARS) {
                showErreur(textAnalyses, errAnalyses,
                    "Saisie trop courte (min. " + MIN_CHARS + " caractères).");
                valid = false;
            } else if (!analyses.matches(PATTERN_VALIDE)) {
                showErreur(textAnalyses, errAnalyses,
                    "Caractères non autorisés dans les analyses.");
                valid = false;
            } else if (analyses.length() > 15 && !analyses.contains(" ") && !analyses.contains(",")) {
                showAvertissement(textAnalyses, errAnalyses,
                    "Saisie suspecte — séparez les examens par des virgules.");
                valid = false;
            }
        }

        if (!imageries.isEmpty()) {
            if (imageries.length() < MIN_CHARS) {
                showErreur(textImageries, errImageries,
                    "Saisie trop courte (min. " + MIN_CHARS + " caractères).");
                valid = false;
            } else if (!imageries.matches(PATTERN_VALIDE)) {
                showErreur(textImageries, errImageries,
                    "Caractères non autorisés dans les imageries.");
                valid = false;
            } else if (imageries.length() > 15 && !imageries.contains(" ") && !imageries.contains(",")) {
                showAvertissement(textImageries, errImageries,
                    "Saisie suspecte — séparez les examens par des virgules.");
                valid = false;
            }
        }

        return valid;
    }

    // ── Helpers visuels ───────────────────────────────────────────────────

    private void showErreur(TextArea champ, Label errLabel, String message) {
        champ.getStyleClass().removeAll("field-valid", "field-invalid");
        champ.getStyleClass().add("field-invalid");
        if (errLabel != null) {
            errLabel.setText("  " + message);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
            errLabel.getStyleClass().setAll("validation-error");
        }
    }

    private void showAvertissement(TextArea champ, Label errLabel, String message) {
        champ.getStyleClass().removeAll("field-valid", "field-invalid");
        if (errLabel != null) {
            errLabel.setText("  " + message);
            errLabel.setVisible(true);
            errLabel.setManaged(true);
            errLabel.getStyleClass().setAll("validation-warning");
        }
    }

    private void showOk(TextArea champ, Label errLabel) {
        champ.getStyleClass().removeAll("field-valid", "field-invalid");
        champ.getStyleClass().add("field-valid");
        cacher(errLabel);
    }

    private void setNeutre(TextArea champ, Label errLabel) {
        champ.getStyleClass().removeAll("field-valid", "field-invalid");
        cacher(errLabel);
    }

    private void cacher(Label lbl) {
        if (lbl != null) {
            lbl.setText("");
            lbl.setVisible(false);
            lbl.setManaged(false);
        }
    }

    private void showErreurGlobale(String message) {
        if (errGlobal != null) {
            errGlobal.setText("  " + message);
            errGlobal.setVisible(true);
            errGlobal.setManaged(true);
        }
    }

    private void cacherErreurGlobale() {
        cacher(errGlobal);
    }

    // ── Raccourcis analyses ───────────────────────────────────────────────

    @FXML private void ajouterNFS()           { appendAnalyse("NFS"); }
    @FXML private void ajouterCRP()           { appendAnalyse("CRP"); }
    @FXML private void ajouterTroponines()    { appendAnalyse("Troponines"); }
    @FXML private void ajouterBilanHepatique(){ appendAnalyse("Bilan hepatique"); }
    @FXML private void ajouterGlycemie()      { appendAnalyse("Glycemie"); }

    // ── Raccourcis imageries ──────────────────────────────────────────────

    @FXML private void ajouterRadioThorax()    { appendImagerie("Radio thorax"); }
    @FXML private void ajouterScannerCerebral(){ appendImagerie("Scanner cerebral"); }
    @FXML private void ajouterEchoAbdo()       { appendImagerie("Echo abdominale"); }
    @FXML private void ajouterIRMLombaire()    { appendImagerie("IRM lombaire"); }

    // ── Helpers raccourcis ────────────────────────────────────────────────

    private void appendAnalyse(String item) {
        String current = textAnalyses.getText();
        if (current == null || current.trim().isEmpty()) {
            textAnalyses.setText(item);
        } else if (!current.contains(item)) {
            textAnalyses.setText(current.trim() + ", " + item);
        }
    }

    private void appendImagerie(String item) {
        String current = textImageries.getText();
        if (current == null || current.trim().isEmpty()) {
            textImageries.setText(item);
        } else if (!current.contains(item)) {
            textImageries.setText(current.trim() + ", " + item);
        }
    }

    // ── Actions boutons ───────────────────────────────────────────────────

    @FXML
    private void onEnvoyer() {
        if (consultation == null) return;
        if (!validerAvantEnvoi()) return;

        String analyses  = textAnalyses.getText()  != null ? textAnalyses.getText().trim()  : "";
        String imageries = textImageries.getText() != null ? textImageries.getText().trim() : "";

        consultation.setAnalyses(analyses.isEmpty()   ? null : analyses);
        consultation.setImageries(imageries.isEmpty() ? null : imageries);
        consultation.calculerStatutDemande();

        cs.updateExamens(consultation.getIdConsultation(), analyses, imageries);
        fermer();
    }

    @FXML
    private void onAnnuler() {
        fermer();
    }

    private Runnable onCloseCallback;
    public void setOnCloseCallback(Runnable cb) { this.onCloseCallback = cb; }

    private void fermer() {
        if (onCloseCallback != null) {
            onCloseCallback.run();
        } else {
            Stage stage = (Stage) textAnalyses.getScene().getWindow();
            stage.close();
        }
    }

    private String nvl(String s) { return s != null ? s : "—"; }
}

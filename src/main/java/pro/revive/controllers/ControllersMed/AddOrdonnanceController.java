package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Ordonnance;
import pro.revive.services.ServicesMed.DrugInteractionService;
import pro.revive.services.ServicesMed.DrugInteractionService.Interaction;
import pro.revive.services.ServicesMed.DrugSearchService;
import pro.revive.services.ServicesMed.OrdonnanceService;
import pro.revive.utils.UtilesMed.DrugAutoCompleteField;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Control;
import javafx.stage.Stage;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AddOrdonnanceController implements Initializable {

    @FXML private Label              labelConsultationId;
    @FXML private TextField          fieldMedicament;
    @FXML private TextArea           areaPosologie;
    @FXML private Spinner<Integer>   spinnerDuree;
    @FXML private Button             btnValider;
    @FXML private Button             btnAnnuler;
    @FXML private Label              lblInteraction;   // nouveau — affiche les alertes

    private final OrdonnanceService       ordonnanceService  = new OrdonnanceService();
    private final DrugSearchService       drugSearchService  = new DrugSearchService();
    private final DrugInteractionService  interactionService = new DrugInteractionService();

    private int                           consultationId;
    private OrdonnancesDialogController   parentController;
    private Ordonnance                    ordonnanceAModifier = null;
    private DrugAutoCompleteField         drugAutoComplete;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SpinnerValueFactory<Integer> factory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 365, 7);
        spinnerDuree.setValueFactory(factory);
        spinnerDuree.setEditable(true);
        spinnerDuree.getEditor().textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) spinnerDuree.getEditor().setText(o);
        });

        // Feature 2 — Drug autocomplete
        Platform.runLater(() -> {
            if (fieldMedicament != null) {
                drugAutoComplete = new DrugAutoCompleteField(fieldMedicament, entry -> {
                    verifierInteractions(entry.name());
                });
            }
        });

        // Posologie intelligente — completion automatique
        configurerPosologieIntelligente();
    }

    // ── Posologie intelligente ────────────────────────────────────────────

    /**
     * Complete automatiquement la posologie selon ce que l'utilisateur tape.
     * La suggestion est inseree et selectionnee — continuer a taper l'efface,
     * Tab ou Entree la confirme.
     */
    private void configurerPosologieIntelligente() {
        areaPosologie.textProperty().addListener((obs, oldVal, newVal) -> {
            // Eviter la recursion quand on modifie le texte programmatiquement
            if (suppressPosologieListener) return;
            if (newVal == null) return;

            // Seulement si l'utilisateur a ajoute du texte (pas supprime)
            if (newVal.length() <= oldVal.length()) return;

            String suggestion = trouverSuggestion(newVal);
            if (suggestion != null && !suggestion.isEmpty()) {
                supprimerEtCompleter(newVal, suggestion);
            }
        });

        // Tab ou Entree confirme la selection (ne fait rien de special, le texte est deja la)
        areaPosologie.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case TAB -> {
                    // Deplacer le curseur a la fin pour confirmer
                    int len = areaPosologie.getText().length();
                    areaPosologie.positionCaret(len);
                    areaPosologie.deselect();
                    e.consume();
                }
                case ESCAPE -> {
                    // Annuler la suggestion : garder seulement la partie tapee
                    String sel = areaPosologie.getSelectedText();
                    if (sel != null && !sel.isEmpty()) {
                        int start = areaPosologie.getSelection().getStart();
                        suppressPosologieListener = true;
                        areaPosologie.setText(areaPosologie.getText().substring(0, start));
                        areaPosologie.positionCaret(start);
                        suppressPosologieListener = false;
                    }
                }
                default -> {}
            }
        });
    }

    private boolean suppressPosologieListener = false;

    private void supprimerEtCompleter(String typed, String suggestion) {
        String full = typed + suggestion;
        suppressPosologieListener = true;
        areaPosologie.setText(full);
        // Selectionner la partie suggeree pour que l'utilisateur puisse l'ecraser
        areaPosologie.selectRange(typed.length(), full.length());
        suppressPosologieListener = false;
    }

    /**
     * Retourne la partie a completer selon ce que l'utilisateur a tape.
     * Retourne null si aucune suggestion.
     */
    private String trouverSuggestion(String input) {
        String t = input.toLowerCase().trim();

        // Patterns numeriques : "1", "2", "3", "4", "6"
        if (t.equals("1"))  return " comprime par jour";
        if (t.equals("2"))  return " comprimes par jour";
        if (t.equals("3"))  return " fois par jour";
        if (t.equals("4"))  return " fois par jour";
        if (t.equals("6"))  return " comprimes par jour";

        // "1/2" ou "1/2 c"
        if (t.equals("1/2")) return " comprime par jour";

        // Patterns texte — debut de mot
        if (t.equals("mat"))   return "in et soir";
        if (t.equals("mati"))  return "n et soir";
        if (t.equals("matin")) return " et soir";
        if (t.equals("1 co"))  return "mprime par jour";
        if (t.equals("2 co"))  return "mprimes par jour";
        if (t.equals("3 co"))  return "mprimes par jour";
        if (t.equals("1 f"))   return "ois par jour";
        if (t.equals("2 f"))   return "ois par jour";
        if (t.equals("3 f"))   return "ois par jour";
        if (t.equals("inj"))   return "ection IV par jour";
        if (t.equals("perf"))  return "usion IV en 30 min";
        if (t.equals("supp"))  return "ositoire 1 fois par jour";
        if (t.equals("sirop")) return " : 1 cuillere a soupe 3 fois par jour";
        if (t.equals("goutt")) return "es : 3 fois par jour";
        if (t.equals("patch")) return " : 1 application par jour";
        if (t.equals("creme")) return " : appliquer 2 fois par jour";
        if (t.equals("poud"))  return "re : 1 sachet par jour";
        if (t.equals("gel"))   return " : appliquer 2 fois par jour";
        if (t.equals("spray")) return " : 2 bouffees 3 fois par jour";
        if (t.equals("inhala")) return "tion : 2 bouffees matin et soir";

        // Patterns "X fois" -> completer "par jour"
        if (t.matches("\\d+ fois$"))       return " par jour";
        if (t.matches("\\d+ comprime$"))   return " par jour";
        if (t.matches("\\d+ comprimes$"))  return " par jour";

        return null;
    }

    public void setConsultationId(int id) {
        this.consultationId = id;
        if (labelConsultationId != null) labelConsultationId.setText("Consultation #" + id);
    }

    public void setParentController(OrdonnancesDialogController parent) {
        this.parentController = parent;
    }

    public void setOrdonnancePourModification(Ordonnance o) {
        this.ordonnanceAModifier = o;
        fieldMedicament.setText(o.getMedicament());
        areaPosologie.setText(o.getPosologie());
        spinnerDuree.getValueFactory().setValue(o.getDureeJours());
        btnValider.setText("Mettre a jour");
    }

    /**
     * Feature 3 — Verifie les interactions medicamenteuses en arriere-plan.
     */
    private void verifierInteractions(String newDrug) {
        if (parentController == null) return;

        // Recuperer les medicaments deja prescrits
        List<String> existingDrugs = ordonnanceService
            .getByConsultation(consultationId)
            .stream()
            .map(Ordonnance::getMedicament)
            .collect(Collectors.toList());

        if (existingDrugs.isEmpty()) return;

        // Afficher un indicateur de chargement
        if (lblInteraction != null) {
            lblInteraction.setText("Verification des interactions...");
            lblInteraction.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
        }

        Task<List<Interaction>> task = new Task<>() {
            @Override
            protected List<Interaction> call() {
                return interactionService.checkInteractions(newDrug, existingDrugs);
            }
        };

        task.setOnSucceeded(e -> {
            List<Interaction> interactions = task.getValue();
            if (lblInteraction == null) return;
            if (interactions.isEmpty()) {
                lblInteraction.setText("Aucune interaction detectee.");
                lblInteraction.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
            } else {
                String msg = "ATTENTION : " + interactions.size()
                    + " interaction(s) detectee(s) ! "
                    + interactions.get(0).description();
                if (msg.length() > 120) msg = msg.substring(0, 120) + "...";
                lblInteraction.setText(msg);
                lblInteraction.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 11px;");
                // Alerte visuelle
                fieldMedicament.setStyle(fieldMedicament.getStyle()
                    + "-fx-border-color: #dc2626;");
            }
        });

        task.setOnFailed(e ->  {
            if (lblInteraction != null) {
                lblInteraction.setText("Verification indisponible (hors ligne).");
                lblInteraction.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onValider() {
        if (!validerSaisie()) return;
        try {
            int duree = obtenirDuree();
            if (duree <= 0) {
                afficherErreur("Duree invalide", "La duree doit etre superieure a 0 jour.");
                return;
            }
            if (ordonnanceAModifier == null) {
                Ordonnance o = new Ordonnance(
                    consultationId,
                    fieldMedicament.getText().trim(),
                    areaPosologie.getText().trim(),
                    duree);
                ordonnanceService.addEntity(o);
            } else {
                ordonnanceAModifier.setMedicament(fieldMedicament.getText().trim());
                ordonnanceAModifier.setPosologie(areaPosologie.getText().trim());
                ordonnanceAModifier.setDureeJours(duree);
                ordonnanceService.updateEntity(ordonnanceAModifier.getIdOrdo(), ordonnanceAModifier);
            }
            if (parentController != null) parentController.chargerOrdonnances();
            fermer();
        } catch (Exception e) {
            afficherErreur("Erreur", "Impossible d'enregistrer :\n" + e.getMessage());
        }
    }

    @FXML private void onAnnuler() { fermer(); }

    private boolean validerSaisie() {
        boolean valid = true;

        // Reset styles
        fieldMedicament.setStyle("");
        areaPosologie.setStyle("");

        if (fieldMedicament.getText() == null || fieldMedicament.getText().trim().isEmpty()) {
            setFieldError(fieldMedicament, "Médicament obligatoire");
            valid = false;
        }
        if (areaPosologie.getText() == null || areaPosologie.getText().trim().isEmpty()) {
            setFieldError(areaPosologie, "Posologie obligatoire");
            valid = false;
        }
        if (obtenirDuree() <= 0) {
            spinnerDuree.getEditor().setStyle(
                "-fx-border-color: #EF4444; -fx-border-width: 2px; " +
                "-fx-border-radius: 8px; -fx-background-color: #FFF5F5;");
            valid = false;
        } else {
            spinnerDuree.getEditor().setStyle("");
        }

        return valid;
    }

    private void setFieldError(Control field, String msg) {
        field.setStyle(
            "-fx-border-color: #EF4444; -fx-border-width: 2px; " +
            "-fx-border-radius: 8px; -fx-background-radius: 8px; " +
            "-fx-background-color: #FFF5F5;"
        );
        Tooltip t = new Tooltip("⚠ " + msg);
        t.setStyle("-fx-background-color: #EF4444; -fx-text-fill: white; -fx-font-size: 11px;");
        Tooltip.install(field, t);
        if (field instanceof TextField tf)
            tf.textProperty().addListener((obs, o, n) -> { field.setStyle(""); Tooltip.install(field, null); });
        if (field instanceof TextArea ta)
            ta.textProperty().addListener((obs, o, n) -> { field.setStyle(""); Tooltip.install(field, null); });
    }

    private int obtenirDuree() {
        try {
            spinnerDuree.commitValue();
            Integer val = spinnerDuree.getValue();
            return val != null ? val : 0;
        } catch (Exception e) {
            try { return Integer.parseInt(spinnerDuree.getEditor().getText().trim()); }
            catch (NumberFormatException ex) { return 0; }
        }
    }

    private void fermer() {
        if (drugAutoComplete != null) drugAutoComplete.shutdown();
        ((Stage) btnAnnuler.getScene().getWindow()).close();
    }

    private void afficherErreur(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titre); alert.setHeaderText(null); alert.setContentText(message);
        styleAlert(alert); alert.showAndWait();
    }

    private void styleAlert(Alert alert) {
        try {
            alert.getDialogPane().getStylesheets().add(
                getClass().getResource("/ResourcesMed/module3/css/revive-dark.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}

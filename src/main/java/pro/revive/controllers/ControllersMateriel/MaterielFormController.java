package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.services.ServicesMateriel.MaterielService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur du formulaire de création / modification d'un matériel d'urgence.
 * Modes :
 *   - Création : setMateriel(null)
 *   - Modification : setMateriel(materiel)
 */
public class MaterielFormController implements Initializable {

    // ── Composants FXML ──────────────────────────────────────────────

    @FXML private Label              lblTitre;
    @FXML private TextField          txtNom;
    @FXML private ComboBox<String>   cmbSalle;
    @FXML private DatePicker         dpMaintenance;
    @FXML private ComboBox<String>   cmbEtat;
    @FXML private Spinner<Integer>   spnQuantite;
    @FXML private Button             btnEnregistrer;
    @FXML private Button             btnAnnuler;
    @FXML private Label              lblErreur;

    // ── État interne ─────────────────────────────────────────────────

    private MaterielUrgence materielEnCours = null;
    private final MaterielService materielService = new MaterielService();

    // ── Initialisation ───────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // ComboBox État — valeurs exactes de l'ENUM dans materiel_urgence
        cmbEtat.getItems().addAll("Fonctionnel", "A reviser");

        // Spinner quantité (min 1, max 9999, défaut 1)
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1);
        spnQuantite.setValueFactory(factory);
        spnQuantite.setEditable(true);

        // Forcer la saisie numérique dans le Spinner
        spnQuantite.getEditor().textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                spnQuantite.getEditor().setText(old);
            }
        });

        // DatePicker : format français
        dpMaintenance.setConverter(new StringConverter<>() {
            private final java.time.format.DateTimeFormatter fmt =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
            @Override public String toString(LocalDate d)   { return d != null ? fmt.format(d) : ""; }
            @Override public LocalDate fromString(String s) {
                try { return (s != null && !s.isEmpty()) ? LocalDate.parse(s, fmt) : null; }
                catch (Exception e) { return null; }
            }
        });

        // Charger les salles dans la ComboBox
        chargerSalles();

        lblErreur.setVisible(false);
    }

    /** Charge la liste des salles depuis la base pour la ComboBox. */
    private void chargerSalles() {
        try {
            List<String> salles = materielService.getAllSallesForCombo();
            cmbSalle.getItems().setAll(salles);
            cmbSalle.setValue("Réserve"); // valeur par défaut
        } catch (SQLException e) {
            lblErreur.setText("Impossible de charger les salles : " + e.getMessage());
            lblErreur.setVisible(true);
        }
    }

    /**
     * Injecte le matériel à modifier (null = mode création).
     * Doit être appelé APRÈS initialize().
     */
    public void setMateriel(MaterielUrgence materiel) {
        this.materielEnCours = materiel;
        if (materiel != null) {
            lblTitre.setText("Modifier le Matériel");
            txtNom.setText(materiel.getNom());
            dpMaintenance.setValue(materiel.getDateDerniereMaintenance());
            cmbEtat.setValue(materiel.getEtat());
            spnQuantite.getValueFactory().setValue(materiel.getQuantite());

            // Sélectionner la salle dans la ComboBox
            if (materiel.getIdSalle() == null) {
                cmbSalle.setValue("Réserve");
            } else {
                // Chercher l'entrée "id - nom" correspondante
                cmbSalle.getItems().stream()
                        .filter(s -> s.startsWith(materiel.getIdSalle() + " - "))
                        .findFirst()
                        .ifPresent(cmbSalle::setValue);
            }
        } else {
            lblTitre.setText("Nouveau Matériel");
        }
    }

    // ── Actions ──────────────────────────────────────────────────────

    @FXML
    private void onEnregistrer() {
        // ── Contrôle de saisie ────────────────────────────────────
        String nom    = txtNom.getText() == null ? "" : txtNom.getText().trim();
        String salle  = cmbSalle.getValue();
        LocalDate date = dpMaintenance.getValue();
        String etat   = cmbEtat.getValue();
        Integer qte   = spnQuantite.getValue();

        // 1. Nom obligatoire
        if (nom.isEmpty()) {
            afficherErreur("Le nom du matériel est obligatoire.");
            txtNom.requestFocus();
            return;
        }
        // 2. Nom : minimum 3 caractères
        if (nom.length() < 3) {
            afficherErreur("Le nom doit contenir au moins 3 caractères.");
            txtNom.requestFocus();
            return;
        }
        // 3. Nom : pas uniquement des chiffres
        if (nom.matches("\\d+")) {
            afficherErreur("Le nom du matériel ne peut pas être uniquement des chiffres.");
            txtNom.requestFocus();
            return;
        }
        // 4. Nom : pas un seul mot de moins de 3 lettres
        if (!nom.contains(" ") && nom.length() < 3) {
            afficherErreur("Le nom doit être plus descriptif (ex: 'Défibrillateur Zoll').");
            txtNom.requestFocus();
            return;
        }
        // 5. Date obligatoire
        if (date == null) {
            afficherErreur("La date de dernière maintenance est obligatoire.");
            dpMaintenance.requestFocus();
            return;
        }
        // 6. Date pas dans le futur
        if (date.isAfter(LocalDate.now())) {
            afficherErreur("La date de maintenance ne peut pas être dans le futur.");
            dpMaintenance.requestFocus();
            return;
        }
        // 7. État obligatoire
        if (etat == null || etat.isEmpty()) {
            afficherErreur("Veuillez sélectionner l'état du matériel.");
            cmbEtat.requestFocus();
            return;
        }
        // 8. Quantité : valider la saisie manuelle dans le spinner
        try {
            String editorText = spnQuantite.getEditor().getText().trim();
            if (editorText.isEmpty()) {
                afficherErreur("La quantité est obligatoire.");
                spnQuantite.requestFocus();
                return;
            }
            qte = Integer.parseInt(editorText);
            if (qte < 1) {
                afficherErreur("La quantité doit être au moins 1.");
                spnQuantite.requestFocus();
                return;
            }
            if (qte > 9999) {
                afficherErreur("La quantité ne peut pas dépasser 9999.");
                spnQuantite.requestFocus();
                return;
            }
            spnQuantite.getValueFactory().setValue(qte);
        } catch (NumberFormatException e) {
            afficherErreur("La quantité doit être un nombre entier (ex: 5).");
            spnQuantite.requestFocus();
            return;
        }

        // ── Résoudre l'ID de salle ────────────────────────────────
        Integer idSalle = null;
        if (salle != null && !"Réserve".equals(salle)) {
            try {
                idSalle = Integer.parseInt(salle.split(" - ")[0].trim());
            } catch (NumberFormatException e) {
                afficherErreur("Salle invalide.");
                return;
            }
        }

        // ── Persistance ───────────────────────────────────────────
        try {
            if (materielEnCours == null) {
                MaterielUrgence nouveau = new MaterielUrgence(idSalle, nom, date, etat, qte);
                materielService.create(nouveau);
            } else {
                materielEnCours.setNom(nom);
                materielEnCours.setIdSalle(idSalle);
                materielEnCours.setDateDerniereMaintenance(date);
                materielEnCours.setEtat(etat);
                materielEnCours.setQuantite(qte);
                materielService.update(materielEnCours);
            }
            fermerFenetre();
        } catch (SQLException e) {
            afficherErreur("Erreur base de données : " + e.getMessage());
        }
    }

    /** Ferme le formulaire sans sauvegarder. */
    @FXML
    private void onAnnuler() {
        fermerFenetre();
    }

    // ── Utilitaires ──────────────────────────────────────────────────

    private void afficherErreur(String message) {
        lblErreur.setText("⚠  " + message);
        lblErreur.setVisible(true);
    }

    private void fermerFenetre() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}

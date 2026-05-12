package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.SallePhysique;
import pro.revive.services.ServicesMateriel.SalleService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SalleFormController implements Initializable {

    // ── Composants FXML ──────────────────────────────────────────────
    @FXML private Label              lblTitre;
    @FXML private TextField          txtNom;
    @FXML private ComboBox<String>   cmbType;
    @FXML private ComboBox<String>   cmbStatut;
    @FXML private Spinner<Integer>   spnCapacite;
    @FXML private Spinner<Integer>   spnEtage;
    @FXML private ComboBox<String>   cmbAile;
    @FXML private Button             btnEnregistrer;
    @FXML private Button             btnAnnuler;
    @FXML private Label              lblErreur;

    private SallePhysique salleEnCours = null;
    private final SalleService salleService = new SalleService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Types et statuts
        cmbType.getItems().addAll("resuscitation", "urgence", "standard", "attente");
        cmbStatut.getItems().addAll("Disponible", "Occupée", "Nettoyage", "Maintenance");

        // ── Spinner Capacité (nombre de lits) : 1 à 100 ──────────
        SpinnerValueFactory<Integer> factoryCapacite =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1);
        spnCapacite.setValueFactory(factoryCapacite);
        spnCapacite.setEditable(true);

        // Forcer saisie numérique uniquement dans le spinner capacité
        spnCapacite.getEditor().textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                spnCapacite.getEditor().setText(old);
                return;
            }
            if (!val.isEmpty()) {
                try {
                    int v = Integer.parseInt(val);
                    if (v < 1 || v > 100) {
                        spnCapacite.getEditor().setText(old);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        // ── Spinner Étage : 0 (RDC) à 20 ────────────────────────
        SpinnerValueFactory<Integer> factoryEtage =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0);
        spnEtage.setValueFactory(factoryEtage);
        spnEtage.setEditable(true);

        // Forcer saisie numérique uniquement dans le spinner étage
        spnEtage.getEditor().textProperty().addListener((obs, old, val) -> {
            if (!val.matches("\\d*")) {
                spnEtage.getEditor().setText(old);
                return;
            }
            if (!val.isEmpty()) {
                try {
                    int v = Integer.parseInt(val);
                    if (v < 0 || v > 20) {
                        spnEtage.getEditor().setText(old);
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        // ── ComboBox Aile / Secteur ───────────────────────────────
        cmbAile.getItems().addAll(
            "Aile A — Urgences",
            "Aile B — Chirurgie",
            "Aile C — Médecine",
            "Aile D — Pédiatrie",
            "Aile E — Maternité",
            "Aile F — Réanimation",
            "Aile G — Cardiologie",
            "Aile H — Neurologie",
            "Bloc Opératoire",
            "Soins Intensifs",
            "Radiologie",
            "Laboratoire"
        );
        cmbAile.setValue("Aile A — Urgences");

        // ── Validation nom en temps réel ──────────────────────────
        txtNom.textProperty().addListener((obs, old, val) -> {
            // Bloquer les caractères spéciaux dangereux
            if (!val.matches("[a-zA-ZÀ-ÿ0-9\\s\\-_']*")) {
                txtNom.setText(old);
            }
        });

        lblErreur.setVisible(false);
    }

    public void setSalle(SallePhysique salle) {
        this.salleEnCours = salle;
        if (salle != null) {
            lblTitre.setText("Modifier la Salle");
            txtNom.setText(salle.getNom());
            cmbType.setValue(salle.getType());
            cmbStatut.setValue(salle.getStatut());
            spnCapacite.getValueFactory().setValue(Math.max(1, salle.getCapaciteMax()));

            // Parser la localisation "Aile A — Urgences | Étage 2"
            String loc = salle.getLocalisation();
            if (loc != null && loc.contains("|")) {
                String[] parts = loc.split("\\|");
                String aile = parts[0].trim();
                String etageStr = parts[1].trim().replace("Étage", "").replace("RDC", "0").trim();
                cmbAile.setValue(aile);
                try {
                    spnEtage.getValueFactory().setValue(Integer.parseInt(etageStr));
                } catch (NumberFormatException ignored) {
                    spnEtage.getValueFactory().setValue(0);
                }
            } else if (loc != null && !loc.isEmpty()) {
                // Ancienne localisation texte libre
                cmbAile.setValue(loc);
            }
        } else {
            lblTitre.setText("Nouvelle Salle");
        }
    }

    @FXML
    private void onEnregistrer() {
        lblErreur.setVisible(false);

        // ── 1. Nom ────────────────────────────────────────────────
        String nom = txtNom.getText() == null ? "" : txtNom.getText().trim();
        if (nom.isEmpty()) {
            afficherErreur("Le nom de la salle est obligatoire.");
            txtNom.requestFocus();
            return;
        }
        if (nom.length() < 3) {
            afficherErreur("Le nom doit contenir au moins 3 caractères.");
            txtNom.requestFocus();
            return;
        }
        if (nom.length() > 100) {
            afficherErreur("Le nom ne peut pas dépasser 100 caractères.");
            txtNom.requestFocus();
            return;
        }

        // ── 2. Type ───────────────────────────────────────────────
        String type = cmbType.getValue();
        if (type == null || type.isEmpty()) {
            afficherErreur("Veuillez sélectionner un type de salle.");
            cmbType.requestFocus();
            return;
        }

        // ── 3. Statut ─────────────────────────────────────────────
        String statut = cmbStatut.getValue();
        if (statut == null || statut.isEmpty()) {
            afficherErreur("Veuillez sélectionner un statut.");
            cmbStatut.requestFocus();
            return;
        }

        // ── 4. Capacité (Spinner) ─────────────────────────────────
        int capacite;
        try {
            // Valider la valeur saisie manuellement dans le spinner
            String editorText = spnCapacite.getEditor().getText().trim();
            if (editorText.isEmpty()) {
                afficherErreur("Le nombre de lits est obligatoire.");
                spnCapacite.requestFocus();
                return;
            }
            capacite = Integer.parseInt(editorText);
            if (capacite < 1 || capacite > 100) {
                afficherErreur("Le nombre de lits doit être entre 1 et 100.");
                spnCapacite.requestFocus();
                return;
            }
            spnCapacite.getValueFactory().setValue(capacite);
        } catch (NumberFormatException e) {
            afficherErreur("Le nombre de lits doit être un nombre entier (ex: 5).");
            spnCapacite.requestFocus();
            return;
        }

        // ── 5. Étage (Spinner) ────────────────────────────────────
        int etage;
        try {
            String editorText = spnEtage.getEditor().getText().trim();
            etage = editorText.isEmpty() ? 0 : Integer.parseInt(editorText);
            if (etage < 0 || etage > 20) {
                afficherErreur("L'étage doit être entre 0 (RDC) et 20.");
                spnEtage.requestFocus();
                return;
            }
            spnEtage.getValueFactory().setValue(etage);
        } catch (NumberFormatException e) {
            afficherErreur("L'étage doit être un nombre entier (0 = RDC).");
            spnEtage.requestFocus();
            return;
        }

        // ── 6. Construire la localisation ─────────────────────────
        String aile = cmbAile.getValue() != null ? cmbAile.getValue() : "Non spécifiée";
        String etageLabel = etage == 0 ? "RDC" : "Étage " + etage;
        String localisation = aile + " | " + etageLabel;

        // ── Persistance ───────────────────────────────────────────
        try {
            if (salleEnCours == null) {
                SallePhysique nouvelle = new SallePhysique(nom, type, statut);
                nouvelle.setCapaciteMax(capacite);
                nouvelle.setLocalisation(localisation);
                salleService.create(nouvelle);
            } else {
                salleEnCours.setNom(nom);
                salleEnCours.setType(type);
                salleEnCours.setStatut(statut);
                salleEnCours.setCapaciteMax(capacite);
                salleEnCours.setLocalisation(localisation);
                salleService.update(salleEnCours);
            }
            fermerFenetre();
        } catch (SQLException e) {
            afficherErreur("Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    private void onAnnuler() {
        fermerFenetre();
    }

    private void afficherErreur(String message) {
        lblErreur.setText("⚠  " + message);
        lblErreur.setVisible(true);
    }

    private void fermerFenetre() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
}

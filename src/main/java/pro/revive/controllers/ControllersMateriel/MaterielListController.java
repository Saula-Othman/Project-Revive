package pro.revive.controllers.ControllersMateriel; // Mise a jour interactions materiel

import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.services.ServicesMateriel.MaterielService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Contrôleur de la liste du matériel d'urgence.
 * Inclut : CRUD, filtre par salle, recherche en temps réel, export PDF, navigation.
 */
public class MaterielListController implements Initializable {

    // ── Composants FXML ──────────────────────────────────────────────
    @FXML private FlowPane cardContainer;
    @FXML private StackPane modalOverlay;
    @FXML private VBox      modalContent;

    @FXML private TextField        txtRecherche;
    @FXML private ComboBox<String> cmbFiltresSalle;
    @FXML private Button           btnNouveau;
    @FXML private Button           btnModifier;
    @FXML private Button           btnSupprimer;
    @FXML private Button           btnActualiser;
    @FXML private Button           btnExportPdf;
    @FXML private Button           btnRetourSalles;
    @FXML private Button           btnDashboard;
    @FXML private Label            lblTotal;
    @FXML private Label            lblUserName, lblUserRole, lblUserInitial;

    // ── Données ──────────────────────────────────────────────────────
    private final MaterielService materielService = new MaterielService();
    private final ObservableList<MaterielUrgence> data     = FXCollections.observableArrayList();
    private       FilteredList<MaterielUrgence>   filtered;
    private       MaterielUrgence                 selectedMateriel;

    // ── Initialisation ───────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filtered = new FilteredList<>(data, p -> true);
        configurerRecherche();
        chargerFiltresSalle();
        chargerDonnees();
        updateButtonStates();

        // Informations utilisateur
        String fullName = pro.revive.SessionManager.getFullName();
        String role = pro.revive.SessionManager.getRole();
        lblUserName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        lblUserRole.setText(role.isEmpty() ? "Personnel" : role);
        if (!fullName.isEmpty()) {
            lblUserInitial.setText(fullName.substring(0, 1).toUpperCase());
        }
    }

    private void updateButtonStates() {
        boolean selected = selectedMateriel != null;
        btnModifier.setDisable(!selected);
        btnSupprimer.setDisable(!selected);
    }

    /** Filtre combiné : recherche texte + filtre salle. */
    private void configurerRecherche() {
        txtRecherche.textProperty().addListener((obs, old, val) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        String terme  = txtRecherche.getText() == null ? "" : txtRecherche.getText().toLowerCase().trim();
        String salle  = cmbFiltresSalle.getValue();

        filtered.setPredicate(m -> {
            boolean matchTexte = terme.isEmpty()
                    || m.getNom().toLowerCase().contains(terme)
                    || (m.getNomSalle() != null && m.getNomSalle().toLowerCase().contains(terme))
                    || m.getEtat().toLowerCase().contains(terme);

            boolean matchSalle = true;
            if (salle != null && !"Toutes les salles".equals(salle)) {
                if ("Réserve".equals(salle)) {
                    matchSalle = m.getIdSalle() == null || m.getIdSalle() == 0;
                } else {
                    try {
                        int idSalle = Integer.parseInt(salle.split(" - ")[0].trim());
                        matchSalle = m.getIdSalle() != null && m.getIdSalle() == idSalle;
                    } catch (NumberFormatException e) {
                        matchSalle = true;
                    }
                }
            }
            return matchTexte && matchSalle;
        });
        
        afficherCartes();
        lblTotal.setText("Résultats : " + filtered.size() + " / " + data.size() + " équipement(s)");
    }

    private void chargerFiltresSalle() {
        try {
            List<String> salles = materielService.getAllSallesForCombo();
            cmbFiltresSalle.getItems().clear();
            cmbFiltresSalle.getItems().add("Toutes les salles");
            cmbFiltresSalle.getItems().addAll(salles);
            cmbFiltresSalle.setValue("Toutes les salles");
        } catch (SQLException e) {
            afficherErreur("Erreur salles", e.getMessage());
        }
    }

    private void chargerDonnees() {
        try {
            data.setAll(materielService.findAll());
            selectedMateriel = null;
            appliquerFiltres();
            updateButtonStates();
        } catch (SQLException e) {
            afficherErreur("Erreur chargement", e.getMessage());
        }
    }

    private void afficherCartes() {
        cardContainer.getChildren().clear();
        for (MaterielUrgence m : filtered) {
            cardContainer.getChildren().add(creerCarte(m));
        }
        
        if (filtered.isEmpty()) {
            Label placeholder = new Label("Aucun matériel trouvé");
            placeholder.getStyleClass().add("table-placeholder");
            cardContainer.getChildren().add(placeholder);
        }
    }

    private VBox creerCarte(MaterielUrgence m) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card-item");
        if (m == selectedMateriel) card.getStyleClass().add("card-item-selected");

        Label title = new Label(m.getNom());
        title.getStyleClass().add("card-title");

        Label subtitle = new Label(m.getNomSalle() != null ? m.getNomSalle() : "Non affecté");
        subtitle.getStyleClass().add("card-subtitle");

        HBox badgeContainer = new HBox();
        Label badge = new Label(m.getEtat());
        badge.getStyleClass().add("card-badge");
        if ("Fonctionnel".equals(m.getEtat()))   badge.getStyleClass().add("badge-success");
        else if ("A reviser".equals(m.getEtat())) badge.getStyleClass().add("badge-warning");
        else                                       badge.getStyleClass().add("badge-danger");
        badgeContainer.getChildren().add(badge);

        VBox info = new VBox(5);
        info.getChildren().addAll(
            creerInfoRow("Quantité:", String.valueOf(m.getQuantite())),
            creerInfoRow("Maintenance:", m.getDateDerniereMaintenance() != null ? m.getDateDerniereMaintenance().toString() : "N/A")
        );

        card.getChildren().addAll(title, subtitle, badgeContainer, info);

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ouvrirQuickDetails(m);
            } else {
                selectedMateriel = m;
                afficherCartes(); // Rafraîchir pour montrer la sélection
                updateButtonStates();
            }
            event.consume();
        });

        return card;
    }

    private HBox creerInfoRow(String label, String value) {
        HBox row = new HBox(10);
        row.getStyleClass().add("card-info-row");
        Label lbl = new Label(label);
        lbl.getStyleClass().add("card-info-label");
        Label val = new Label(value);
        val.getStyleClass().add("card-info-value");
        row.getChildren().addAll(lbl, val);
        return row;
    }

    // ── Actions ──────────────────────────────────────────────────────

    @FXML private void onFiltrer()        { appliquerFiltres(); }
    @FXML private void onNouveauMateriel(){ ouvrirFormulaire(null); }
    @FXML private void onActualiser()     { chargerFiltresSalle(); chargerDonnees(); }

    @FXML
    private void onModifier() {
        if (selectedMateriel == null) { afficherInfo("Sélection requise", "Veuillez sélectionner un équipement."); return; }
        ouvrirFormulaire(selectedMateriel);
    }

    @FXML
    private void onSupprimer() {
        if (selectedMateriel == null) { afficherInfo("Sélection requise", "Veuillez sélectionner un équipement."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer « " + selectedMateriel.getNom() + " » ?");
        confirm.setContentText("Cette action est irréversible.");
        styleAlert(confirm);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try { materielService.delete(selectedMateriel.getIdMateriel()); chargerDonnees(); }
            catch (SQLException e) { afficherErreur("Erreur suppression", e.getMessage()); }
        }
    }

    /** Export PDF de l'inventaire visible. */
    @FXML
    private void onExportPdf() {
        PdfExportService.exporterMateriel(
                filtered,
                (Stage) btnExportPdf.getScene().getWindow()
        );
    }

    @FXML private void onRetourSalles()   { naviguerVers("/ResourcesMateriel/module5/view/SalleList.fxml",          "REVIVE — Salles",              btnRetourSalles); }
    @FXML private void onVoirDashboard()  { naviguerVers("/ResourcesMateriel/module5/view/dashboardMateriel.fxml",          "REVIVE — Tableau de Bord",     btnDashboard); }
    @FXML private void onVoirAmbulances() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceList.fxml",      "REVIVE — Gestion Ambulances",  btnDashboard); }
    @FXML private void onVoirSimulation() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceSim.fxml",       "REVIVE — Simulation Trajet",   btnDashboard); }
    @FXML private void onVoirHistorique() { naviguerVers("/ResourcesMateriel/module5/view/HistoriqueMissions.fxml", "REVIVE — Historique",          btnDashboard); }
    @FXML private void onVoirRecherche()  { naviguerVers("/ResourcesMateriel/module5/view/RechercheGlobale.fxml",   "REVIVE — Recherche",           btnDashboard); }

    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) btnNouveau.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("REVIVE — Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Navigation & formulaire ───────────────────────────────────────

    private void naviguerVers(String fxml, String titre, Button source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) source.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(titre);
        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Navigation", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private void ouvrirQuickDetails(Object item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/QuickDetails.fxml"));
            Parent root = loader.load();
            QuickDetailsController ctrl = loader.getController();
            ctrl.setOnCloseRequested(() -> fermerModal());
            ctrl.setOnEditRequested(itemToEdit -> gererEditDepuisModal(itemToEdit));
            ctrl.setItem(item);

            modalContent.getChildren().clear();
            modalContent.getChildren().add(root);
            modalOverlay.setVisible(true);
            modalOverlay.setManaged(true);

            // Animation
            root.setScaleX(0.7); root.setScaleY(0.7); root.setOpacity(0);
            javafx.animation.FadeTransition ftO = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), modalOverlay);
            ftO.setFromValue(0); ftO.setToValue(1);
            javafx.animation.FadeTransition ftC = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), root);
            ftC.setFromValue(0); ftC.setToValue(1);
            javafx.animation.ScaleTransition stC = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), root);
            stC.setFromX(0.7); stC.setFromY(0.7); stC.setToX(1); stC.setToY(1);
            ftO.play(); ftC.play(); stC.play();

            modalOverlay.setOnMouseClicked(e -> { if (e.getTarget() == modalOverlay) fermerModal(); });

        } catch (IOException e) {
            afficherErreur("Popup", e.getMessage());
        }
    }

    public void fermerModal() {
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), modalOverlay);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
            chargerDonnees();
        });
        ft.play();
    }

    public void gererEditDepuisModal(Object item) {
        if (item instanceof MaterielUrgence) {
            ouvrirFormulaire((MaterielUrgence) item);
        }
    }

    private void ouvrirFormulaire(MaterielUrgence materiel) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ResourcesMateriel/module5/view/MaterielForm.fxml"));
            Parent root = loader.load();
            MaterielFormController ctrl = loader.getController();
            ctrl.setMateriel(materiel);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(materiel == null ? "Nouveau Matériel" : "Modifier le Matériel");
            modal.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm());
            modal.setScene(scene);
            modal.showAndWait();
            chargerDonnees();
        } catch (IOException e) { afficherErreur("Formulaire", e.getMessage()); }
    }

    // ── Alertes ──────────────────────────────────────────────────────

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Dialog<?> d) {
        try { d.getDialogPane().getStylesheets().add(
                getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}

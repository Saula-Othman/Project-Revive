package pro.revive.controllers.ControllersMateriel; // Mise a jour du controleur

import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.entities.EntitiesMateriel.SallePhysique;
import pro.revive.services.ServicesMateriel.MaterielService;
import pro.revive.services.ServicesMateriel.SalleService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
import java.util.stream.Collectors;

/**
 * Contrôleur de la liste des salles physiques.
 * Inclut : CRUD, changement de statut, recherche en temps réel, navigation.
 */
public class SalleListController implements Initializable {

    // ── Composants FXML ──────────────────────────────────────────────
    @FXML private FlowPane cardContainer;
    @FXML private VBox     reserveContainer;
    @FXML private StackPane modalOverlay;
    @FXML private VBox      modalContent;

    @FXML private TextField txtRecherche;
    @FXML private Button    btnNouvelle;
    @FXML private Button    btnModifier;
    @FXML private Button    btnSupprimer;
    @FXML private Button    btnStatut;
    @FXML private Button    btnActualiser;
    @FXML private Button    btnMateriel;
    @FXML private Button    btnDashboard;
    @FXML private Label     lblTotal;

    // ── Données ──────────────────────────────────────────────────────
    private final SalleService    salleService    = new SalleService();
    private final MaterielService materielService = new MaterielService();
    private final ObservableList<SallePhysique> data     = FXCollections.observableArrayList();
    private       FilteredList<SallePhysique>   filtered;
    private       SallePhysique                 selectedSalle;

    // ── Initialisation ───────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filtered = new FilteredList<>(data, p -> true);
        configurerRecherche();
        chargerDonnees();
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean selected = selectedSalle != null;
        btnModifier.setDisable(!selected);
        btnSupprimer.setDisable(!selected);
        btnStatut.setDisable(!selected);
    }

    /** Filtre en temps réel sur le TextField de recherche. */
    private void configurerRecherche() {
        txtRecherche.textProperty().addListener((obs, old, val) -> {
            String terme = val == null ? "" : val.toLowerCase().trim();
            filtered.setPredicate(salle -> {
                if (terme.isEmpty()) return true;
                return salle.getNom().toLowerCase().contains(terme)
                    || salle.getType().toLowerCase().contains(terme)
                    || salle.getStatut().toLowerCase().contains(terme);
            });
            afficherCartes();
            lblTotal.setText("Résultats : " + filtered.size() + " / " + data.size() + " salle(s)");
        });
    }

    private void chargerDonnees() {
        try {
            data.setAll(salleService.findAll());
            selectedSalle = null;
            afficherCartes();
            chargerReserve();
            lblTotal.setText("Total : " + data.size() + " salle(s)");
            updateButtonStates();
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    private void chargerReserve() {
        try {
            reserveContainer.getChildren().clear();
            List<MaterielUrgence> reserve = materielService.findAll().stream()
                    .filter(m -> m.getIdSalle() == null || m.getIdSalle() == 0)
                    .collect(Collectors.toList());

            for (MaterielUrgence m : reserve) {
                reserveContainer.getChildren().add(creerCarteMateriel(m));
            }

            if (reserve.isEmpty()) {
                Label empty = new Label("Aucun matériel en réserve");
                empty.setStyle("-fx-text-fill: rgba(255,255,255,0.3); -fx-font-size: 11px;");
                reserveContainer.getChildren().add(empty);
            }
        } catch (SQLException e) {
            System.err.println("Erreur réserve : " + e.getMessage());
        }
    }

    private void afficherCartes() {
        cardContainer.getChildren().clear();
        for (SallePhysique s : filtered) {
            cardContainer.getChildren().add(creerCarte(s));
        }

        if (filtered.isEmpty()) {
            Label placeholder = new Label("Aucune salle trouvée");
            placeholder.getStyleClass().add("table-placeholder");
            cardContainer.getChildren().add(placeholder);
        }
    }

    private VBox creerCarte(SallePhysique s) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card-item");
        if (s == selectedSalle) card.getStyleClass().add("card-item-selected");

        Label title = new Label(s.getNom());
        title.getStyleClass().add("card-title");

        Label subtitle = new Label(s.getType());
        subtitle.getStyleClass().add("card-subtitle");

        // ── Liste des équipements (Noms) ──
        Label lblEquipements = new Label("Chargement...");
        lblEquipements.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");
        lblEquipements.setWrapText(true);
        
        try {
            List<MaterielUrgence> mats = materielService.findBySalle(s.getIdSalle());
            if (mats.isEmpty()) {
                lblEquipements.setText("Aucun matériel");
            } else {
                String noms = mats.stream()
                        .map(MaterielUrgence::getNom)
                        .collect(java.util.stream.Collectors.joining(", "));
                lblEquipements.setText("📦 " + noms);
            }
        } catch (SQLException e) {
            lblEquipements.setText("Erreur matériel");
        }

        HBox badgeContainer = new HBox();
        Label badge = new Label(s.getStatut());
        badge.getStyleClass().add("card-badge");
        switch (s.getStatut()) {
            case "Disponible" -> badge.getStyleClass().add("badge-success");
            case "Occupée", "Pleine" -> badge.getStyleClass().add("badge-warning");
            case "Nettoyage"  -> badge.getStyleClass().add("badge-info");
            case "Maintenance" -> badge.getStyleClass().add("badge-danger");
            default           -> badge.getStyleClass().add("badge-danger");
        }
        badgeContainer.getChildren().add(badge);

        card.getChildren().addAll(title, subtitle, lblEquipements, badgeContainer);

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ouvrirQuickDetails(s);
            } else {
                selectedSalle = s;
                afficherCartes();
                updateButtonStates();
            }
        });

        // ── Drag & Drop Handlers ─────────────────────────────────────
        card.setOnDragOver(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        card.setOnDragEntered(event -> {
            if (event.getGestureSource() != card && event.getDragboard().hasString()) {
                card.getStyleClass().add("card-item-drag-over");
            }
            event.consume();
        });

        card.setOnDragExited(event -> {
            card.getStyleClass().remove("card-item-drag-over");
            event.consume();
        });

        card.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    int idMat = Integer.parseInt(db.getString());
                    MaterielUrgence m = materielService.findById(idMat);
                    if (m != null) {
                        m.setIdSalle(s.getIdSalle());
                        materielService.update(m);
                        chargerDonnees(); // Rafraîchit tout
                        success = true;
                    }
                } catch (Exception e) {
                    afficherErreur("Erreur Drop", e.getMessage());
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        return card;
    }

    private VBox creerCarteMateriel(MaterielUrgence m) {
        VBox card = new VBox(4);
        card.getStyleClass().add("mini-card");

        Label title = new Label(m.getNom());
        title.getStyleClass().add("mini-card-title");

        Label subtitle = new Label("Quantité: " + m.getQuantite() + " | " + m.getEtat());
        subtitle.getStyleClass().add("mini-card-subtitle");

        card.getChildren().addAll(title, subtitle);

        // Début du Drag
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(m.getIdMateriel()));
            db.setContent(content);
            event.consume();
        });

        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ouvrirQuickDetails(m);
            }
        });

        return card;
    }

    private void ouvrirQuickDetails(Object item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/QuickDetails.fxml"));
            Parent root = loader.load();
            QuickDetailsController ctrl = loader.getController();
            ctrl.setOnCloseRequested(() -> fermerModal());
            ctrl.setOnEditRequested(itemToEdit -> gererEditDepuisModal(itemToEdit));
            ctrl.setItem(item);

            // Configuration de l'overlay
            modalContent.getChildren().clear();
            modalContent.getChildren().add(root);
            modalOverlay.setVisible(true);
            modalOverlay.setManaged(true);

            // Animation d'entrée
            root.setScaleX(0.7); root.setScaleY(0.7); root.setOpacity(0);
            
            javafx.animation.FadeTransition ftOverlay = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), modalOverlay);
            ftOverlay.setFromValue(0); ftOverlay.setToValue(1);
            
            javafx.animation.FadeTransition ftContent = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), root);
            ftContent.setFromValue(0); ftContent.setToValue(1);
            
            javafx.animation.ScaleTransition stContent = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(300), root);
            stContent.setFromX(0.7); stContent.setFromY(0.7); stContent.setToX(1); stContent.setToY(1);
            
            ftOverlay.play();
            ftContent.play();
            stContent.play();

            // Gestion de la fermeture depuis le controleur du popup
            // On peut utiliser une ruse : quand le controleur finit, il peut notifier.
            // Mais ici on va simplement attendre que l'utilisateur clique sur Fermer.
            // Pour que l'overlay se ferme, on va injecter une action dans le ctrl.
            
            modalOverlay.setOnMouseClicked(event -> {
                if (event.getTarget() == modalOverlay) fermerModal();
            });

        } catch (IOException e) {
            afficherErreur("Popup", e.getMessage());
        }
    }

    public void fermerModal() {
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(200), modalOverlay);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
            chargerDonnees(); 
        });
        ft.play();
    }

    public void gererEditDepuisModal(Object item) {
        if (item instanceof SallePhysique) {
            ouvrirFormulaire((SallePhysique) item);
        } else if (item instanceof MaterielUrgence) {
            ouvrirMaterielForm((MaterielUrgence) item);
        }
    }

    private void ouvrirMaterielForm(MaterielUrgence m) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/MaterielForm.fxml"));
            Parent root = loader.load();
            MaterielFormController ctrl = loader.getController();
            ctrl.setMateriel(m);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Matériel");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Formulaire Matériel", e.getMessage());
        }
    }

    // ── Actions ──────────────────────────────────────────────────────

    @FXML private void onNouvelleSalle()  { ouvrirFormulaire(null); }
    @FXML private void onActualiser()     { chargerDonnees(); }

    @FXML
    private void onModifier() {
        if (selectedSalle == null) { afficherInfo("Sélection requise", "Veuillez sélectionner une salle."); return; }
        ouvrirFormulaire(selectedSalle);
    }

    @FXML
    private void onSupprimer() {
        if (selectedSalle == null) { afficherInfo("Sélection requise", "Veuillez sélectionner une salle."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer « " + selectedSalle.getNom() + " » ?");
        confirm.setContentText("Le matériel associé sera déplacé en réserve.");
        styleAlert(confirm);
        Optional<ButtonType> r = confirm.showAndWait();
        if (r.isPresent() && r.get() == ButtonType.OK) {
            try { salleService.delete(selectedSalle.getIdSalle()); chargerDonnees(); }
            catch (SQLException e) { afficherErreur("Erreur suppression", e.getMessage()); }
        }
    }

    @FXML
    private void onChangerStatut() {
        if (selectedSalle == null) { afficherInfo("Sélection requise", "Veuillez sélectionner une salle."); return; }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(selectedSalle.getStatut(),
                "Disponible", "Occup\u00e9e", "Pleine", "Nettoyage", "Maintenance");
        dialog.setTitle("Changer le statut");
        dialog.setHeaderText("Salle : " + selectedSalle.getNom());
        dialog.setContentText("Nouveau statut :");
        styleAlert(dialog);
        dialog.showAndWait().ifPresent(st -> {
            try { salleService.changerStatut(selectedSalle.getIdSalle(), st); chargerDonnees(); }
            catch (SQLException e) { afficherErreur("Erreur statut", e.getMessage()); }
        });
    }

    @FXML private void onVoirDashboard()  { naviguerVers("/ResourcesMateriel/module5/view/dashboardMateriel.fxml",         "REVIVE — Tableau de Bord", btnDashboard); }
    @FXML private void onVoirMateriel()   { naviguerVers("/ResourcesMateriel/module5/view/MaterielList.fxml",       "REVIVE — Matériel",        btnMateriel); }
    @FXML private void onVoirAmbulances() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceList.fxml",      "REVIVE — Gestion Ambulances", btnMateriel); }
    @FXML private void onVoirSimulation() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceSim.fxml",       "REVIVE — Simulation Trajet",  btnMateriel); }
    @FXML private void onVoirHistorique() { naviguerVers("/ResourcesMateriel/module5/view/HistoriqueMissions.fxml", "REVIVE — Historique",         btnMateriel); }
    @FXML private void onVoirRecherche()  { naviguerVers("/ResourcesMateriel/module5/view/RechercheGlobale.fxml",   "REVIVE — Recherche",          btnMateriel); }

    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) btnDashboard.getScene().getWindow();
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

    private void ouvrirFormulaire(SallePhysique salle) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ResourcesMateriel/module5/view/SalleForm.fxml"));
            Parent root = loader.load();
            SalleFormController ctrl = loader.getController();
            ctrl.setSalle(salle);

            Stage modal = new Stage();
            modal.initModality(Modality.APPLICATION_MODAL);
            modal.setTitle(salle == null ? "Nouvelle Salle" : "Modifier la Salle");
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

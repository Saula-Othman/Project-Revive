package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.AlerteMaintenance;
import pro.revive.services.ServicesMateriel.AmbulanceService;
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
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Contrôleur de la liste des ambulances avec affichage en cartes.
 */
public class AmbulanceListController implements Initializable {

    // ── Composants FXML ──────────────────────────────────────────────
    @FXML private FlowPane cardContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbFiltreEtat;
    @FXML private Label lblTotal;
    @FXML private Label lblTotalAmbulances;
    @FXML private Label lblDisponibles;
    @FXML private Label lblEnRoute;
    @FXML private Label lblAlertes;

    // ── Données ──────────────────────────────────────────────────────
    private final AmbulanceService ambulanceService = new AmbulanceService();
    private final ObservableList<Ambulance> data = FXCollections.observableArrayList();
    private FilteredList<Ambulance> filtered;
    private Ambulance selectedAmbulance;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filtered = new FilteredList<>(data, p -> true);
        configurerRecherche();
        configurerFiltres();
        chargerDonnees();
    }

    private void configurerRecherche() {
        txtSearch.textProperty().addListener((obs, old, val) -> appliquerFiltres());
    }

    private void configurerFiltres() {
        cmbFiltreEtat.getItems().addAll("Tous les états", "Disponible", "En route", "En panne", "En maintenance");
        cmbFiltreEtat.setValue("Tous les états");
        cmbFiltreEtat.valueProperty().addListener((obs, old, val) -> appliquerFiltres());
    }

    private void appliquerFiltres() {
        String terme = txtSearch.getText() == null ? "" : txtSearch.getText().toLowerCase().trim();
        String etat = cmbFiltreEtat.getValue();

        filtered.setPredicate(amb -> {
            boolean matchTexte = terme.isEmpty()
                    || amb.getNumeroSerie().toLowerCase().contains(terme)
                    || amb.getMarque().toLowerCase().contains(terme)
                    || (amb.getModele() != null && amb.getModele().toLowerCase().contains(terme));

            boolean matchEtat = etat == null || "Tous les états".equals(etat) || amb.getEtat().equals(etat);

            return matchTexte && matchEtat;
        });

        afficherCartes();
        lblTotal.setText("Total : " + filtered.size() + " / " + data.size() + " ambulance(s)");
    }

    private void chargerDonnees() {
        try {
            data.setAll(ambulanceService.findAll());
            selectedAmbulance = null;
            appliquerFiltres();
            chargerStatistiques();
        } catch (SQLException e) {
            afficherErreur("Erreur de chargement", e.getMessage());
        }
    }

    private void chargerStatistiques() {
        try {
            long total = data.size();
            long disponibles = data.stream().filter(a -> "Disponible".equals(a.getEtat())).count();
            long enRoute = data.stream().filter(a -> "En route".equals(a.getEtat())).count();
            
            // Compter les alertes actives
            long alertes = 0;
            for (Ambulance amb : data) {
                List<AlerteMaintenance> alerts = ambulanceService.getAlertesAmbulance(amb.getIdAmbulance());
                alertes += alerts.stream().filter(a -> "En attente".equals(a.getStatut())).count();
            }

            lblTotalAmbulances.setText(String.valueOf(total));
            lblDisponibles.setText(String.valueOf(disponibles));
            lblEnRoute.setText(String.valueOf(enRoute));
            lblAlertes.setText(String.valueOf(alertes));
        } catch (Exception e) {
            System.err.println("Erreur stats: " + e.getMessage());
        }
    }

    private void afficherCartes() {
        cardContainer.getChildren().clear();
        for (Ambulance amb : filtered) {
            cardContainer.getChildren().add(creerCarte(amb));
        }

        if (filtered.isEmpty()) {
            Label placeholder = new Label("Aucune ambulance trouvée");
            placeholder.getStyleClass().add("table-placeholder");
            cardContainer.getChildren().add(placeholder);
        }
    }

    private VBox creerCarte(Ambulance amb) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card-item");
        if (amb == selectedAmbulance) card.getStyleClass().add("card-item-selected");

        // Titre: Numéro de série
        Label title = new Label("🚑 " + amb.getNumeroSerie());
        title.getStyleClass().add("card-title");

        // Sous-titre: Marque + Modèle
        String subtitleText = amb.getMarque() + " " + (amb.getModele() != null ? amb.getModele() : "") + 
                              (amb.getAnneeFabrication() != null ? " (" + amb.getAnneeFabrication() + ")" : "");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("card-subtitle");

        // Badge état
        HBox badgeContainer = new HBox();
        Label badge = new Label(amb.getEtat());
        badge.getStyleClass().add("card-badge");
        switch (amb.getEtat()) {
            case "Disponible" -> badge.getStyleClass().add("badge-success");
            case "En route" -> badge.getStyleClass().add("badge-info");
            case "En panne" -> badge.getStyleClass().add("badge-danger");
            case "En maintenance" -> badge.getStyleClass().add("badge-warning");
        }
        badgeContainer.getChildren().add(badge);

        // Infos
        VBox info = new VBox(5);
        info.getChildren().addAll(
            creerInfoRow("Kilométrage:", String.format("%.0f km", amb.getKmTotal())),
            creerInfoRow("Dernière vidange:", amb.getDateDerniereVidange() != null ? 
                amb.getDateDerniereVidange().toString() : "N/A")
        );

        // Si l'ambulance est en route, ajouter un bouton pour voir la localisation
        if ("En route".equals(amb.getEtat())) {
            Button btnLocalisation = new Button("📍 Voir Localisation en Temps Réel");
            btnLocalisation.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                                    "-fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand; " +
                                    "-fx-font-size: 11px; -fx-font-weight: bold;");
            btnLocalisation.setOnAction(e -> {
                System.out.println("[AmbulanceList] Clic sur suivi pour ambulance ID: " + amb.getIdAmbulance() + " - " + amb.getNumeroSerie());
                ouvrirSimulationEnCours(amb);
                e.consume();
            });
            info.getChildren().add(btnLocalisation);
        }

        // Alertes IA
        try {
            List<AlerteMaintenance> alertes = ambulanceService.getAlertesAmbulance(amb.getIdAmbulance());
            long alertesActives = alertes.stream().filter(a -> "En attente".equals(a.getStatut())).count();
            if (alertesActives > 0) {
                Label lblAlertes = new Label("⚠️ " + alertesActives + " alerte(s) maintenance");
                lblAlertes.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 11px; -fx-font-weight: bold;");
                info.getChildren().add(lblAlertes);
            }
        } catch (SQLException e) {
            System.err.println("Erreur alertes: " + e.getMessage());
        }

        card.getChildren().addAll(title, subtitle, badgeContainer, info);

        // Double-clic pour voir les détails
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ouvrirDetails(amb);
            } else {
                selectedAmbulance = amb;
                afficherCartes();
            }
        });

        return card;
    }

    private void ouvrirSimulationEnCours(Ambulance amb) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/AmbulanceSim.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur de la simulation
            AmbulanceSimController simController = loader.getController();
            
            // Passer l'ID de l'ambulance à suivre
            simController.setAmbulanceASuivre(amb.getIdAmbulance());
            
            Stage stage = (Stage) cardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("REVIVE — Suivi Ambulance " + amb.getNumeroSerie());
            
            System.out.println("[Navigation] Ouverture simulation pour suivi de " + amb.getNumeroSerie() + " (ID: " + amb.getIdAmbulance() + ")");
        } catch (IOException e) {
            afficherErreur("Erreur", "Impossible d'ouvrir la simulation: " + e.getMessage());
        }
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

    @FXML
    private void onAjouter() {
        ouvrirFormulaire(null);
    }

    @FXML
    private void onRefresh() {
        chargerDonnees();
    }

    private void ouvrirDetails(Ambulance amb) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/AmbulanceDetails.fxml"));
            Parent root = loader.load();
            AmbulanceDetailsController ctrl = loader.getController();
            ctrl.setAmbulance(amb);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Détails Ambulance - " + amb.getNumeroSerie());
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    private void ouvrirFormulaire(Ambulance amb) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/AmbulanceForm.fxml"));
            Parent root = loader.load();
            AmbulanceFormController ctrl = loader.getController();
            ctrl.setAmbulance(amb);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(amb == null ? "Nouvelle Ambulance" : "Modifier Ambulance");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            chargerDonnees();
        } catch (IOException e) {
            afficherErreur("Erreur", e.getMessage());
        }
    }

    // ── Navigation ───────────────────────────────────────────────────

    @FXML private void onVoirDashboard()  { naviguerVers("/ResourcesMateriel/module5/view/dashboardMateriel.fxml",         "REVIVE — Dashboard"); }
    @FXML private void onVoirSalles()     { naviguerVers("/ResourcesMateriel/module5/view/SalleList.fxml",          "REVIVE — Salles"); }
    @FXML private void onVoirMateriel()   { naviguerVers("/ResourcesMateriel/module5/view/MaterielList.fxml",       "REVIVE — Matériel"); }
    @FXML private void onVoirSimulation() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceSim.fxml",       "REVIVE — Simulation"); }
    @FXML private void onVoirHistorique() { naviguerVers("/ResourcesMateriel/module5/view/HistoriqueMissions.fxml", "REVIVE — Historique"); }
    @FXML private void onVoirRecherche()  { naviguerVers("/ResourcesMateriel/module5/view/RechercheGlobale.fxml",   "REVIVE — Recherche"); }

    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) cardContainer.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("REVIVE — Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void naviguerVers(String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) cardContainer.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(titre);
        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Navigation", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    // ── Alertes ──────────────────────────────────────────────────────

    private void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private void styleAlert(Dialog<?> d) {
        try {
            d.getDialogPane().getStylesheets().add(
                    getClass().getResource("/ResourcesMateriel/module5/css/revive-dark.css").toExternalForm());
        } catch (Exception ignored) {}
    }
}

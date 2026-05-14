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
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    @FXML private Label lblUserName, lblUserRole, lblUserInitial;

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

        // Informations utilisateur
        String fullName = pro.revive.SessionManager.getFullName();
        String role = pro.revive.SessionManager.getRole();
        lblUserName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        lblUserRole.setText(role.isEmpty() ? "Personnel" : role);
        if (!fullName.isEmpty()) {
            lblUserInitial.setText(fullName.substring(0, 1).toUpperCase());
        }
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
        VBox card = new VBox(0);
        card.getStyleClass().add("card-item");
        if (amb == selectedAmbulance) card.getStyleClass().add("card-item-selected");

        // ── Barre colorée en haut selon l'état ──────────────────────
        Region topBar = new Region();
        topBar.setPrefHeight(5);
        topBar.setMinHeight(5);
        topBar.setMaxWidth(Double.MAX_VALUE);
        String barColor = switch (amb.getEtat()) {
            case "Disponible"    -> "#22C55E";
            case "En route"      -> "#0B4EA2";
            case "En panne"      -> "#EF4444";
            case "En maintenance"-> "#F59E0B";
            default              -> "#CBD5E1";
        };
        topBar.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 14 14 0 0;");

        // ── Corps de la carte ────────────────────────────────────────
        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 18, 18, 18));

        // En-tête : icône + numéro de série
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label icon = new Label("🚑");
        icon.setStyle("-fx-font-size: 22px;");

        VBox titleBlock = new VBox(2);
        Label title = new Label(amb.getNumeroSerie());
        title.getStyleClass().add("card-title");

        String subtitleText = amb.getMarque()
                + (amb.getModele() != null ? " " + amb.getModele() : "")
                + (amb.getAnneeFabrication() != null ? "  •  " + amb.getAnneeFabrication() : "");
        Label subtitle = new Label(subtitleText);
        subtitle.getStyleClass().add("card-subtitle");

        titleBlock.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);
        header.getChildren().addAll(icon, titleBlock);

        // Badge état
        Label badge = new Label(amb.getEtat());
        badge.getStyleClass().add("card-badge");
        switch (amb.getEtat()) {
            case "Disponible"     -> badge.getStyleClass().add("badge-success");
            case "En route"       -> badge.getStyleClass().add("badge-info");
            case "En panne"       -> badge.getStyleClass().add("badge-danger");
            case "En maintenance" -> badge.getStyleClass().add("badge-warning");
        }

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #EEF4FB;");

        // Infos kilométrage et maintenance
        VBox info = new VBox(6);
        info.getChildren().addAll(
            creerInfoRow("🛣  Kilométrage :", String.format("%.0f km", amb.getKmTotal())),
            creerInfoRow("🔧  Dernière vidange :", amb.getDateDerniereVidange() != null
                ? amb.getDateDerniereVidange().toString() : "Non renseignée")
        );

        // Bouton suivi si en route
        if ("En route".equals(amb.getEtat())) {
            Button btnLocalisation = new Button("📍  Suivi en temps réel");
            btnLocalisation.setStyle(
                "-fx-background-color: #0B4EA2; -fx-text-fill: white; " +
                "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-max-width: infinity;");
            btnLocalisation.setMaxWidth(Double.MAX_VALUE);
            btnLocalisation.setOnAction(e -> {
                ouvrirSimulationEnCours(amb);
                e.consume();
            });
            info.getChildren().add(btnLocalisation);
        }

        // Alertes maintenance
        try {
            List<AlerteMaintenance> alertes = ambulanceService.getAlertesAmbulance(amb.getIdAmbulance());
            long alertesActives = alertes.stream().filter(a -> "En attente".equals(a.getStatut())).count();
            if (alertesActives > 0) {
                HBox alertRow = new HBox(6);
                alertRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                alertRow.setStyle(
                    "-fx-background-color: #FEF2F2; -fx-padding: 7 12; " +
                    "-fx-background-radius: 8; -fx-border-color: #FECACA; " +
                    "-fx-border-width: 1; -fx-border-radius: 8;");
                Label alertLbl = new Label("⚠️  " + alertesActives + " alerte(s) maintenance");
                alertLbl.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px; -fx-font-weight: bold;");
                alertRow.getChildren().add(alertLbl);
                info.getChildren().add(alertRow);
            }
        } catch (SQLException e) {
            System.err.println("Erreur alertes: " + e.getMessage());
        }

        // Pied de carte : double-clic hint
        Label hint = new Label("Double-clic pour les détails");
        hint.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px; -fx-font-style: italic;");

        body.getChildren().addAll(header, badge, sep, info, hint);
        card.getChildren().addAll(topBar, body);

        // Interactions souris
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
        HBox row = new HBox(8);
        row.getStyleClass().add("card-info-row");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("card-info-label");
        Label val = new Label(value);
        val.getStyleClass().add("card-info-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(lbl, spacer, val);
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

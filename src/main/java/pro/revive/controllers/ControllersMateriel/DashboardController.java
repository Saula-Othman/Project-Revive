package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.AlerteMaintenance;
import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.entities.EntitiesMateriel.SallePhysique;
import pro.revive.services.ServicesMateriel.AmbulanceService;
import pro.revive.services.ServicesMateriel.MaterielService;
import pro.revive.services.ServicesMateriel.SalleService;
import pro.revive.utils.UtilesMateriel.NotificationService;
import pro.revive.utils.UtilesMateriel.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ── Cartes statistiques ──────────────────────────────────────────
    @FXML private Label lblTotalSalles, lblSallesDisponibles, lblSallesOccupees, lblSallesNettoyage;
    @FXML private Label lblTotalMateriel, lblMaterielFonctionnel, lblMaterielAReviser, lblMaterielReserve;
    @FXML private Label lblUserName, lblUserRole, lblUserInitial;

    // ── Graphiques ───────────────────────────────────────────────────
    @FXML private PieChart pieStatutSalles;
    @FXML private BarChart<String, Number> barMaterielParSalle;

    // ── Navigation ───────────────────────────────────────────────────
    @FXML private Button btnNavSalles, btnNavMateriel, btnNavAmbulances, btnNavSimulation;
    @FXML private Button btnNavHistorique, btnActualiser, btnRecherche;

    // ── Conteneurs pour le mode sombre ───────────────────────────────
    @FXML private ScrollPane scrollContent;
    @FXML private VBox       dashBody;

    // ── Services ─────────────────────────────────────────────────────
    private final SalleService    salleService    = new SalleService();
    private final MaterielService materielService = new MaterielService();
    private final AmbulanceService ambulanceService = new AmbulanceService();

    // ── Auto-refresh ─────────────────────────────────────────────────
    private Timeline autoRefreshTimeline;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chargerStatistiques();
        demarrerAutoRefresh();

        // Informations utilisateur
        String fullName = pro.revive.SessionManager.getFullName();
        String role = pro.revive.SessionManager.getRole();
        lblUserName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        lblUserRole.setText(role.isEmpty() ? "Personnel" : role);
        if (!fullName.isEmpty()) {
            lblUserInitial.setText(fullName.substring(0, 1).toUpperCase());
        }

        // Appliquer le thème sauvegardé dès que la scène est disponible
        Platform.runLater(() -> {
            Scene scene = btnActualiser.getScene();
            if (scene != null) {
                ThemeManager.register(scene);
            }
        });
    }

    // ── Auto-refresh toutes les 30 secondes ──────────────────────────
    private void demarrerAutoRefresh() {
        autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(30), e -> chargerStatistiques())
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();
    }

    // ── Chargement des données ────────────────────────────────────────
    private void chargerStatistiques() {
        try {
            List<SallePhysique>   salles    = salleService.findAll();
            List<MaterielUrgence> materiels = materielService.findAll();
            List<Ambulance>       ambulances = ambulanceService.findAll();

            // ── Stats salles ──────────────────────────────────────────
            long disponibles = salles.stream().filter(s -> "Disponible".equals(s.getStatut())).count();
            long occupees    = salles.stream().filter(s -> "Occup\u00e9e".equals(s.getStatut()) || "Pleine".equals(s.getStatut())).count();
            long nettoyage   = salles.stream().filter(s -> "Nettoyage".equals(s.getStatut())).count();
            long maintenance = salles.stream().filter(s -> "Maintenance".equals(s.getStatut())).count();

            lblTotalSalles.setText(String.valueOf(salles.size()));
            lblSallesDisponibles.setText(String.valueOf(disponibles));
            lblSallesOccupees.setText(String.valueOf(occupees));
            lblSallesNettoyage.setText(String.valueOf(nettoyage));

            // ── Stats matériel ────────────────────────────────────────
            long fonctionnel = materiels.stream().filter(m -> "Fonctionnel".equals(m.getEtat())).count();
            long aReviser    = materiels.stream().filter(m -> "A reviser".equals(m.getEtat())).count();
            long reserve     = materiels.stream().filter(m -> m.getIdSalle() == null).count();

            lblTotalMateriel.setText(String.valueOf(materiels.size()));
            lblMaterielFonctionnel.setText(String.valueOf(fonctionnel));
            lblMaterielAReviser.setText(String.valueOf(aReviser));
            lblMaterielReserve.setText(String.valueOf(reserve));

            // ── Notifications alertes critiques ───────────────────────
            verifierAlertesCritiques(ambulances);

            // ── PieChart statut salles ────────────────────────────────
            if (!salles.isEmpty()) {
                pieStatutSalles.setData(FXCollections.observableArrayList(
                    new PieChart.Data("Disponible (" + disponibles + ")", disponibles),
                    new PieChart.Data("Occup\u00e9e/Pleine (" + occupees + ")", occupees),
                    new PieChart.Data("Nettoyage (" + nettoyage + ")", nettoyage),
                    new PieChart.Data("Maintenance (" + maintenance + ")", maintenance)
                ));
                // Colorier les tranches apr\u00e8s le rendu JavaFX
                javafx.application.Platform.runLater(() -> {
                    if (pieStatutSalles.getData().size() >= 4) {
                        if (pieStatutSalles.getData().get(0).getNode() != null)
                            pieStatutSalles.getData().get(0).getNode().setStyle("-fx-pie-color: #4caf50;");
                        if (pieStatutSalles.getData().get(1).getNode() != null)
                            pieStatutSalles.getData().get(1).getNode().setStyle("-fx-pie-color: #ff9800;");
                        if (pieStatutSalles.getData().get(2).getNode() != null)
                            pieStatutSalles.getData().get(2).getNode().setStyle("-fx-pie-color: #4fc3f7;");
                        if (pieStatutSalles.getData().get(3).getNode() != null)
                            pieStatutSalles.getData().get(3).getNode().setStyle("-fx-pie-color: #e57373;");
                    }
                });
            }

            // ── BarChart matériel par salle ───────────────────────────
            barMaterielParSalle.getData().clear();
            XYChart.Series<String, Number> seriesFonc = new XYChart.Series<>();
            seriesFonc.setName("Fonctionnel");
            XYChart.Series<String, Number> seriesRev = new XYChart.Series<>();
            seriesRev.setName("À réviser");

            for (SallePhysique salle : salles) {
                List<MaterielUrgence> matSalle = materielService.findBySalle(salle.getIdSalle());
                long f = matSalle.stream().filter(m -> "Fonctionnel".equals(m.getEtat())).count();
                long r = matSalle.stream().filter(m -> "A reviser".equals(m.getEtat())).count();
                String nomCourt = salle.getNom().length() > 12 ? salle.getNom().substring(0, 12) + "…" : salle.getNom();
                seriesFonc.getData().add(new XYChart.Data<>(nomCourt, f));
                seriesRev.getData().add(new XYChart.Data<>(nomCourt, r));
            }
            barMaterielParSalle.getData().addAll(seriesFonc, seriesRev);

        } catch (SQLException e) {
            System.err.println("[Dashboard] Erreur chargement : " + e.getMessage());
        }
    }

    // ── Vérification alertes critiques ───────────────────────────────
    private void verifierAlertesCritiques(List<Ambulance> ambulances) {
        new Thread(() -> {
            try {
                for (Ambulance amb : ambulances) {
                    List<AlerteMaintenance> alertes = ambulanceService.getAlertesAmbulance(amb.getIdAmbulance());
                    long critiques = alertes.stream()
                        .filter(a -> "Critique".equals(a.getPriorite()) && "En attente".equals(a.getStatut()))
                        .count();
                    if (critiques > 0) {
                        Platform.runLater(() -> {
                            Stage stage = (Stage) btnActualiser.getScene().getWindow();
                            NotificationService.show(stage,
                                "⚠️ " + amb.getNumeroSerie() + " : " + critiques + " alerte(s) critique(s) de maintenance",
                                NotificationService.Type.WARNING);
                            NotificationService.playAlertSound();
                        });
                        break; // Une seule notification à la fois
                    }
                }
            } catch (Exception e) {
                System.err.println("[Dashboard] Erreur alertes: " + e.getMessage());
            }
        }).start();
    }


    // ── Recherche globale ─────────────────────────────────────────────
    @FXML
    private void onOuvrirRecherche() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/RechercheGlobale.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnRecherche.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("REVIVE — Recherche Globale");
        } catch (IOException e) {
            System.err.println("[Dashboard] Recherche: " + e.getMessage());
        }
    }

    // ── Navigation ───────────────────────────────────────────────────
    @FXML private void onNavSalles()     { naviguerVers("/ResourcesMateriel/module5/view/SalleList.fxml",         "REVIVE — Salles"); }
    @FXML private void onNavMateriel()   { naviguerVers("/ResourcesMateriel/module5/view/MaterielList.fxml",      "REVIVE — Matériel"); }
    @FXML private void onNavAmbulances() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceList.fxml",     "REVIVE — Ambulances"); }
    @FXML private void onNavSimulation() { naviguerVers("/ResourcesMateriel/module5/view/AmbulanceSim.fxml",      "REVIVE — Simulation"); }
    @FXML private void onNavHistorique() { naviguerVers("/ResourcesMateriel/module5/view/HistoriqueMissions.fxml","REVIVE — Historique"); }
    @FXML private void onActualiser()    { chargerStatistiques(); }
    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) btnNavSalles.getScene().getWindow();
            java.net.URL cssUrl = getClass().getResource("/ResourcesUser/images/css/user.css");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("REVIVE — Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void naviguerVers(String fxmlPath, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) btnNavSalles.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(titre);
        } catch (Exception e) {
            System.err.println("[Dashboard] Navigation erreur : " + e.getMessage());
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur de navigation");
            alert.setHeaderText("Impossible d'ouvrir la vue");
            alert.setContentText(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            alert.showAndWait();
        }
    }
}

package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.entities.EntitiesMateriel.SallePhysique;
import pro.revive.services.ServicesMateriel.AmbulanceService;
import pro.revive.services.ServicesMateriel.MaterielService;
import pro.revive.services.ServicesMateriel.SalleService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class RechercheGlobaleController implements Initializable {

    @FXML private TextField txtRecherche;
    @FXML private Label     lblResultats;

    @FXML private VBox      sectionAmbulances, sectionSalles, sectionMateriel, sectionVide;
    @FXML private FlowPane  resultatsAmbulances, resultatsSalles, resultatsMateriel;
    @FXML private Label     lblUserName, lblUserRole, lblUserInitial;

    private final AmbulanceService ambulanceService = new AmbulanceService();
    private final SalleService     salleService     = new SalleService();
    private final MaterielService  materielService  = new MaterielService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Recherche en temps réel
        txtRecherche.textProperty().addListener((obs, old, val) -> {
            if (val != null && val.trim().length() >= 2) {
                onRechercher();
            } else if (val == null || val.trim().isEmpty()) {
                onEffacer();
            }
        });

        // Informations utilisateur
        String fullName = pro.revive.SessionManager.getFullName();
        String role = pro.revive.SessionManager.getRole();
        lblUserName.setText(fullName.isEmpty() ? "Utilisateur" : fullName);
        lblUserRole.setText(role.isEmpty() ? "Personnel" : role);
        if (!fullName.isEmpty()) {
            lblUserInitial.setText(fullName.substring(0, 1).toUpperCase());
        }
    }

    @FXML
    private void onRechercher() {
        String terme = txtRecherche.getText();
        if (terme == null || terme.trim().length() < 2) {
            lblResultats.setText("Tapez au moins 2 caractères pour rechercher");
            return;
        }
        terme = terme.trim().toLowerCase();

        int totalResultats = 0;

        // ── Recherche Ambulances ──────────────────────────────────────
        try {
            final String t = terme;
            List<Ambulance> ambulances = ambulanceService.findAll().stream()
                .filter(a -> a.getNumeroSerie().toLowerCase().contains(t)
                          || a.getMarque().toLowerCase().contains(t)
                          || (a.getModele() != null && a.getModele().toLowerCase().contains(t))
                          || a.getEtat().toLowerCase().contains(t))
                .collect(Collectors.toList());

            resultatsAmbulances.getChildren().clear();
            for (Ambulance a : ambulances) {
                resultatsAmbulances.getChildren().add(creerCarteAmbulance(a));
            }
            afficherSection(sectionAmbulances, !ambulances.isEmpty());
            totalResultats += ambulances.size();
        } catch (SQLException e) {
            System.err.println("[Recherche] Ambulances: " + e.getMessage());
        }

        // ── Recherche Salles ──────────────────────────────────────────
        try {
            final String t = terme;
            List<SallePhysique> salles = salleService.findAll().stream()
                .filter(s -> s.getNom().toLowerCase().contains(t)
                          || s.getType().toLowerCase().contains(t)
                          || s.getStatut().toLowerCase().contains(t)
                          || (s.getLocalisation() != null && s.getLocalisation().toLowerCase().contains(t)))
                .collect(Collectors.toList());

            resultatsSalles.getChildren().clear();
            for (SallePhysique s : salles) {
                resultatsSalles.getChildren().add(creerCarteSalle(s));
            }
            afficherSection(sectionSalles, !salles.isEmpty());
            totalResultats += salles.size();
        } catch (SQLException e) {
            System.err.println("[Recherche] Salles: " + e.getMessage());
        }

        // ── Recherche Matériel ────────────────────────────────────────
        try {
            final String t = terme;
            List<MaterielUrgence> materiels = materielService.findAll().stream()
                .filter(m -> m.getNom().toLowerCase().contains(t)
                          || m.getEtat().toLowerCase().contains(t)
                          || (m.getNomSalle() != null && m.getNomSalle().toLowerCase().contains(t)))
                .collect(Collectors.toList());

            resultatsMateriel.getChildren().clear();
            for (MaterielUrgence m : materiels) {
                resultatsMateriel.getChildren().add(creerCarteMateriel(m));
            }
            afficherSection(sectionMateriel, !materiels.isEmpty());
            totalResultats += materiels.size();
        } catch (SQLException e) {
            System.err.println("[Recherche] Matériel: " + e.getMessage());
        }

        // ── Résumé ────────────────────────────────────────────────────
        if (totalResultats == 0) {
            afficherSection(sectionVide, true);
            lblResultats.setText("Aucun résultat pour \"" + txtRecherche.getText() + "\"");
        } else {
            afficherSection(sectionVide, false);
            lblResultats.setText(totalResultats + " résultat(s) trouvé(s) pour \"" + txtRecherche.getText() + "\"");
        }
    }

    @FXML
    private void onEffacer() {
        txtRecherche.clear();
        resultatsAmbulances.getChildren().clear();
        resultatsSalles.getChildren().clear();
        resultatsMateriel.getChildren().clear();
        afficherSection(sectionAmbulances, false);
        afficherSection(sectionSalles, false);
        afficherSection(sectionMateriel, false);
        afficherSection(sectionVide, false);
        lblResultats.setText("Tapez pour rechercher dans toute l'application");
    }

    private void afficherSection(VBox section, boolean visible) {
        section.setVisible(visible);
        section.setManaged(visible);
    }

    // ── Cartes de résultats ───────────────────────────────────────────
    private VBox creerCarteAmbulance(Ambulance a) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; " +
                      "-fx-pref-width: 260; -fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                      "-fx-border-width: 1; -fx-cursor: hand; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 3);");

        Label title = new Label("🚑 " + a.getNumeroSerie());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f2744;");

        Label sub = new Label(a.getMarque() + " " + (a.getModele() != null ? a.getModele() : ""));
        sub.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        String badgeColor = switch (a.getEtat()) {
            case "Disponible" -> "#10b981";
            case "En route"   -> "#3b82f6";
            case "En panne"   -> "#ef4444";
            default           -> "#f59e0b";
        };
        Label badge = new Label(a.getEtat());
        badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; " +
                       "-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label km = new Label(String.format("%.0f km", a.getKmTotal()));
        km.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        card.getChildren().addAll(title, sub, badge, km);
        card.setOnMouseClicked(e -> naviguer("/ResourcesMateriel/module5/view/AmbulanceList.fxml", "REVIVE — Ambulances"));
        return card;
    }

    private VBox creerCarteSalle(SallePhysique s) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; " +
                      "-fx-pref-width: 240; -fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                      "-fx-border-width: 1; -fx-cursor: hand; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 3);");

        Label title = new Label("🏠 " + s.getNom());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f2744;");

        Label type = new Label(s.getType() + " | " + s.getCapaciteMax() + " lits");
        type.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        String badgeColor = "Disponible".equals(s.getStatut()) ? "#10b981" : "#f59e0b";
        Label badge = new Label(s.getStatut());
        badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; " +
                       "-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");

        if (s.getLocalisation() != null && !s.getLocalisation().isEmpty()) {
            Label loc = new Label("📍 " + s.getLocalisation());
            loc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            card.getChildren().addAll(title, type, badge, loc);
        } else {
            card.getChildren().addAll(title, type, badge);
        }

        card.setOnMouseClicked(e -> naviguer("/ResourcesMateriel/module5/view/SalleList.fxml", "REVIVE — Salles"));
        return card;
    }

    private VBox creerCarteMateriel(MaterielUrgence m) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 16; " +
                      "-fx-pref-width: 240; -fx-border-color: #e2e8f0; -fx-border-radius: 12; " +
                      "-fx-border-width: 1; -fx-cursor: hand; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 3);");

        Label title = new Label("🔧 " + m.getNom());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #0f2744;");

        Label salle = new Label(m.getNomSalle() != null ? m.getNomSalle() : "En réserve");
        salle.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        String badgeColor = "Fonctionnel".equals(m.getEtat()) ? "#10b981" : "#ef4444";
        Label badge = new Label(m.getEtat());
        badge.setStyle("-fx-background-color: " + badgeColor + "; -fx-text-fill: white; " +
                       "-fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label qte = new Label("Quantité : " + m.getQuantite());
        qte.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        card.getChildren().addAll(title, salle, badge, qte);
        card.setOnMouseClicked(e -> naviguer("/ResourcesMateriel/module5/view/MaterielList.fxml", "REVIVE — Matériel"));
        return card;
    }

    // ── Navigation ────────────────────────────────────────────────────
    @FXML private void onNavDashboard()  { naviguer("/ResourcesMateriel/module5/view/dashboardMateriel.fxml",         "REVIVE — Dashboard"); }
    @FXML private void onNavSalles()     { naviguer("/ResourcesMateriel/module5/view/SalleList.fxml",          "REVIVE — Salles"); }
    @FXML private void onNavMateriel()   { naviguer("/ResourcesMateriel/module5/view/MaterielList.fxml",       "REVIVE — Matériel"); }
    @FXML private void onNavAmbulances() { naviguer("/ResourcesMateriel/module5/view/AmbulanceList.fxml",      "REVIVE — Ambulances"); }
    @FXML private void onNavSimulation() { naviguer("/ResourcesMateriel/module5/view/AmbulanceSim.fxml",       "REVIVE — Simulation"); }
    @FXML private void onNavHistorique() { naviguer("/ResourcesMateriel/module5/view/HistoriqueMissions.fxml", "REVIVE — Historique"); }

    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) txtRecherche.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            java.net.URL css = getClass().getResource("/ResourcesUser/images/css/user.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            stage.setTitle("REVIVE — Connexion");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void naviguer(String fxml, String titre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage stage = (Stage) txtRecherche.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(titre);
        } catch (IOException e) { System.err.println("[Recherche] Nav: " + e.getMessage()); }
    }
}

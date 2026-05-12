package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.services.ServicesMateriel.AmbulanceService;
import pro.revive.utils.MyConnection;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class HistoriqueMissionsController implements Initializable {

    @FXML private TableView<TrajetRow>          tableHistorique;
    @FXML private TableColumn<TrajetRow,String> colDate, colAmbulance, colDepart, colUrgence,
                                                 colDistance, colDuree, colStatut;
    @FXML private ComboBox<String>  cmbFiltreAmbulance, cmbFiltreStatut;
    @FXML private DatePicker        dpDebut, dpFin;
    @FXML private Label             lblTotal, lblTotalMissions, lblDistanceTotale,
                                    lblDureeMoyenne, lblMissionsAujourdhui, lblFooter;

    private final AmbulanceService ambulanceService = new AmbulanceService();
    private final ObservableList<TrajetRow> data     = FXCollections.observableArrayList();
    private       FilteredList<TrajetRow>   filtered;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurerColonnes();
        filtered = new FilteredList<>(data, p -> true);
        tableHistorique.setItems(filtered);

        // Politique de redimensionnement — remplit toute la largeur sans scroll horizontal
        tableHistorique.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Proportions des colonnes via les poids relatifs
        colDate.setMaxWidth(1f * Integer.MAX_VALUE * 13);
        colAmbulance.setMaxWidth(1f * Integer.MAX_VALUE * 15);
        colDepart.setMaxWidth(1f * Integer.MAX_VALUE * 18);
        colUrgence.setMaxWidth(1f * Integer.MAX_VALUE * 20);
        colDistance.setMaxWidth(1f * Integer.MAX_VALUE * 9);
        colDuree.setMaxWidth(1f * Integer.MAX_VALUE * 8);
        colStatut.setMaxWidth(1f * Integer.MAX_VALUE * 17);

        // Filtres
        cmbFiltreStatut.setItems(FXCollections.observableArrayList(
            "Tous les statuts", "Terminé", "En cours", "Annulé"
        ));
        cmbFiltreStatut.setValue("Tous les statuts");

        chargerAmbulancesFiltres();
        chargerDonnees();

        // Sélection → footer
        tableHistorique.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> {
            if (row != null) {
                lblFooter.setText("✅  " + row.getAmbulance() +
                    "  →  " + row.getUrgence() +
                    "   |   " + row.getDistance() +
                    "   |   " + row.getDuree() +
                    "   |   " + row.getStatut());
            }
        });
    }

    private void configurerColonnes() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDate()));
        colAmbulance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getAmbulance()));
        colDepart.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepart()));
        colUrgence.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getUrgence()));
        colDistance.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDistance()));
        colDuree.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDuree()));
        colStatut.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatut()));

        // Colorer le statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                switch (item) {
                    case "Terminé"  -> setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    case "En cours" -> setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    case "Annulé"   -> setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    default         -> setStyle("");
                }
            }
        });
    }

    private void chargerAmbulancesFiltres() {
        try {
            List<Ambulance> ambulances = ambulanceService.findAll();
            List<String> items = new ArrayList<>();
            items.add("Toutes les ambulances");
            ambulances.forEach(a -> items.add(a.getNumeroSerie() + " - " + a.getMarque()));
            cmbFiltreAmbulance.setItems(FXCollections.observableArrayList(items));
            cmbFiltreAmbulance.setValue("Toutes les ambulances");
        } catch (Exception e) {
            System.err.println("[Historique] Erreur chargement ambulances: " + e.getMessage());
        }
    }

    private void chargerDonnees() {
        try {
            data.clear();
            Connection conn = MyConnection.getInstance().getCnx();
            String sql = "SELECT t.*, a.numero_serie, a.marque FROM trajets t " +
                         "JOIN ambulances a ON t.id_ambulance = a.id_ambulance " +
                         "ORDER BY t.date_trajet DESC";
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    LocalDateTime dt = rs.getTimestamp("date_trajet").toLocalDateTime();
                    data.add(new TrajetRow(
                        rs.getInt("id_trajet"),
                        dt.format(fmt),
                        rs.getString("numero_serie") + " " + rs.getString("marque"),
                        rs.getString("localisation_depart"),
                        rs.getString("localisation_urgence"),
                        String.format("%.1f km", rs.getDouble("distance_km")),
                        rs.getInt("duree_minutes") + " min",
                        rs.getString("statut"),
                        dt
                    ));
                }
            }
            mettreAJourStats();
            lblTotal.setText(data.size() + " mission(s)");
        } catch (Exception e) {
            System.err.println("[Historique] Erreur: " + e.getMessage());
        }
    }

    private void mettreAJourStats() {
        // Calculer sur TOUTES les données (pas filtered)
        lblTotalMissions.setText(String.valueOf(data.size()));

        // Distance totale — parser "12.5 km" → 12.5
        double distTotale = 0;
        for (TrajetRow r : data) {
            try {
                String raw = r.getDistance().trim().replace(" km", "").replace(",", ".");
                distTotale += Double.parseDouble(raw);
            } catch (Exception ignored) {}
        }
        lblDistanceTotale.setText(String.format("%.1f km", distTotale));

        // Durée moyenne — parser "28 min" → 28
        double dureeMoy = 0;
        int count = 0;
        for (TrajetRow r : data) {
            try {
                String raw = r.getDuree().trim().replace(" min", "");
                dureeMoy += Integer.parseInt(raw);
                count++;
            } catch (Exception ignored) {}
        }
        lblDureeMoyenne.setText(count > 0 ? String.format("%.0f min", dureeMoy / count) : "0 min");

        // Missions aujourd'hui
        long aujourd = data.stream()
            .filter(r -> r.getDateTime() != null &&
                         r.getDateTime().toLocalDate().equals(LocalDate.now()))
            .count();
        lblMissionsAujourdhui.setText(String.valueOf(aujourd));
    }

    @FXML
    private void onFiltrer() {
        String ambulance = cmbFiltreAmbulance.getValue();
        String statut    = cmbFiltreStatut.getValue();
        LocalDate debut  = dpDebut.getValue();
        LocalDate fin    = dpFin.getValue();

        filtered.setPredicate(row -> {
            boolean matchAmb = ambulance == null || "Toutes les ambulances".equals(ambulance)
                || row.getAmbulance().contains(ambulance.split(" - ")[0]);
            boolean matchStat = statut == null || "Tous les statuts".equals(statut)
                || row.getStatut().equals(statut);
            boolean matchDebut = debut == null || (row.getDateTime() != null &&
                !row.getDateTime().toLocalDate().isBefore(debut));
            boolean matchFin = fin == null || (row.getDateTime() != null &&
                !row.getDateTime().toLocalDate().isAfter(fin));
            return matchAmb && matchStat && matchDebut && matchFin;
        });

        lblTotal.setText(filtered.size() + " / " + data.size() + " mission(s)");
    }

    @FXML
    private void onReinitialiser() {
        cmbFiltreAmbulance.setValue("Toutes les ambulances");
        cmbFiltreStatut.setValue("Tous les statuts");
        dpDebut.setValue(null);
        dpFin.setValue(null);
        filtered.setPredicate(p -> true);
        lblTotal.setText(data.size() + " mission(s)");
    }

    @FXML
    private void onActualiser() { chargerDonnees(); }

    @FXML
    private void onExportPdf() {
        PdfExportService.exporterHistoriqueMissions(
            filtered,
            (Stage) tableHistorique.getScene().getWindow()
        );
    }

    // ── Navigation ────────────────────────────────────────────────────
    @FXML private void onNavDashboard()  { naviguer("/ResourcesMateriel/module5/view/dashboardMateriel.fxml",        "REVIVE — Dashboard"); }
    @FXML private void onNavSalles()     { naviguer("/ResourcesMateriel/module5/view/SalleList.fxml",         "REVIVE — Salles"); }
    @FXML private void onNavMateriel()   { naviguer("/ResourcesMateriel/module5/view/MaterielList.fxml",      "REVIVE — Matériel"); }
    @FXML private void onNavAmbulances() { naviguer("/ResourcesMateriel/module5/view/AmbulanceList.fxml",     "REVIVE — Ambulances"); }
    @FXML private void onNavSimulation() { naviguer("/ResourcesMateriel/module5/view/AmbulanceSim.fxml",      "REVIVE — Simulation"); }

    @FXML private void onDeconnexion() {
        pro.revive.SessionManager.logout();
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(
                getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Stage stage = (Stage) tableHistorique.getScene().getWindow();
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
            Stage stage = (Stage) tableHistorique.getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle(titre);
        } catch (IOException e) { System.err.println("[Nav] " + e.getMessage()); }
    }

    // ── Inner class ───────────────────────────────────────────────────
    public static class TrajetRow {
        private final int id;
        private final String date, ambulance, depart, urgence, distance, duree, statut;
        private final LocalDateTime dateTime;

        public TrajetRow(int id, String date, String ambulance, String depart, String urgence,
                         String distance, String duree, String statut, LocalDateTime dateTime) {
            this.id = id; this.date = date; this.ambulance = ambulance;
            this.depart = depart; this.urgence = urgence; this.distance = distance;
            this.duree = duree; this.statut = statut; this.dateTime = dateTime;
        }

        public int getId()              { return id; }
        public String getDate()         { return date; }
        public String getAmbulance()    { return ambulance; }
        public String getDepart()       { return depart; }
        public String getUrgence()      { return urgence; }
        public String getDistance()     { return distance; }
        public String getDuree()        { return duree; }
        public String getStatut()       { return statut; }
        public LocalDateTime getDateTime() { return dateTime; }
    }
}

package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.AmbulanceDAO;
import pro.revive.daoAdmission.NotificationDAO;
import pro.revive.entities.EntitiesAdmission.AmbulanceSuivi;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class AmbulanceController implements Initializable {

    @FXML private WebView mapView;
    @FXML private VBox ambulanceListBox;
    @FXML private Label refreshLabel;
    @FXML private Label refreshDot;
    @FXML private Button alertTriageBtn;

    private WebEngine engine;
    private boolean mapLoaded = false;

    private final AmbulanceDAO dao = new AmbulanceDAO();
    private final NotificationDAO notifDAO = new NotificationDAO();
    private Timer refreshTimer;

    private List<AmbulanceSuivi> lastAmbulances = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        engine = mapView.getEngine();

        // JavaFX WebKit configuration
        engine.setJavaScriptEnabled(true);
        engine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // Charger la carte HTML locale (Leaflet + OpenStreetMap)
        URL mapUrl = getClass().getResource("/ResourceAdmission/urgence/fxml/map.html");
        if (mapUrl != null) {
            engine.load(mapUrl.toExternalForm());
        } else {
            System.err.println("❌ map.html introuvable !");
        }

        // Quand la page est chargée → injecter les marqueurs
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                mapLoaded = true;

                // Force Leaflet to recalculate the map container size
                // JavaFX layout settles in stages, so fire at several intervals
                engine.executeScript(
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 200);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 500);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 1000);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 2000);"
                );

                // Charger les données de démo puis DB
                loadTestData();
                updateAmbulanceList();
                injectAmbulancesInMap();
                startRefreshTimer();
            }
        });

        // When WebView dimensions change, re-invalidate the map
        mapView.widthProperty().addListener((o, ov, nv) -> {
            if (mapLoaded) {
                engine.executeScript(
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 100);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 500);"
                );
            }
        });
        mapView.heightProperty().addListener((o, ov, nv) -> {
            if (mapLoaded) {
                engine.executeScript(
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 100);" +
                    "setTimeout(function(){ if(typeof map!=='undefined') map.invalidateSize({animate:false,pan:false}); }, 500);"
                );
            }
        });
    }

    // ─── Données de démo ──────────────────────────────────
    private void loadTestData() {
        lastAmbulances.clear();

        AmbulanceSuivi a1 = new AmbulanceSuivi();
        a1.setId(1); a1.setMatricule("TUN-AMB-003");
        a1.setLatitude(36.8065); a1.setLongitude(10.1815);
        a1.setEtaMinutes(8); a1.setNiveauUrgence("Critique");
        a1.setPatientInfoProvisoire("Homme ~50 ans, douleur thoracique, TA basse");
        a1.setStatut("En route");
        lastAmbulances.add(a1);

        AmbulanceSuivi a2 = new AmbulanceSuivi();
        a2.setId(2); a2.setMatricule("TUN-AMB-001");
        a2.setLatitude(36.8420); a2.setLongitude(10.1350);
        a2.setEtaMinutes(15); a2.setNiveauUrgence("Urgent");
        a2.setPatientInfoProvisoire("Femme ~30 ans, détresse respiratoire");
        a2.setStatut("En route");
        lastAmbulances.add(a2);

        AmbulanceSuivi a3 = new AmbulanceSuivi();
        a3.setId(3); a3.setMatricule("TUN-AMB-007");
        a3.setLatitude(36.7950); a3.setLongitude(10.1900);
        a3.setEtaMinutes(22); a3.setNiveauUrgence("Modéré");
        a3.setPatientInfoProvisoire("Enfant ~8 ans, fracture supposée");
        a3.setStatut("En route");
        lastAmbulances.add(a3);
    }

    // ─── Injecter les ambulances dans la carte Leaflet ────
    private void injectAmbulancesInMap() {
        if (!mapLoaded || engine == null) return;

        // Construire JSON à la main (pas de dépendance gson nécessaire)
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < lastAmbulances.size(); i++) {
            AmbulanceSuivi a = lastAmbulances.get(i);
            if (i > 0) json.append(",");
            json.append("{");
            json.append("\"lat\":").append(a.getLatitude()).append(",");
            json.append("\"lng\":").append(a.getLongitude()).append(",");
            json.append("\"matricule\":\"").append(escape(a.getMatricule())).append("\",");
            json.append("\"niveau\":\"").append(escape(a.getNiveauUrgence())).append("\",");
            json.append("\"eta\":").append(a.getEtaMinutes()).append(",");
            json.append("\"patient\":\"").append(escape(a.getPatientInfoProvisoire())).append("\"");
            json.append("}");
        }
        json.append("]");

        String script = "updateAmbulances('" + json.toString().replace("'", "\\'") + "');";
        engine.executeScript(script);
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                .replace("\n", " ").replace("\r", "");
    }

    // ─── Timer rafraîchissement ───────────────────────────
    private void startRefreshTimer() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                Platform.runLater(() -> refreshData());
            }
        }, 30_000, 30_000);
    }

    private void refreshData() {
        try {
            List<AmbulanceSuivi> db = dao.findActiveAmbulances();
            if (db != null && !db.isEmpty()) lastAmbulances = db;
        } catch (Exception e) {
            System.err.println("DB ambulance: " + e.getMessage());
        }
        updateAmbulanceList();
        injectAmbulancesInMap();
        updateRefreshIndicator();
    }

    // ─── Liste sidebar ────────────────────────────────────
    private void updateAmbulanceList() {
        ambulanceListBox.getChildren().clear();
        if (lastAmbulances.isEmpty()) {
            Label empty = new Label("Aucune ambulance en route");
            empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px; -fx-padding:20;");
            ambulanceListBox.getChildren().add(empty);
            return;
        }
        for (AmbulanceSuivi amb : lastAmbulances)
            ambulanceListBox.getChildren().add(createAmbCard(amb));
    }

    private VBox createAmbCard(AmbulanceSuivi amb) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE);

        String niv = amb.getNiveauUrgence() != null ? amb.getNiveauUrgence() : "";
        String col;
        switch (niv) {
            case "Critique": col = "#dc2626"; break;
            case "Urgent":   col = "#ea580c"; break;
            case "Modéré":   col = "#ca8a04"; break;
            default:         col = "#16a34a"; break;
        }

        card.setStyle(
            "-fx-background-color:white;" +
            "-fx-border-color:" + col + ";" +
            "-fx-border-width:0 0 0 4;" +
            "-fx-border-radius:0 8 8 0;" +
            "-fx-background-radius:8;" +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"
        );

        // Header : badge + matricule + ETA
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(niv.toUpperCase());
        badge.setStyle("-fx-background-color:" + col + ";-fx-text-fill:white;" +
            "-fx-font-size:9px;-fx-font-weight:bold;-fx-padding:2 7 2 7;-fx-background-radius:10;");

        Label mat = new Label("🚑 " + amb.getMatricule());
        mat.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1e293b;");
        HBox.setHgrow(mat, Priority.ALWAYS);

        String etaCol = amb.getEtaMinutes() <= 5 ? "#dc2626" :
                        amb.getEtaMinutes() <= 10 ? "#ea580c" : "#17A2A0";
        Label etaNum = new Label(amb.getEtaMinutes() + " min");
        etaNum.setStyle("-fx-font-size:17px;-fx-font-weight:bold;-fx-text-fill:" + etaCol + ";");
        Label etaLbl = new Label("ETA");
        etaLbl.setStyle("-fx-font-size:9px;-fx-text-fill:#94a3b8;");
        VBox etaBox = new VBox(0, etaLbl, etaNum);
        etaBox.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(badge, mat, etaBox);
        card.getChildren().add(header);

        // Info patient
        if (amb.getPatientInfoProvisoire() != null && !amb.getPatientInfoProvisoire().isEmpty()) {
            Label info = new Label("👤 " + amb.getPatientInfoProvisoire());
            info.setStyle("-fx-font-size:11.5px;-fx-text-fill:#475569;-fx-wrap-text:true;");
            info.setMaxWidth(280);
            card.getChildren().add(info);
        }

        // Bouton pré-inscrire (Critique ou Urgent)
        if ("Critique".equals(niv) || "Urgent".equals(niv)) {
            Button btn = new Button("Pré-inscrire ce patient");
            btn.setStyle(
                "-fx-background-color:" + col + ";-fx-text-fill:white;" +
                "-fx-font-size:11px;-fx-font-weight:bold;-fx-padding:5 12;" +
                "-fx-background-radius:4;-fx-cursor:hand;"
            );
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> openAdmissionForm(amb));
            card.getChildren().add(btn);
        }

        return card;
    }

    private void updateRefreshIndicator() {
        if (refreshDot != null) refreshDot.setStyle("-fx-text-fill:#16a34a;-fx-font-size:10px;");
        if (refreshLabel != null) {
            String t = java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            refreshLabel.setText("Actualisé à " + t);
        }
    }

    // ─── Actions FXML ─────────────────────────────────────
    @FXML private void handlePreInscription() { openAdmissionForm(null); }

    @FXML
    private void handleAlerteTriage() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Alerter le Triage");
        dlg.setHeaderText("⚠ Envoyer une alerte urgente au Module Triage");
        dlg.setContentText("Message :");
        dlg.showAndWait().ifPresent(msg -> {
            if (msg.trim().isEmpty()) return;
            try {
                notifDAO.sendNotification("MODULE_1", "MODULE_2", "URGENCE_CRITIQUE",
                    "⚠ ALERTE TRIAGE", msg, null, null, null);
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Alerte envoyée"); ok.setHeaderText(null);
                ok.setContentText("✓ L'alerte a été transmise au module Triage.");
                ok.showAndWait();
            } catch (Exception e) { showError("Impossible d'envoyer: " + e.getMessage()); }
        });
    }

    private void openAdmissionForm(AmbulanceSuivi amb) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ResourceAdmission/urgence/fxml/AdmissionForm.fxml"));
            Parent content = loader.load();
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Nouvelle Admission");
            Scene scene = new Scene(content);
            scene.getStylesheets().add(
                getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            refreshData();
        } catch (Exception e) { e.printStackTrace(); showError("Erreur: " + e.getMessage()); }
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}

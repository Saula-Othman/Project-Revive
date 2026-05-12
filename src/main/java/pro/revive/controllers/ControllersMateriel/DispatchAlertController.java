package pro.revive.controllers.ControllersMateriel;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import pro.revive.services.ServicesMateriel.EmailAlert;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class DispatchAlertController implements Initializable {

    @FXML private Label   lblSender;
    @FXML private Label   lblSubject;
    @FXML private TextArea txtBody;
    @FXML private Label   lblLocation;
    @FXML private Label   lblDistance;
    @FXML private Label   lblTime;
    @FXML private Label   lblCalcStatus;
    @FXML private Button  btnAccept;
    @FXML private Button  btnRefuse;

    private EmailAlert alert;
    private Consumer<Boolean> resultCallback; // true = accepté, false = refusé

    // Résultats du calcul d'itinéraire
    private double calculatedDistanceKm = 0;
    private double calculatedDurationSec = 0;
    private boolean routeCalculated = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Désactiver Accepter jusqu'à ce que le calcul soit terminé
        btnAccept.setDisable(true);
    }

    /**
     * Injecter les données de l'alerte et lancer le calcul d'itinéraire.
     */
    public void setAlert(EmailAlert alert, Consumer<Boolean> callback) {
        this.alert = alert;
        this.resultCallback = callback;

        // Remplir les champs
        lblSender.setText(alert.getSenderEmail());
        lblSubject.setText(alert.getSubject());
        txtBody.setText(alert.getBodyPreview().isBlank() ? "(message vide)" : alert.getBodyPreview());
        lblLocation.setText(alert.getLocation());

        // Lancer le calcul en arrière-plan
        new Thread(this::calculateRoute).start();
    }

    // ─────────────────────────────────────────────────────────────────
    // Calcul de l'itinéraire Base → Urgence
    // ─────────────────────────────────────────────────────────────────

    private void calculateRoute() {
        try {
            Platform.runLater(() -> lblCalcStatus.setText("⏳ Géocodage de la localisation..."));

            double[] baseCoord = geocode("Clinique Hannibal, Tunis");
            double[] urgCoord  = geocode(alert.getLocation());

            if (baseCoord == null || urgCoord == null) {
                Platform.runLater(() -> {
                    lblCalcStatus.setText("⚠️ Impossible de géocoder la localisation.");
                    lblDistance.setText("N/A");
                    lblTime.setText("N/A");
                    btnAccept.setDisable(false); // Permettre quand même d'accepter
                });
                return;
            }

            Platform.runLater(() -> lblCalcStatus.setText("⏳ Calcul de l'itinéraire..."));

            // Appel OSRM
            String urlStr = String.format(java.util.Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                baseCoord[1], baseCoord[0], urgCoord[1], urgCoord[0]);

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);

                JSONObject json = new JSONObject(response.toString());
                JSONArray routes = json.getJSONArray("routes");

                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    calculatedDurationSec = route.getDouble("duration");
                    calculatedDistanceKm  = route.getDouble("distance") / 1000.0;
                    routeCalculated = true;

                    final double distKm  = Math.round(calculatedDistanceKm * 10.0) / 10.0;
                    final int    minutes = (int) Math.ceil(calculatedDurationSec / 60.0);

                    Platform.runLater(() -> {
                        lblDistance.setText(distKm + " km");
                        lblTime.setText(minutes + " min");
                        lblCalcStatus.setText("✅ Itinéraire calculé avec succès.");
                        btnAccept.setDisable(false);
                    });
                } else {
                    Platform.runLater(() -> {
                        lblCalcStatus.setText("⚠️ Aucun itinéraire trouvé.");
                        lblDistance.setText("N/A");
                        lblTime.setText("N/A");
                        btnAccept.setDisable(false);
                    });
                }
            }

        } catch (Exception e) {
            System.err.println("[DispatchAlert] Erreur calcul itinéraire : " + e.getMessage());
            Platform.runLater(() -> {
                lblCalcStatus.setText("⚠️ Erreur réseau : " + e.getMessage());
                lblDistance.setText("N/A");
                lblTime.setText("N/A");
                btnAccept.setDisable(false);
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Actions boutons
    // ─────────────────────────────────────────────────────────────────

    @FXML
    private void onAccept() {
        closeWindow();
        if (resultCallback != null) resultCallback.accept(true);
    }

    @FXML
    private void onRefuse() {
        closeWindow();
        if (resultCallback != null) resultCallback.accept(false);
    }

    private void closeWindow() {
        Stage stage = (Stage) btnAccept.getScene().getWindow();
        stage.close();
    }

    // ─────────────────────────────────────────────────────────────────
    // Géocodage
    // ─────────────────────────────────────────────────────────────────

    private double[] geocode(String address) throws Exception {
        // Coordonnées directes (lat,lon)
        if (address.matches("^-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?$")) {
            String[] parts = address.split(",");
            return new double[]{Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim())};
        }

        String query  = URLEncoder.encode(address, StandardCharsets.UTF_8);
        String urlStr = "https://nominatim.openstreetmap.org/search?q=" + query + "&format=json&limit=1";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "ReviveApp/1.0");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) response.append(line);

            JSONArray arr = new JSONArray(response.toString());
            if (arr.length() > 0) {
                JSONObject obj = arr.getJSONObject(0);
                return new double[]{obj.getDouble("lat"), obj.getDouble("lon")};
            }
        }
        return null;
    }
}

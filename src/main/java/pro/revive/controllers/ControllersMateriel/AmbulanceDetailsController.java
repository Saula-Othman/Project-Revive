package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.AlerteMaintenance;
import pro.revive.entities.EntitiesMateriel.Trajet;
import pro.revive.services.ServicesMateriel.AmbulanceService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AmbulanceDetailsController implements Initializable {

    @FXML private Label lblNumero, lblEtatBadge, lblMarqueModele;
    @FXML private Label lblKmTotal, lblNbTrajets, lblNbAlertes, lblAnnee;
    @FXML private Label lblDateVidange, lblKmVidange, lblDatePneus, lblKmPneus;
    @FXML private Label lblNoAlertes;
    @FXML private VBox boxAlertes;

    @FXML private TableView<Trajet> tableTrajets;
    @FXML private TableColumn<Trajet, LocalDateTime> colDate;
    @FXML private TableColumn<Trajet, String> colDepart, colUrgence, colStatut;
    @FXML private TableColumn<Trajet, Double> colDistance;
    @FXML private TableColumn<Trajet, Integer> colDuree;

    private final AmbulanceService ambulanceService = new AmbulanceService();
    private Ambulance ambulance;
    private Runnable onUpdateCallback;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableTrajets();
    }

    private void setupTableTrajets() {
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateTrajet"));
        colDate.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
            }
        });

        colDepart.setCellValueFactory(new PropertyValueFactory<>("localisationDepart"));
        colUrgence.setCellValueFactory(new PropertyValueFactory<>("localisationUrgence"));
        colDistance.setCellValueFactory(new PropertyValueFactory<>("distanceKm"));
        colDistance.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double dist, boolean empty) {
                super.updateItem(dist, empty);
                setText(empty || dist == null ? null : String.format("%.1f km", dist));
            }
        });

        colDuree.setCellValueFactory(new PropertyValueFactory<>("dureeMinutes"));
        colDuree.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer duree, boolean empty) {
                super.updateItem(duree, empty);
                setText(empty || duree == null ? null : duree + " min");
            }
        });

        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String statut, boolean empty) {
                super.updateItem(statut, empty);
                if (empty || statut == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(statut);
                    badge.setStyle(getStatutStyle(statut));
                    setGraphic(badge);
                }
            }
        });
    }

    public void setAmbulance(Ambulance ambulance) {
        this.ambulance = ambulance;
        chargerDetails();
    }

    public void setOnUpdateCallback(Runnable callback) {
        this.onUpdateCallback = callback;
    }

    private void chargerDetails() {
        lblNumero.setText(ambulance.getNumeroSerie());
        lblMarqueModele.setText(ambulance.getMarque() + " " + (ambulance.getModele() != null ? ambulance.getModele() : ""));
        lblEtatBadge.setText(ambulance.getEtat());
        lblEtatBadge.setStyle(getBadgeStyle(ambulance.getEtat()));

        lblKmTotal.setText(String.format("%.0f km", ambulance.getKmTotal()));
        lblAnnee.setText(ambulance.getAnneeFabrication() != null ? String.valueOf(ambulance.getAnneeFabrication()) : "N/A");

        // Maintenance
        if (ambulance.getDateDerniereVidange() != null) {
            lblDateVidange.setText(ambulance.getDateDerniereVidange().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            lblKmVidange.setText(String.format("%.0f km", ambulance.getKmDerniereVidange()));
        }
        if (ambulance.getDateDerniersPneus() != null) {
            lblDatePneus.setText(ambulance.getDateDerniersPneus().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            lblKmPneus.setText(String.format("%.0f km", ambulance.getKmDerniersPneus()));
        }

        // Charger trajets et alertes
        new Thread(() -> {
            try {
                List<Trajet> trajets = ambulanceService.getTrajetsAmbulance(ambulance.getIdAmbulance());
                List<AlerteMaintenance> alertes = ambulanceService.getAlertesAmbulance(ambulance.getIdAmbulance());

                Platform.runLater(() -> {
                    lblNbTrajets.setText(String.valueOf(trajets.size()));
                    tableTrajets.setItems(FXCollections.observableArrayList(trajets));

                    long nbEnAttente = alertes.stream().filter(a -> a.getStatut().equals("En attente")).count();
                    lblNbAlertes.setText(String.valueOf(nbEnAttente));

                    afficherAlertes(alertes);
                });
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void afficherAlertes(List<AlerteMaintenance> alertes) {
        boxAlertes.getChildren().clear();

        List<AlerteMaintenance> enAttente = alertes.stream()
                .filter(a -> a.getStatut().equals("En attente"))
                .toList();

        if (enAttente.isEmpty()) {
            lblNoAlertes.setVisible(true);
            lblNoAlertes.setManaged(true);
        } else {
            lblNoAlertes.setVisible(false);
            lblNoAlertes.setManaged(false);

            for (AlerteMaintenance alerte : enAttente) {
                VBox alerteBox = creerAlerteCard(alerte);
                boxAlertes.getChildren().add(alerteBox);
            }
        }
    }

    private VBox creerAlerteCard(AlerteMaintenance alerte) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #1e293b; -fx-padding: 14; -fx-background-radius: 8; " +
                     "-fx-border-color: " + getPrioriteColor(alerte.getPriorite()) + "; -fx-border-width: 2; -fx-border-radius: 8;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblType = new Label(getIconType(alerte.getTypeMaintenance()) + "  " + alerte.getTypeMaintenance());
        lblType.setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 13px; -fx-font-weight: bold;");

        Label lblPriorite = new Label(alerte.getPriorite());
        lblPriorite.setStyle("-fx-background-color: " + getPrioriteColor(alerte.getPriorite()) + "; " +
                           "-fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 10; " +
                           "-fx-font-size: 10px; -fx-font-weight: bold;");

        header.getChildren().addAll(lblType, lblPriorite);

        Label lblDesc = new Label(alerte.getDescription());
        lblDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px;");
        lblDesc.setWrapText(true);

        Button btnEffectue = new Button("✅  Marquer effectuée");
        btnEffectue.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-padding: 6 12; " +
                           "-fx-background-radius: 6; -fx-font-size: 11px; -fx-cursor: hand;");
        btnEffectue.setOnAction(e -> marquerEffectuee(alerte));

        card.getChildren().addAll(header, lblDesc, btnEffectue);
        return card;
    }

    private void marquerEffectuee(AlerteMaintenance alerte) {
        try {
            ambulanceService.marquerAlerteEffectuee(alerte.getIdAlerte());
            chargerDetails();
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onAnalyserMaintenance() {
        new Thread(() -> {
            try {
                ambulanceService.analyserMaintenanceIA(ambulance.getIdAmbulance());
                Platform.runLater(() -> {
                    showInfo("Analyse terminée", "L'analyse de maintenance a été effectuée avec succès.");
                    chargerDetails();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError("Erreur", e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onModifier() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesMateriel/module5/view/AmbulanceForm.fxml"));
            Parent root = loader.load();

            AmbulanceFormController controller = loader.getController();
            controller.setAmbulance(ambulance);
            controller.setOnSaveCallback(() -> {
                try {
                    ambulance = ambulanceService.findById(ambulance.getIdAmbulance());
                    chargerDetails();
                    if (onUpdateCallback != null) onUpdateCallback.run();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UNDECORATED);
            dialog.setScene(new Scene(root));
            dialog.show();
        } catch (Exception e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void onSupprimer() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'ambulance " + ambulance.getNumeroSerie() + " ?");
        alert.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                ambulanceService.delete(ambulance.getIdAmbulance());
                if (onUpdateCallback != null) onUpdateCallback.run();
                onFermer();
            } catch (SQLException e) {
                showError("Erreur", e.getMessage());
            }
        }
    }

    @FXML
    private void onFermer() {
        Stage stage = (Stage) lblNumero.getScene().getWindow();
        stage.close();
    }

    // Helpers
    private String getBadgeStyle(String etat) {
        return switch (etat) {
            case "Disponible" -> "-fx-background-color: rgba(34,197,94,0.2); -fx-text-fill: #22c55e; " +
                               "-fx-padding: 6 14; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;";
            case "En route" -> "-fx-background-color: rgba(56,189,248,0.2); -fx-text-fill: #38bdf8; " +
                             "-fx-padding: 6 14; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;";
            case "En panne" -> "-fx-background-color: rgba(239,68,68,0.2); -fx-text-fill: #ef4444; " +
                             "-fx-padding: 6 14; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;";
            default -> "-fx-background-color: rgba(148,163,184,0.2); -fx-text-fill: #94a3b8; " +
                      "-fx-padding: 6 14; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;";
        };
    }

    private String getStatutStyle(String statut) {
        return switch (statut) {
            case "Terminé" -> "-fx-background-color: rgba(34,197,94,0.2); -fx-text-fill: #22c55e; " +
                            "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;";
            case "En cours" -> "-fx-background-color: rgba(251,191,36,0.2); -fx-text-fill: #fbbf24; " +
                             "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;";
            default -> "-fx-background-color: rgba(148,163,184,0.2); -fx-text-fill: #94a3b8; " +
                      "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold;";
        };
    }

    private String getPrioriteColor(String priorite) {
        return switch (priorite) {
            case "Critique" -> "#ef4444";
            case "Élevée" -> "#f97316";
            case "Moyenne" -> "#fbbf24";
            case "Faible" -> "#22c55e";
            default -> "#94a3b8";
        };
    }

    private String getIconType(String type) {
        return switch (type) {
            case "Vidange" -> "🛢";
            case "Pneus" -> "🛞";
            case "Freins" -> "🛑";
            case "Révision complète" -> "🔧";
            default -> "⚙️";
        };
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

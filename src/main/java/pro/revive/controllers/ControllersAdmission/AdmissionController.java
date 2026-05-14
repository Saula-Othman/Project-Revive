package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.AdmissionDAO;
import pro.revive.entities.EntitiesAdmission.Admission;
import pro.revive.utils.UtilesAdmission.ToastNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdmissionController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilter;
    @FXML private TableView<Admission> admissionTable;
    // @FXML private TableColumn<Admission, Integer> colId; // ID masqué UI
    @FXML private TableColumn<Admission, String> colPatient;
    @FXML private TableColumn<Admission, String> colMode;
    @FXML private TableColumn<Admission, String> colMotif;
    @FXML private TableColumn<Admission, String> colStatut;
    @FXML private TableColumn<Admission, String> colPriorite;
    @FXML private TableColumn<Admission, String> colDate;
    @FXML private TableColumn<Admission, Boolean> colActif;
    @FXML private TableColumn<Admission, Void> colActions;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private VBox root;

    private final AdmissionDAO dao = new AdmissionDAO();
    private ObservableList<Admission> allAdmissions = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statutFilter.setItems(FXCollections.observableArrayList(
            "Tous", "En attente triage", "En triage", "En consultation", "Hospitalisé", "Sorti", "Transféré"
        ));
        statutFilter.setValue("Tous");
        statutFilter.setOnAction(e -> applyFilters());
        setupColumns();
        loadAdmissions();
    }

    private void setupColumns() {
        // colId hidden - IDs not shown in UI
        colMode.setCellValueFactory(new PropertyValueFactory<>("modeArrivee"));
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motifAdmission"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colPriorite.setCellValueFactory(new PropertyValueFactory<>("prioriteInitiale"));

        colPatient.setCellValueFactory(data -> {
            Admission a = data.getValue();
            if (a.getPatient() != null) {
                return new javafx.beans.property.SimpleStringProperty(a.getPatient().getNomComplet());
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });

        colDate.setCellValueFactory(data -> {
            Admission a = data.getValue();
            if (a.getDateAdmission() != null) {
                return new javafx.beans.property.SimpleStringProperty(a.getDateAdmission().format(FMT));
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });

        // Actif indicator column
        if (colActif != null) {
            colActif.setCellValueFactory(data ->
                new javafx.beans.property.SimpleBooleanProperty(data.getValue().isActif()));
            colActif.setCellFactory(col -> new TableCell<>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setStyle(""); return; }
                    if (item) {
                        setText("✅ Actif");
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    } else {
                        setText("🔴 Inactif");
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    }
                }
            });
        }

        // Color-code statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                String color;
                if ("En attente triage".equals(item))   color = "#f97316";
                else if ("En triage".equals(item))      color = "#eab308";
                else if ("En consultation".equals(item))color = "#3b82f6";
                else if ("Hospitalisé".equals(item))    color = "#8b5cf6";
                else if ("Sorti".equals(item))          color = "#6b7280";
                else if (item != null && item.startsWith("Transf"))      color = "#06b6d4";
                else                                     color = "#64748b";
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final HBox box = new HBox(6, editBtn, delBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().add("btn-table-action");
                delBtn.getStyleClass().add("btn-table-action-danger");
                editBtn.setOnAction(e -> {
                    Admission a = getTableView().getItems().get(getIndex());
                    handleEdit(a);
                });
                delBtn.setOnAction(e -> {
                    Admission a = getTableView().getItems().get(getIndex());
                    handleDelete(a);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadAdmissions() {
        try {
            List<Admission> list = dao.findAll();
            allAdmissions = FXCollections.observableArrayList(list);
            admissionTable.setItems(allAdmissions);
            statusLabel.setText("Admissions chargées");
            countLabel.setText(list.size() + " admissions");
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML private void handleSearch() { applyFilters(); }

    private void applyFilters() {
        String query = searchField.getText().toLowerCase().trim();
        String statut = statutFilter.getValue();
        ObservableList<Admission> filtered = allAdmissions.filtered(a -> {
            boolean matchQuery = query.isEmpty()
                || (a.getPatient() != null && a.getPatient().getNomComplet().toLowerCase().contains(query))
                || (a.getMotifAdmission() != null && a.getMotifAdmission().toLowerCase().contains(query));
            boolean matchStatut = "Tous".equals(statut) || statut == null || statut.equals(a.getStatut());
            return matchQuery && matchStatut;
        });
        admissionTable.setItems(filtered);
        countLabel.setText(filtered.size() + " admissions");
    }

    @FXML private void handleClearFilters() {
        searchField.clear();
        statutFilter.setValue("Tous");
        admissionTable.setItems(allAdmissions);
        countLabel.setText(allAdmissions.size() + " admissions");
    }

    @FXML private void handleAdd() {
        openAdmissionForm(null);
    }

    private void handleEdit(Admission a) {
        openAdmissionForm(a);
    }

    private void handleDelete(Admission a) {
        String patientNom = a.getPatient() != null ? a.getPatient().getNomComplet() : "patient inconnu";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer cette admission ?");
        confirm.setContentText("Supprimer l'admission pour " + patientNom + " ?\nCette action est irreversible.");
        confirm.initModality(Modality.APPLICATION_MODAL);
        try {
            confirm.getDialogPane().getStylesheets().add(
                    getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
        } catch (Exception ignored) {}

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                dao.delete(a.getId());
                loadAdmissions();
                ToastNotification.show(root, "Admission supprimee avec succes.", ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            } catch (Exception e) {
                ToastNotification.show(root, "Erreur lors de la suppression: " + e.getMessage(), ToastNotification.ToastType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void openAdmissionForm(Admission admission) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/AdmissionForm.fxml"));
            Parent content = loader.load();
            AdmissionFormController ctrl = loader.getController();
            if (admission != null) ctrl.setAdmission(admission);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(admission == null ? "Nouvelle Admission" : "Modifier Admission");
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

            if (ctrl.isSaved()) {
                String msg = admission == null
                    ? "Admission enregistrée avec succès."
                    : "Admission modifiée avec succès.";
                loadAdmissions();
                ToastNotification.show(root, msg, ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            }
        } catch (Exception e) {
            ToastNotification.show(root, "Erreur: " + e.getMessage(), ToastNotification.ToastType.ERROR);
            e.printStackTrace();
        }
    }
}

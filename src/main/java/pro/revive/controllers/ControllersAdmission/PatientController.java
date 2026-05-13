package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.PatientDAO;
import pro.revive.entities.EntitiesAdmission.Patient;
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
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PatientController implements Initializable {

    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, Integer> colId;
    @FXML private TableColumn<Patient, String> colNom;
    @FXML private TableColumn<Patient, String> colPrenom;
    @FXML private TableColumn<Patient, String> colDN;
    @FXML private TableColumn<Patient, String> colSexe;
    @FXML private TableColumn<Patient, String> colGS;
    @FXML private TableColumn<Patient, String> colTel;
    @FXML private TableColumn<Patient, String> colCin;
    @FXML private TableColumn<Patient, Integer> colAdmissions;
    @FXML private TableColumn<Patient, Void> colActions;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;
    @FXML private VBox root;

    private final PatientDAO dao = new PatientDAO();
    private ObservableList<Patient> allPatients = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupColumns();
        loadPatients();
    }

    private void setupColumns() {
        // colId masquée en UI - ne pas afficher les IDs aux utilisateurs
        // colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colPrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colSexe.setCellValueFactory(new PropertyValueFactory<>("sexe"));
        colGS.setCellValueFactory(new PropertyValueFactory<>("groupeSanguin"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colCin.setCellValueFactory(new PropertyValueFactory<>("numCin"));

        colDN.setCellValueFactory(data -> {
            Patient p = data.getValue();
            if (p.getDateNaissance() != null) {
                return new javafx.beans.property.SimpleStringProperty(p.getDateNaissance().format(FMT));
            }
            return new javafx.beans.property.SimpleStringProperty("—");
        });

        colAdmissions.setCellValueFactory(data -> {
            try {
                int count = dao.countAdmissions(data.getValue().getId());
                return new javafx.beans.property.SimpleObjectProperty<>(count);
            } catch (Exception e) {
                return new javafx.beans.property.SimpleObjectProperty<>(0);
            }
        });

        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            private final Button admBtn = new Button("Admission");
            private final HBox box = new HBox(6, editBtn, delBtn, admBtn);

            {
                box.setAlignment(Pos.CENTER_LEFT);
                editBtn.getStyleClass().addAll("btn-table-action");
                delBtn.getStyleClass().addAll("btn-table-action-danger");
                admBtn.getStyleClass().addAll("btn-table-action-info");

                editBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    handleEdit(p);
                });
                delBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    handleDelete(p);
                });
                admBtn.setOnAction(e -> {
                    Patient p = getTableView().getItems().get(getIndex());
                    handleNewAdmission(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private void loadPatients() {
        try {
            List<Patient> patients = dao.findAll();
            allPatients = FXCollections.observableArrayList(patients);
            patientTable.setItems(allPatients);
            statusLabel.setText("Patients chargés");
            countLabel.setText(patients.size() + " patients");
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            patientTable.setItems(allPatients);
            countLabel.setText(allPatients.size() + " patients");
            return;
        }
        try {
            List<Patient> results = dao.search(query);
            patientTable.setItems(FXCollections.observableArrayList(results));
            countLabel.setText(results.size() + " résultats");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        patientTable.setItems(allPatients);
        countLabel.setText(allPatients.size() + " patients");
    }

    @FXML
    private void handleAdd() {
        openPatientForm(null);
    }

    private void handleEdit(Patient p) {
        openPatientForm(p);
    }

    private void handleDelete(Patient p) {
        // Inline confirmation - no extra window needed
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer le patient ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer " + p.getNomComplet() + " ?\nCette action est irréversible.");
        confirm.initModality(Modality.APPLICATION_MODAL);
        // Style the dialog
        confirm.getDialogPane().getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                dao.delete(p.getId());
                loadPatients();
                showToast("Patient " + p.getNomComplet() + " supprimé avec succès.", ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            } catch (Exception e) {
                showToast("Erreur lors de la suppression: " + e.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }

    private void handleNewAdmission(Patient p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/AdmissionForm.fxml"));
            Parent content = loader.load();
            AdmissionFormController ctrl = loader.getController();
            ctrl.setPreSelectedPatient(p);
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Nouvelle Admission — " + p.getNomComplet());
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            loadPatients();
            if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void openPatientForm(Patient patient) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/PatientForm.fxml"));
            Parent content = loader.load();
            PatientFormController ctrl = loader.getController();
            if (patient != null) ctrl.setPatient(patient);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(patient == null ? "Nouveau Patient" : "Modifier Patient");

            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

            // Show toast result
            if (ctrl.isSaved()) {
                String msg = patient == null
                    ? "Patient " + ctrl.getSavedPatientName() + " ajouté avec succès."
                    : "Patient " + ctrl.getSavedPatientName() + " modifié avec succès.";
                loadPatients();
                showToast(msg, ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            }
        } catch (Exception e) {
            showToast("Erreur: " + e.getMessage(), ToastNotification.ToastType.ERROR);
            e.printStackTrace();
        }
    }

    private void showToast(String message, ToastNotification.ToastType type) {
        if (root != null) {
            ToastNotification.show(root, message, type);
        }
    }
}

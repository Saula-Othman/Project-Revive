package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.PatientDAO;
import pro.revive.entities.EntitiesAdmission.Patient;
import pro.revive.utils.UtilesAdmission.ToastNotification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PatientCardsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private VBox cardsContainer;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    private final PatientDAO dao = new PatientDAO();
    private ObservableList<Patient> allPatients = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        loadPatients();
    }

    private void loadPatients() {
        try {
            List<Patient> patients = dao.findAll();
            allPatients = FXCollections.observableArrayList(patients);
            displayPatients(allPatients);
            statusLabel.setText("Patients chargés");
            countLabel.setText(patients.size() + " patients");
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayPatients(List<Patient> patients) {
        cardsContainer.getChildren().clear();
        
        if (patients.isEmpty()) {
            Label emptyLabel = new Label("Aucun patient trouvé");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 40px;");
            cardsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Patient p : patients) {
            cardsContainer.getChildren().add(createPatientCard(p));
        }
    }

    private HBox createPatientCard(Patient p) {
        HBox card = new HBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("patient-card");
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-width: 1px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        // Avatar
        Label avatar = new Label(p.getPrenom().substring(0, 1).toUpperCase() + p.getNom().substring(0, 1).toUpperCase());
        avatar.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; " +
                       "-fx-font-size: 16px; -fx-font-weight: bold; " +
                       "-fx-min-width: 50px; -fx-min-height: 50px; -fx-max-width: 50px; -fx-max-height: 50px; " +
                       "-fx-background-radius: 25px; -fx-alignment: center;");

        // Info section
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label nameLabel = new Label(p.getNomComplet());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        HBox detailsRow = new HBox(20);
        detailsRow.setAlignment(Pos.CENTER_LEFT);

        Label dnLabel = new Label("📅 " + (p.getDateNaissance() != null ? p.getDateNaissance().format(FMT) : "—"));
        dnLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label sexeLabel = new Label((p.getSexe().equals("M") ? "👨" : "👩") + " " + p.getSexe());
        sexeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label gsLabel = new Label("🩸 " + (p.getGroupeSanguin() != null ? p.getGroupeSanguin() : "Inconnu"));
        gsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label telLabel = new Label("📞 " + (p.getTelephone() != null ? p.getTelephone() : "—"));
        telLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        Label cinLabel = new Label("🆔 " + (p.getNumCin() != null ? p.getNumCin() : "—"));
        cinLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        detailsRow.getChildren().addAll(dnLabel, sexeLabel, gsLabel, telLabel, cinLabel);

        // Admissions count
        try {
            int admCount = dao.countAdmissions(p.getId());
            Label admLabel = new Label("📋 " + admCount + " admission(s)");
            admLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            detailsRow.getChildren().add(admLabel);
        } catch (Exception e) {
            e.printStackTrace();
        }

        infoBox.getChildren().addAll(nameLabel, detailsRow);

        // Actions
        VBox actionsBox = new VBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);

        Button editBtn = new Button("✏ Modifier");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setPrefWidth(120);
        editBtn.setOnAction(e -> handleEdit(p));

        Button admBtn = new Button("➕ Admission");
        admBtn.getStyleClass().add("btn-primary");
        admBtn.setPrefWidth(120);
        admBtn.setOnAction(e -> handleNewAdmission(p));

        Button delBtn = new Button("🗑 Supprimer");
        delBtn.getStyleClass().add("btn-ghost");
        delBtn.setStyle("-fx-text-fill: #dc2626;");
        delBtn.setPrefWidth(120);
        delBtn.setOnAction(e -> handleDelete(p));

        actionsBox.getChildren().addAll(admBtn, editBtn, delBtn);

        card.getChildren().addAll(avatar, infoBox, actionsBox);
        return card;
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            displayPatients(allPatients);
            countLabel.setText(allPatients.size() + " patients");
            return;
        }
        try {
            List<Patient> results = dao.search(query);
            displayPatients(results);
            countLabel.setText(results.size() + " résultats");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearSearch() {
        searchField.clear();
        displayPatients(allPatients);
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
        try {
            List<Integer> admissionIds = dao.findAdmissionIds(p.getId());
            String admissionsText = admissionIds.isEmpty()
                    ? "Aucune admission associee."
                    : admissionIds.size() + " admission(s) seront supprimees.";

            Alert confirm = new Alert(Alert.AlertType.WARNING);
            confirm.setTitle("Confirmer la suppression");
            confirm.setHeaderText("Supprimer le patient et ses admissions ?");
            confirm.setContentText("Patient: " + p.getNomComplet() + "\n\n"
                    + admissionsText + "\n\nCette action est irreversible.");
            confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
            confirm.initModality(Modality.APPLICATION_MODAL);
            confirm.getDialogPane().getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                dao.delete(p.getId());
                loadPatients();
                showToast("Patient supprime avec " + admissionIds.size() + " admission(s).", ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            }
        } catch (Exception e) {
            showToast("Erreur lors de la suppression: " + e.getMessage(), ToastNotification.ToastType.ERROR);
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

    private void openPatientForm(Patient p) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/PatientForm.fxml"));
            Parent content = loader.load();
            if (p != null) {
                PatientFormController ctrl = loader.getController();
                ctrl.setPatient(p);
            }
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(p == null ? "Nouveau Patient" : "Modifier Patient");
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

    private void showToast(String message, ToastNotification.ToastType type) {
        try {
            // Get the root pane from the scene
            if (cardsContainer.getScene() != null && cardsContainer.getScene().getRoot() instanceof javafx.scene.layout.Pane) {
                ToastNotification.show((javafx.scene.layout.Pane) cardsContainer.getScene().getRoot(), message, type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

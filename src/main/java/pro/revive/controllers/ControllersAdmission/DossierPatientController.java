package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.HistoriqueDAO;
import pro.revive.entities.EntitiesAdmission.HistoriquePatient;
import pro.revive.entities.EntitiesAdmission.Patient;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class DossierPatientController {

    @FXML private Label dialogTitle;
    @FXML private Label avatarLabel;
    @FXML private Label nomLabel;
    @FXML private Label dnLabel;
    @FXML private Label sexeLabel;
    @FXML private Label gsLabel;
    @FXML private Label telLabel;
    @FXML private Label allergiesLabel;
    @FXML private Label antecedentsLabel;
    @FXML private Label countLabel;
    @FXML private VBox historiqueContainer;

    private final HistoriqueDAO historiqueDAO = new HistoriqueDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private Patient patient;
    private Integer admissionId;

    public void setPatient(Patient p, Integer admId) {
        this.patient = p;
        this.admissionId = admId;
        displayPatientInfo();
        loadHistorique();
    }

    private void displayPatientInfo() {
        dialogTitle.setText("Dossier Patient — " + patient.getNomComplet());
        avatarLabel.setText(patient.getPrenom().substring(0, 1).toUpperCase() + 
                           patient.getNom().substring(0, 1).toUpperCase());
        nomLabel.setText(patient.getNomComplet());
        dnLabel.setText("📅 " + (patient.getDateNaissance() != null ? patient.getDateNaissance().format(FMT) : "—"));
        sexeLabel.setText((patient.getSexe().equals("M") ? "👨" : "👩") + " " + patient.getSexe());
        gsLabel.setText("🩸 " + (patient.getGroupeSanguin() != null ? patient.getGroupeSanguin() : "Inconnu"));
        telLabel.setText("📞 " + (patient.getTelephone() != null ? patient.getTelephone() : "—"));
        allergiesLabel.setText(patient.getAllergies() != null && !patient.getAllergies().isEmpty() 
            ? patient.getAllergies() : "Aucune connue");
        antecedentsLabel.setText(patient.getAntecedents() != null && !patient.getAntecedents().isEmpty() 
            ? patient.getAntecedents() : "Aucun");
    }

    private void loadHistorique() {
        try {
            // La VIEW v_historique_patient_complet fusionne automatiquement :
            // historique_patient (LOCAL/MODULE_3/MODULE_4) + consultations + ordonnances + examens
            // Les triggers MySQL assurent la copie automatique depuis les autres modules.
            List<HistoriquePatient> historique = historiqueDAO.findAllByPatient(patient.getId());

            countLabel.setText(historique.size() + " document(s)");

            if (historique.isEmpty()) {
                Label emptyLabel = new Label("Aucun historique médical disponible pour ce patient.");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 40px;");
                historiqueContainer.getChildren().add(emptyLabel);
                return;
            }

            for (HistoriquePatient h : historique) {
                historiqueContainer.getChildren().add(createHistoriqueCard(h));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("Erreur lors du chargement de l'historique: " + e.getMessage());
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #dc2626; -fx-padding: 20px;");
            historiqueContainer.getChildren().add(errorLabel);
        }
    }

    private VBox createHistoriqueCard(HistoriquePatient h) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-width: 1px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");

        // Header row
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = createTypeBadge(h.getTypeDocument());
        
        Label titreLabel = new Label(h.getTitre());
        titreLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(titreLabel, Priority.ALWAYS);

        Label dateLabel = new Label(h.getDateConsultation() != null ? h.getDateConsultation().format(FMT) : "—");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        headerRow.getChildren().addAll(typeLabel, titreLabel, dateLabel);

        // Content
        Label contenuLabel = new Label(h.getContenu());
        contenuLabel.setWrapText(true);
        contenuLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569; -fx-line-spacing: 2px;");

        // Footer row
        HBox footerRow = new HBox(16);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        if (h.getMedecinNom() != null && !h.getMedecinNom().isEmpty()) {
            Label medecinLabel = new Label("👨‍⚕️ " + h.getMedecinNom());
            medecinLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            footerRow.getChildren().add(medecinLabel);
        }

        if (h.getEtablissement() != null && !h.getEtablissement().isEmpty()) {
            Label etablissementLabel = new Label("🏥 " + h.getEtablissement());
            etablissementLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            footerRow.getChildren().add(etablissementLabel);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        footerRow.getChildren().add(spacer);

        Label sourceLabel = createSourceBadge(h.getSource());
        footerRow.getChildren().add(sourceLabel);

        card.getChildren().addAll(headerRow, contenuLabel, footerRow);
        return card;
    }

    private Label createTypeBadge(String type) {
        Label badge = new Label(type);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle("-fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        switch (type) {
            case "Consultation":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;");
                break;
            case "Ordonnance":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #fce7f3; -fx-text-fill: #9f1239;");
                break;
            case "Examen":
            case "Résultat Examen":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca;");
                break;
            case "Hospitalisation":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412;");
                break;
            case "Compte-rendu":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
                break;
            default:
                badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
        }
        return badge;
    }

    private Label createSourceBadge(String source) {
        Label badge = new Label();
        badge.setPadding(new Insets(2, 8, 2, 8));
        badge.setStyle("-fx-background-radius: 10px; -fx-font-size: 9px; -fx-font-weight: bold; " +
                      "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;");
        
        if (source != null) {
            switch (source) {
                case "MDIA_API":
                    badge.setText("MDIA National");
                    badge.setStyle(badge.getStyle() + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;");
                    break;
                case "MODULE_1_ADMISSION":
                    badge.setText("Module Admission");
                    break;
                case "MODULE_2_CONSULTATION":
                    badge.setText("Module Consultation");
                    break;
                case "MODULE_3_EXAMENS":
                    badge.setText("Module Examens");
                    break;
                default:
                    badge.setText(source);
            }
        } else {
            badge.setText("Source inconnue");
        }
        return badge;
    }

    @FXML
    private void handleAjouterDocument() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/DocumentForm.fxml"));
            Parent content = loader.load();
            DocumentFormController ctrl = loader.getController();
            ctrl.setPatientId(patient.getId(), admissionId);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Ajouter un Document — " + patient.getNomComplet());
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();

            // Recharger le dossier si un document a été ajouté
            if (ctrl.isSaved()) {
                historiqueContainer.getChildren().clear();
                loadHistorique();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) dialogTitle.getScene().getWindow()).close();
    }
}

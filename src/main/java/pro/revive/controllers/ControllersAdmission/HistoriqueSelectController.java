package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.HistoriqueDAO;
import pro.revive.entities.EntitiesAdmission.HistoriquePatient;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class HistoriqueSelectController implements Initializable {

    @FXML private Label headerTitle;
    @FXML private VBox documentsContainer;
    @FXML private Label selectionCountLabel;
    @FXML private Button importBtn;
    @FXML private Label loadingLabel;
    @FXML private Label emptyLabel;

    private final HistoriqueDAO historiqueDAO = new HistoriqueDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private int patientId;
    private String patientNom = "";
    private String importedText = null;
    private boolean hasImported = false;

    // List of CheckBox + HistoriquePatient pairs
    private final List<CheckBox> checkBoxes = new ArrayList<>();
    private final List<HistoriquePatient> documents = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        importBtn.setDisable(true);
        selectionCountLabel.setText("0 document(s) sélectionné(s)");
    }

    public void setPatientId(int patientId, String patientNom) {
        this.patientId = patientId;
        this.patientNom = patientNom != null ? patientNom : "";
        headerTitle.setText("Historique de " + this.patientNom);
        loadDocuments();
    }

    private void loadDocuments() {
        documentsContainer.getChildren().clear();
        checkBoxes.clear();
        documents.clear();

        if (loadingLabel != null) {
            loadingLabel.setVisible(true);
            loadingLabel.setManaged(true);
        }

        try {
            List<HistoriquePatient> historique = historiqueDAO.findAllByPatient(patientId);

            if (loadingLabel != null) {
                loadingLabel.setVisible(false);
                loadingLabel.setManaged(false);
            }

            if (historique.isEmpty()) {
                if (emptyLabel != null) {
                    emptyLabel.setVisible(true);
                    emptyLabel.setManaged(true);
                }
                importBtn.setDisable(true);
                return;
            }

            if (emptyLabel != null) {
                emptyLabel.setVisible(false);
                emptyLabel.setManaged(false);
            }

            for (HistoriquePatient h : historique) {
                documents.add(h);
                VBox card = createSelectableCard(h, checkBoxes.size());
                documentsContainer.getChildren().add(card);
            }

        } catch (Exception e) {
            if (loadingLabel != null) {
                loadingLabel.setText("Erreur lors du chargement : " + e.getMessage());
                loadingLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 13px;");
                loadingLabel.setVisible(true);
                loadingLabel.setManaged(true);
            }
            e.printStackTrace();
        }
    }

    private VBox createSelectableCard(HistoriquePatient h, int index) {
        CheckBox cb = new CheckBox();
        cb.setStyle("-fx-cursor: hand;");
        checkBoxes.add(cb);

        cb.selectedProperty().addListener((obs, oldVal, newVal) -> updateSelectionCount());

        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                "-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-width: 1.5px; " +
                "-fx-cursor: hand;");

        // Click on card toggles checkbox
        card.setOnMouseClicked(e -> cb.setSelected(!cb.isSelected()));
        cb.selectedProperty().addListener((obs, old, selected) -> {
            if (selected) {
                card.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 8px; " +
                        "-fx-border-color: #3b82f6; -fx-border-radius: 8px; -fx-border-width: 2px; " +
                        "-fx-cursor: hand;");
            } else {
                card.setStyle("-fx-background-color: white; -fx-background-radius: 8px; " +
                        "-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-width: 1.5px; " +
                        "-fx-cursor: hand;");
            }
        });

        // Header row: checkbox + type badge + title + date
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = createTypeBadge(h.getTypeDocument());

        Label titreLabel = new Label(h.getTitre() != null ? h.getTitre() : "(Sans titre)");
        titreLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        HBox.setHgrow(titreLabel, Priority.ALWAYS);

        Label dateLabel = new Label(h.getDateConsultation() != null ? h.getDateConsultation().format(FMT) : "—");
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

        headerRow.getChildren().addAll(cb, typeBadge, titreLabel, dateLabel);

        // Content preview (max 120 chars)
        String contenu = h.getContenu() != null ? h.getContenu() : "";
        String preview = contenu.length() > 120 ? contenu.substring(0, 120) + "..." : contenu;
        Label contenuLabel = new Label(preview);
        contenuLabel.setWrapText(true);
        contenuLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        contenuLabel.setPadding(new Insets(0, 0, 0, 30));

        // Footer: medecin + etablissement
        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(0, 0, 0, 30));

        if (h.getMedecinNom() != null && !h.getMedecinNom().isEmpty()) {
            Label medLabel = new Label("👨‍⚕️ " + h.getMedecinNom());
            medLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            footerRow.getChildren().add(medLabel);
        }
        if (h.getEtablissement() != null && !h.getEtablissement().isEmpty()) {
            Label etabLabel = new Label("🏥 " + h.getEtablissement());
            etabLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");
            footerRow.getChildren().add(etabLabel);
        }

        card.getChildren().addAll(headerRow, contenuLabel);
        if (!footerRow.getChildren().isEmpty()) {
            card.getChildren().add(footerRow);
        }

        return card;
    }

    private Label createTypeBadge(String type) {
        Label badge = new Label(type != null ? type : "—");
        badge.setPadding(new Insets(3, 8, 3, 8));
        badge.setStyle("-fx-background-radius: 10px; -fx-font-size: 10px; -fx-font-weight: bold;");
        if (type == null) {
            badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
            return badge;
        }
        switch (type) {
            case "Consultation":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;"); break;
            case "Ordonnance":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #fce7f3; -fx-text-fill: #9f1239;"); break;
            case "Résultat Examen":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca;"); break;
            case "Hospitalisation":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412;"); break;
            case "Compte-rendu":
                badge.setStyle(badge.getStyle() + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;"); break;
            default:
                badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
        }
        return badge;
    }

    private void updateSelectionCount() {
        long count = checkBoxes.stream().filter(CheckBox::isSelected).count();
        selectionCountLabel.setText(count + " document(s) sélectionné(s)");
        importBtn.setDisable(count == 0);
    }

    @FXML
    private void handleSelectAll() {
        checkBoxes.forEach(cb -> cb.setSelected(true));
    }

    @FXML
    private void handleDeselectAll() {
        checkBoxes.forEach(cb -> cb.setSelected(false));
    }

    @FXML
    private void handleImporter() {
        StringBuilder sb = new StringBuilder();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                HistoriquePatient h = documents.get(i);
                sb.append("=== ")
                  .append(h.getTypeDocument() != null ? h.getTypeDocument() : "Document")
                  .append(" — ")
                  .append(h.getTitre() != null ? h.getTitre() : "")
                  .append(" (")
                  .append(h.getDateConsultation() != null ? h.getDateConsultation().format(fmt) : "—")
                  .append(") ===\n");
                if (h.getMedecinNom() != null && !h.getMedecinNom().isEmpty()) {
                    sb.append("Médecin : ").append(h.getMedecinNom()).append("\n");
                }
                if (h.getEtablissement() != null && !h.getEtablissement().isEmpty()) {
                    sb.append("Établissement : ").append(h.getEtablissement()).append("\n");
                }
                if (h.getContenu() != null && !h.getContenu().isEmpty()) {
                    sb.append(h.getContenu()).append("\n");
                }
                sb.append("\n");
            }
        }

        this.importedText = sb.toString().trim();
        this.hasImported = true;
        ((Stage) importBtn.getScene().getWindow()).close();
    }

    @FXML
    private void handleAnnuler() {
        this.hasImported = false;
        ((Stage) importBtn.getScene().getWindow()).close();
    }

    public String getImportedText() { return importedText; }
    public boolean hasImported() { return hasImported; }
}

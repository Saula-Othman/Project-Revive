package pro.revive.controllers.ControllersAdmission;

import pro.revive.daoAdmission.AdmissionDAO;
import pro.revive.daoAdmission.PatientDAO;
import pro.revive.entities.EntitiesAdmission.Admission;
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
import java.util.stream.Collectors;

public class AdmissionCardsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private VBox cardsContainer;
    @FXML private Label statusLabel;
    @FXML private Label countLabel;

    private final AdmissionDAO admissionDAO = new AdmissionDAO();
    private final PatientDAO patientDAO = new PatientDAO();
    private ObservableList<Admission> allAdmissions = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        filterCombo.setItems(FXCollections.observableArrayList(
                "Toutes", "En attente triage", "En triage", "En consultation",
                "Hospitalisé", "Sorti", "Transféré"
        ));
        filterCombo.setValue("Toutes");
        loadAdmissions();
    }

    private void loadAdmissions() {
        try {
            List<Admission> admissions = admissionDAO.findAll();
            for (Admission a : admissions) {
                try {
                    Patient p = patientDAO.findById(a.getPatientId());
                    if (p != null) a.setPatient(p);
                } catch (Exception e) { e.printStackTrace(); }
            }
            allAdmissions = FXCollections.observableArrayList(admissions);
            displayAdmissions(allAdmissions);
            statusLabel.setText("Admissions chargées");
            countLabel.setText(admissions.size() + " admissions");
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayAdmissions(List<Admission> admissions) {
        cardsContainer.getChildren().clear();
        if (admissions.isEmpty()) {
            Label emptyLabel = new Label("Aucune admission trouvée");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #94a3b8; -fx-padding: 40px;");
            cardsContainer.getChildren().add(emptyLabel);
            return;
        }
        for (Admission a : admissions) {
            cardsContainer.getChildren().add(createAdmissionCard(a));
        }
    }

    private VBox createAdmissionCard(Admission a) {
        VBox card = new VBox(12);
        card.getStyleClass().add("admission-card");
        card.setPadding(new Insets(16, 20, 16, 20));

        // Griser la carte si désactivée
        String cardBg = a.isActif() ? "white" : "#f8fafc";
        String cardOpacity = a.isActif() ? "1.0" : "0.7";
        card.setStyle("-fx-background-color: " + cardBg + "; -fx-background-radius: 8px; " +
                "-fx-border-color: " + (a.isActif() ? "#e2e8f0" : "#cbd5e1") + "; " +
                "-fx-border-radius: 8px; -fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);" +
                "-fx-opacity: " + cardOpacity + ";");

        // Header — statut + priorité + badge désactivé + date
        HBox headerRow = new HBox(10);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label statutBadge = createStatutBadge(a.getStatut());
        Label prioriteBadge = createPrioriteBadge(a.getPrioriteInitiale());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label("📅 " + a.getDateAdmission().format(FMT));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        headerRow.getChildren().addAll(statutBadge, prioriteBadge);

        // Badge "Désactivée" si actif=false
        if (!a.isActif()) {
            Label desactiveBadge = new Label("🔴 Désactivée");
            desactiveBadge.setPadding(new Insets(4, 10, 4, 10));
            desactiveBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; " +
                    "-fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
            headerRow.getChildren().add(desactiveBadge);
        }

        // Badge ambulance urgente
        if (a.isPatientInconnu() && a.getAmbulanceId() != null) {
            Label ambBadge = new Label("🚑 Ambulance");
            ambBadge.setPadding(new Insets(4, 10, 4, 10));
            ambBadge.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; " +
                    "-fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
            headerRow.getChildren().add(ambBadge);
        }

        headerRow.getChildren().addAll(spacer, dateLabel);

        // Patient info row
        HBox patientRow = new HBox(16);
        patientRow.setAlignment(Pos.CENTER_LEFT);

        if (a.getPatient() != null) {
            Label patientLabel = new Label("👤 " + a.getPatient().getNomComplet());
            patientLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
            Label sexeLabel = new Label((a.getPatient().getSexe() != null && a.getPatient().getSexe().equals("M") ? "👨" : "👩")
                    + " " + (a.getPatient().getSexe() != null ? a.getPatient().getSexe() : "—"));
            sexeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            Label gsLabel = new Label("🩸 " + (a.getPatient().getGroupeSanguin() != null ? a.getPatient().getGroupeSanguin() : "Inconnu"));
            gsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
            patientRow.getChildren().addAll(patientLabel, sexeLabel, gsLabel);
        } else {
            Label unknownLabel = new Label("👤 Patient inconnu");
            unknownLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            patientRow.getChildren().add(unknownLabel);
        }

        // Details
        VBox detailsBox = new VBox(4);
        Label modeLabel = new Label("🚑 Mode: " + a.getModeArrivee());
        modeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        Label motifLabel = new Label("📋 " + a.getMotifAdmission());
        motifLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        motifLabel.setWrapText(true);
        detailsBox.getChildren().addAll(modeLabel, motifLabel);
        if (a.getNotes() != null && !a.getNotes().isEmpty()) {
            Label notesLabel = new Label("📝 " + a.getNotes());
            notesLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            notesLabel.setWrapText(true);
            detailsBox.getChildren().add(notesLabel);
        }

        // Boutons d'action
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_RIGHT);

        Button viewBtn = new Button("👁 Dossier");
        viewBtn.getStyleClass().add("btn-secondary");
        viewBtn.setOnAction(e -> handleViewDossier(a));

        Button editBtn = new Button("✏ Modifier");
        editBtn.getStyleClass().add("btn-ghost");
        editBtn.setOnAction(e -> handleEdit(a));
        editBtn.setDisable(!a.isActif()); // On ne peut pas modifier une admission désactivée

        // Bouton Désactiver / Activer
        // Règle : désactivation bloquée si dossier patient ou admission incomplet
        String raisonBlocage = getDesactivationBlocageRaison(a);

        if (a.isActif()) {
            Button desactiverBtn = new Button("🔴 Désactiver");
            desactiverBtn.getStyleClass().add("btn-ghost");
            desactiverBtn.setStyle("-fx-text-fill: #b45309;");
            if (raisonBlocage != null) {
                desactiverBtn.setDisable(true);
                desactiverBtn.setTooltip(new Tooltip(raisonBlocage));
            } else {
                desactiverBtn.setOnAction(e -> handleDesactiver(a));
            }
            actionsRow.getChildren().add(desactiverBtn);
        } else {
            Button activerBtn = new Button("🟢 Réactiver");
            activerBtn.getStyleClass().add("btn-ghost");
            activerBtn.setStyle("-fx-text-fill: #16a34a;");
            activerBtn.setOnAction(e -> handleActiver(a));
            actionsRow.getChildren().add(activerBtn);
        }

        Button delBtn = new Button("🗑 Supprimer");
        delBtn.getStyleClass().add("btn-ghost");
        delBtn.setStyle("-fx-text-fill: #dc2626;");
        delBtn.setOnAction(e -> handleDelete(a));

        actionsRow.getChildren().addAll(viewBtn, editBtn, delBtn);

        card.getChildren().addAll(headerRow, patientRow, detailsBox, actionsRow);
        return card;
    }

    private void handleDesactiver(Admission a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Désactiver l'admission");
        confirm.setHeaderText("Désactiver cette admission ?");
        confirm.setContentText("Le patient est sorti. L'admission sera désactivée.\nVous pourrez la réactiver si nécessaire.");
        confirm.initModality(Modality.APPLICATION_MODAL);
        try { confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm()); } catch (Exception ignored) {}
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                admissionDAO.updateActif(a.getId(), false);
                admissionDAO.updateStatut(a.getId(), "Sorti");
                loadAdmissions();
                showToast("Admission désactivée — patient sorti.", ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            } catch (Exception e) {
                showToast("Erreur: " + e.getMessage(), ToastNotification.ToastType.ERROR);
            }
        }
    }

    private void handleActiver(Admission a) {
        try {
            admissionDAO.updateActif(a.getId(), true);
            admissionDAO.updateStatut(a.getId(), "En attente triage");
            loadAdmissions();
            showToast("Admission réactivée.", ToastNotification.ToastType.SUCCESS);
            if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
        } catch (Exception e) {
            showToast("Erreur: " + e.getMessage(), ToastNotification.ToastType.ERROR);
        }
    }

    private void handleDelete(Admission a) {
        String patientNom = a.getPatient() != null ? a.getPatient().getNomComplet() : "patient inconnu";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer cette admission ?");
        confirm.setContentText("Supprimer l'admission du " + a.getDateAdmission().format(FMT)
                + " pour " + patientNom + " ?\nCette action est irréversible.");
        confirm.initModality(Modality.APPLICATION_MODAL);
        try {
            confirm.getDialogPane().getStylesheets().add(
                    getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
        } catch (Exception ignored) {}

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                admissionDAO.delete(a.getId());
                loadAdmissions();
                showToast("Admission supprimée avec succès.", ToastNotification.ToastType.SUCCESS);
                if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
            } catch (Exception e) {
                showToast("Erreur: " + e.getMessage(), ToastNotification.ToastType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private Label createStatutBadge(String statut) {
        Label badge = new Label(statut);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle("-fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        switch (statut != null ? statut : "") {
            case "En attente triage": badge.setStyle(badge.getStyle() + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;"); break;
            case "En triage":         badge.setStyle(badge.getStyle() + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;"); break;
            case "En consultation":   badge.setStyle(badge.getStyle() + "-fx-background-color: #e0e7ff; -fx-text-fill: #4338ca;"); break;
            case "Hospitalisé":       badge.setStyle(badge.getStyle() + "-fx-background-color: #fce7f3; -fx-text-fill: #9f1239;"); break;
            case "Sorti":             badge.setStyle(badge.getStyle() + "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;"); break;
            case "Transféré":         badge.setStyle(badge.getStyle() + "-fx-background-color: #f3e8ff; -fx-text-fill: #6b21a8;"); break;
            default:                  badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
        }
        return badge;
    }

    private Label createPrioriteBadge(String priorite) {
        Label badge = new Label(priorite);
        badge.setPadding(new Insets(4, 10, 4, 10));
        badge.setStyle("-fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        switch (priorite != null ? priorite : "") {
            case "Critique":    badge.setStyle(badge.getStyle() + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;"); break;
            case "Urgent":      badge.setStyle(badge.getStyle() + "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412;"); break;
            case "Modéré":      badge.setStyle(badge.getStyle() + "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;"); break;
            case "Peu urgent":  badge.setStyle(badge.getStyle() + "-fx-background-color: #dbeafe; -fx-text-fill: #1e40af;"); break;
            default:            badge.setStyle(badge.getStyle() + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
        }
        return badge;
    }

    @FXML private void handleFilter() {
        String filter = filterCombo.getValue();
        List<Admission> filtered = (filter == null || filter.equals("Toutes"))
                ? allAdmissions
                : allAdmissions.stream().filter(a -> filter.equals(a.getStatut())).collect(Collectors.toList());
        displayAdmissions(filtered);
        countLabel.setText(filtered.size() + " admissions");
    }

    @FXML private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) { handleFilter(); return; }
        List<Admission> filtered = allAdmissions.stream().filter(a -> {
            if (a.getPatient() != null) {
                return a.getPatient().getNomComplet().toLowerCase().contains(query)
                        || (a.getPatient().getNumCin() != null && a.getPatient().getNumCin().contains(query))
                        || (a.getMotifAdmission() != null && a.getMotifAdmission().toLowerCase().contains(query));
            }
            return false;
        }).collect(Collectors.toList());
        displayAdmissions(filtered);
        countLabel.setText(filtered.size() + " résultats");
    }

    @FXML private void handleClearSearch() {
        searchField.clear();
        filterCombo.setValue("Toutes");
        displayAdmissions(allAdmissions);
        countLabel.setText(allAdmissions.size() + " admissions");
    }

    @FXML private void handleAdd() { openAdmissionForm(null); }

    private void handleEdit(Admission a) { openAdmissionForm(a); }

    private void handleViewDossier(Admission a) {
        if (a.getPatient() == null) {
            showToast("Impossible d'afficher le dossier d'un patient inconnu", ToastNotification.ToastType.WARNING);
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/DossierPatient.fxml"));
            Parent content = loader.load();
            DossierPatientController ctrl = loader.getController();
            ctrl.setPatient(a.getPatient(), a.getId());
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Dossier Patient — " + a.getPatient().getNomComplet());
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showToast("Erreur lors de l'ouverture du dossier", ToastNotification.ToastType.ERROR);
        }
    }

    private void openAdmissionForm(Admission a) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourceAdmission/urgence/fxml/AdmissionForm.fxml"));
            Parent content = loader.load();
            if (a != null) {
                AdmissionFormController ctrl = loader.getController();
                ctrl.setAdmission(a);
                // Passer l'ambulance liée si elle existe (affichage informatif)
                if (a.getAmbulanceId() != null) {
                    ctrl.setLinkedAmbulanceId(a.getAmbulanceId());
                }
            }
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(a == null ? "Nouvelle Admission" : "Modifier Admission");
            Scene scene = new Scene(content);
            scene.getStylesheets().add(getClass().getResource("/ResourceAdmission/urgence/css/theme.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
            loadAdmissions();
            if (MainController.getInstance() != null) MainController.getInstance().refreshStatsNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Vérifie si une admission peut être désactivée.
     * Retourne null si tout est OK, ou un message d'explication si bloqué.
     *
     * Règles :
     * 1. Patient ne doit pas être inconnu (nom INCONNU/INCONU/vide)
     * 2. Patient doit avoir : nom réel, prénom réel, date de naissance, téléphone, CIN
     * 3. Admission doit avoir : motif (non vide, >= 5 chars, pas juste "PRÉ-ADMISSION")
     */
    private String getDesactivationBlocageRaison(Admission a) {
        Patient p = a.getPatient();

        // Pas de patient lié
        if (p == null) {
            return "Impossible : aucun patient lié à cette admission.";
        }

        // Nom inconnu ou vide
        String nom = p.getNom() != null ? p.getNom().trim() : "";
        String prenom = p.getPrenom() != null ? p.getPrenom().trim() : "";

        if (nom.isEmpty() || nom.equalsIgnoreCase("INCONNU") || nom.equalsIgnoreCase("INCONU")) {
            return "Impossible : le nom du patient est manquant ou inconnu.\nModifiez le dossier patient avant de desactiver.";
        }
        if (prenom.isEmpty() || prenom.equalsIgnoreCase("Patient") || prenom.equalsIgnoreCase("Inconnu")) {
            return "Impossible : le prenom du patient est manquant ou inconnu.\nModifiez le dossier patient avant de desactiver.";
        }

        // Date de naissance obligatoire
        if (p.getDateNaissance() == null) {
            return "Impossible : la date de naissance du patient est manquante.\nModifiez le dossier patient avant de desactiver.";
        }

        // Téléphone obligatoire et non vide
        String tel = p.getTelephone() != null ? p.getTelephone().trim() : "";
        if (tel.isEmpty()) {
            return "Impossible : le numero de telephone du patient est manquant.\nModifiez le dossier patient avant de desactiver.";
        }

        // CIN obligatoire et non vide
        String cin = p.getNumCin() != null ? p.getNumCin().trim() : "";
        if (cin.isEmpty()) {
            return "Impossible : le numero CIN du patient est manquant.\nModifiez le dossier patient avant de desactiver.";
        }

        // Motif admission obligatoire et complet
        String motif = a.getMotifAdmission() != null ? a.getMotifAdmission().trim() : "";
        if (motif.isEmpty() || motif.length() < 5) {
            return "Impossible : le motif d'admission est trop court ou manquant.\nModifiez l'admission avant de desactiver.";
        }
        // Bloquer si motif est une pré-admission automatique non complétée
        if (motif.toUpperCase().startsWith("PRE-ADMISSION") || motif.toUpperCase().startsWith("PRE-ADMISSION")) {
            return "Impossible : l'admission est une pre-inscription automatique non completee.\nModifiez l'admission avec le vrai motif avant de desactiver.";
        }

        // Tout est OK
        return null;
    }

    private void showToast(String message, ToastNotification.ToastType type) {
        try {
            if (cardsContainer.getScene() != null && cardsContainer.getScene().getRoot() instanceof Pane) {
                ToastNotification.show((Pane) cardsContainer.getScene().getRoot(), message, type);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}

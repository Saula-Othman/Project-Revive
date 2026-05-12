package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.Ordonnance;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.OrdonnanceService;
import pro.revive.utils.UtilesMed.IcdAutoCompleteField;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ModifierConsultationController implements Initializable {

    @FXML private Label                           lblSideId, lblTopSub;
    @FXML private Label                           lblPatientInfo, lblIdInfo, lblDebutInfo;
    @FXML private TextArea                        taDiagnostic;
    @FXML private ComboBox<String>                cmbOrientation;
    // Nouveaux champs — Laboratoire & Imagerie
    @FXML private TextArea                        taAnalyses;
    @FXML private TextArea                        taImageries;
    @FXML private Label                           lblStatutDemande;
    @FXML private TableView<Ordonnance>           tableOrdonnances;
    @FXML private TableColumn<Ordonnance, String> colMed, colPos, colDuree;
    @FXML private TableColumn<Ordonnance, Void>   colSup;
    @FXML private Label                           lblMessage;

    private final ConsultationService cs = new ConsultationService();
    private final OrdonnanceService   os = new OrdonnanceService();
    private final ObservableList<Ordonnance> ordos = FXCollections.observableArrayList();
    private Consultation consultation;
    private IcdAutoCompleteField icdField;  // Feature 1
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbOrientation.setItems(FXCollections.observableArrayList("Sortie", "Hospitalisation", "Transfert"));
        colMed.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicament()));
        colPos.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPosologie()));
        colDuree.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDureeJours() + " j"));
        colSup.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("\uD83D\uDDD1");
            { btn.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626; -fx-background-radius: 6; -fx-padding: 4 10; -fx-cursor: hand;");
              btn.setOnAction(e -> { Ordonnance o = getTableRow().getItem(); if (o != null) { os.deleteEntity(o); ordos.remove(o); }}); }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : btn); }
        });
        tableOrdonnances.setItems(ordos);

        // Feature 1 — ICD-10 autocomplete sur le champ diagnostic
        // Initialise apres que taDiagnostic soit injecte par FXML
        javafx.application.Platform.runLater(() -> {
            if (taDiagnostic != null) {
                icdField = new IcdAutoCompleteField(taDiagnostic, entry -> {
                    if (consultation != null) {
                        consultation.setIcdCode(entry.code());
                    }
                    // Afficher le code selectionne dans le message
                    setMsg("Code CIM-10 selectionne : " + entry.code() + " — " + entry.description(), true);
                });
            }
        });
    }

    /** Appelé depuis AfficherConsultationController */
    public void setConsultation(Consultation c) {
        this.consultation = c;
        lblSideId.setText("Consultation #" + c.getIdConsultation());
        lblTopSub.setText("Modifier Consultation #" + c.getIdConsultation());
        lblPatientInfo.setText(c.getNomPatient() != null ? c.getNomPatient() : "—");
        lblIdInfo.setText("#" + c.getIdConsultation());
        lblDebutInfo.setText(c.getDateHeureDebut() != null ? c.getDateHeureDebut().format(FMT) : "—");
        taDiagnostic.setText(c.getDiagnostic() != null ? c.getDiagnostic() : "");
        if (c.getOrientation() != null) cmbOrientation.setValue(c.getOrientation());
        // Charger analyses et imageries
        if (taAnalyses  != null) taAnalyses.setText(c.getAnalyses()  != null ? c.getAnalyses()  : "");
        if (taImageries != null) taImageries.setText(c.getImageries() != null ? c.getImageries() : "");
        // Afficher le statut actuel
        mettreAJourStatutLabel(c.getStatutDemande());
        ordos.setAll(os.getByConsultation(c.getIdConsultation()));
    }

    @FXML private void handleAjouterOrdo() {
        // Dialogue simple pour ajouter une ordonnance
        Dialog<Ordonnance> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une ordonnance");
        dialog.setHeaderText("Nouvelle prescription");
        ButtonType btnOk = new ButtonType("Ajouter", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnOk, ButtonType.CANCEL);

        TextField tfMed = new TextField(); tfMed.setPromptText("Médicament *");
        TextField tfPos = new TextField(); tfPos.setPromptText("Posologie");
        TextField tfDur = new TextField(); tfDur.setPromptText("Durée (jours)");
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, new Label("Médicament :"), tfMed, new Label("Posologie :"), tfPos, new Label("Durée (jours) :"), tfDur);
        content.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(btn -> {
            if (btn == btnOk && !tfMed.getText().isBlank()) {
                int dur = 0; try { dur = Integer.parseInt(tfDur.getText()); } catch (Exception ignored) {}
                return new Ordonnance(consultation.getIdConsultation(), tfMed.getText(), tfPos.getText(), dur);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(o -> { os.addEntity(o); ordos.add(o); });
    }

    @FXML private void handleModifier() {
        if (taDiagnostic.getText().isBlank()) { setMsg("Le diagnostic est obligatoire.", false); return; }

        consultation.setDiagnostic(taDiagnostic.getText());
        consultation.setOrientation(cmbOrientation.getValue());

        // Recuperer analyses et imageries
        String analyses  = (taAnalyses  != null) ? taAnalyses.getText().trim()  : "";
        String imageries = (taImageries != null) ? taImageries.getText().trim() : "";
        consultation.setAnalyses(analyses.isEmpty()  ? null : analyses);
        consultation.setImageries(imageries.isEmpty() ? null : imageries);

        // Calcul automatique du statut (fait dans updateEntity aussi, mais on l'affiche ici)
        consultation.calculerStatutDemande();

        cs.updateEntity(consultation.getIdConsultation(), consultation);

        // Message selon le statut de la demande
        if (consultation.isDemandEnvoyee()) {
            setMsg("Consultation mise a jour. Demande envoyee au laboratoire/imagerie.", true);
        } else {
            setMsg("Consultation mise a jour sans demande d'examen.", true);
        }
        mettreAJourStatutLabel(consultation.getStatutDemande());
    }

    private void mettreAJourStatutLabel(String statut) {
        if (lblStatutDemande == null) return;
        if ("Envoyee".equals(statut)) {
            lblStatutDemande.setText("Statut demande : Envoyee");
            lblStatutDemande.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 12px;"
                + "-fx-background-color: #DCFCE7; -fx-background-radius: 6; -fx-padding: 4 10;");
        } else {
            lblStatutDemande.setText("Statut demande : Non envoyee");
            lblStatutDemande.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;"
                + "-fx-background-color: #F3F4F6; -fx-background-radius: 6; -fx-padding: 4 10;");
        }
    }

    /**
     * Mode "Demande Examen" — met en evidence la section Labo/Imagerie
     * et masque les champs non pertinents.
     */
    public void setModeDemandeExamen(boolean mode) {
        if (!mode) return;
        // Mettre en evidence la section labo avec un message guide
        if (lblTopSub != null)
            lblTopSub.setText("Remplissez les analyses et/ou imageries pour envoyer une demande");
        if (lblMessage != null) {
            lblMessage.setText("Remplissez les champs ci-dessous et cliquez sur Enregistrer.");
            lblMessage.setStyle("-fx-font-size: 12px; -fx-text-fill: #1D4ED8;");
        }
    }

    @FXML private void handleAnnuler()   { naviguer("AfficherConsultation.fxml"); }
    @FXML private void handleNavListe()  { naviguer("AfficherConsultation.fxml"); }
    @FXML private void handleNavDashboard() { naviguer("dashboardMed.fxml"); }

    private void setMsg(String msg, boolean ok) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: " + (ok ? "#16A34A;" : "#DC2626;"));
    }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ResourcesMed/module3/fxml/" + fxml));
            ((Stage) taDiagnostic.getScene().getWindow()).setScene(new Scene(root));
       
        } catch (IOException e) { System.out.println("[Nav] " + e.getMessage()); }
    }
}

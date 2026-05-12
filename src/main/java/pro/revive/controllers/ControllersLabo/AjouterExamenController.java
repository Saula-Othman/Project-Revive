package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.ConsultationNotif;
import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.services.ServicesLabo.Examens_demandesService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.*;

public class AjouterExamenController {

    @FXML private ComboBox<String> cbIdConsultation;
    @FXML private ComboBox<String> cbStatut;
    @FXML private CheckBox         cbUrgent;
    @FXML private Label            lblMessage;
    @FXML private Label            lblErrTypeExamen;

    // Panneau demande médecin
    @FXML private VBox   panneauDemande;
    @FXML private Label  lblTypeDetecte;

    // Section Analyse
    @FXML private VBox      sectionAnalyse;
    @FXML private Label     lblAnalysesInfo;
    @FXML private CheckBox  cbInclureAnalyse;
    @FXML private TextField tfTypeAnalyse;

    // Section Imagerie
    @FXML private VBox      sectionImagerie;
    @FXML private Label     lblImageriesInfo;
    @FXML private CheckBox  cbInclureImagerie;
    @FXML private TextField tfTypeImagerie;

    private final Examens_demandesService service = new Examens_demandesService();

    private final List<Integer>           idsConsultation = new ArrayList<>();
    private final List<ConsultationNotif> notifsList      = new ArrayList<>();

    @FXML
    public void initialize() {
        chargerPatientsEnvoyes();

        cbStatut.getItems().addAll("En attente", "Realise");
        cbStatut.setValue("En attente");

        // Quand le patient change → mettre à jour le panneau
        cbIdConsultation.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> mettreAJourPanneauDemande(newIdx.intValue()));
    }

    // ── Charge uniquement les patients avec statut_demande = 'Envoyée'
    private void chargerPatientsEnvoyes() {
        List<ConsultationNotif> notifs = service.getConsultationsEnvoyees();

        idsConsultation.clear();
        notifsList.clear();
        cbIdConsultation.getItems().clear();

        for (ConsultationNotif n : notifs) {
            idsConsultation.add(n.getIdConsultation());
            notifsList.add(n);
            cbIdConsultation.getItems().add(n.getNomPatient());
        }

        if (cbIdConsultation.getItems().isEmpty()) {
            lblMessage.setStyle("-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: 12px;");
            lblMessage.setText("ℹ  Aucune demande de médecin en attente.");
            masquerPanneauDemande();
        } else {
            cbIdConsultation.getSelectionModel().selectFirst();
            lblMessage.setText("");
        }
    }

    // ── Met à jour le panneau selon la consultation sélectionnée
    private void mettreAJourPanneauDemande(int idx) {
        if (idx < 0 || idx >= notifsList.size()) {
            masquerPanneauDemande();
            return;
        }

        ConsultationNotif notif = notifsList.get(idx);
        String analyses  = notif.getAnalyses();
        String imageries = notif.getImageries();

        boolean aAnalyse  = analyses  != null && !analyses.isBlank();
        boolean aImagerie = imageries != null && !imageries.isBlank();

        panneauDemande.setVisible(true);
        panneauDemande.setManaged(true);

        // ── Badge type détecté
        if (aAnalyse && aImagerie) {
            lblTypeDetecte.setText("🔬🩻  Demande mixte — Analyse + Imagerie");
            lblTypeDetecte.setStyle("-fx-text-fill: #7C3AED; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else if (aAnalyse) {
            lblTypeDetecte.setText("🔬  Analyse uniquement");
            lblTypeDetecte.setStyle("-fx-text-fill: #0B4EA2; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else if (aImagerie) {
            lblTypeDetecte.setText("🩻  Imagerie uniquement");
            lblTypeDetecte.setStyle("-fx-text-fill: #0E9B8A; -fx-font-size: 12px; -fx-font-weight: bold;");
        } else {
            lblTypeDetecte.setText("ℹ  Aucun type spécifié");
            lblTypeDetecte.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        }

        // ── Section Analyse
        if (aAnalyse) {
            sectionAnalyse.setVisible(true);
            sectionAnalyse.setManaged(true);
            lblAnalysesInfo.setText("Demandé par le médecin : " + analyses);
            tfTypeAnalyse.setText(analyses);   // pré-rempli, modifiable
            cbInclureAnalyse.setSelected(true);
        } else {
            sectionAnalyse.setVisible(false);
            sectionAnalyse.setManaged(false);
            cbInclureAnalyse.setSelected(false);
        }

        // ── Section Imagerie
        if (aImagerie) {
            sectionImagerie.setVisible(true);
            sectionImagerie.setManaged(true);
            lblImageriesInfo.setText("Demandé par le médecin : " + imageries);
            tfTypeImagerie.setText(imageries); // pré-rempli, modifiable
            cbInclureImagerie.setSelected(true);
        } else {
            sectionImagerie.setVisible(false);
            sectionImagerie.setManaged(false);
            cbInclureImagerie.setSelected(false);
        }
    }

    private void masquerPanneauDemande() {
        panneauDemande.setVisible(false);
        panneauDemande.setManaged(false);
    }

    @FXML
    private void handleActualiser() {
        chargerPatientsEnvoyes();
        if (!cbIdConsultation.getItems().isEmpty()) {
            lblMessage.setStyle("-fx-text-fill: #0B4EA2; -fx-font-weight: bold; -fx-font-size: 12px;");
            lblMessage.setText("✔  " + cbIdConsultation.getItems().size() + " demande(s) en attente.");
        }
    }

    @FXML
    private void handleAjouter() {
        lblMessage.setText("");

        int idx = cbIdConsultation.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= idsConsultation.size()) {
            afficherErreur("Veuillez sélectionner un patient.");
            return;
        }

        boolean inclureAnalyse  = cbInclureAnalyse.isSelected()  && sectionAnalyse.isVisible();
        boolean inclureImagerie = cbInclureImagerie.isSelected() && sectionImagerie.isVisible();

        if (!inclureAnalyse && !inclureImagerie) {
            afficherErreur("Cochez au moins un type d'examen à ajouter.");
            return;
        }

        // Validation des champs texte
        if (inclureAnalyse && tfTypeAnalyse.getText().trim().isEmpty()) {
            afficherErreur("Le type d'analyse ne peut pas être vide.");
            return;
        }
        if (inclureImagerie && tfTypeImagerie.getText().trim().isEmpty()) {
            afficherErreur("Le type d'imagerie ne peut pas être vide.");
            return;
        }

        int    idConsultation = idsConsultation.get(idx);
        String nomPatient     = notifsList.get(idx).getNomPatient();
        int    nbAjoutes      = 0;

        try {
            // ── Insérer l'examen Analyse
            if (inclureAnalyse) {
                Examens_demandes ea = new Examens_demandes();
                ea.setIdConsultation(idConsultation);
                // Préfixe [ANALYSE] pour distinguer dans l'affichage
                ea.setTypeExamen("[ANALYSE] " + tfTypeAnalyse.getText().trim());
                ea.setDateDemande(new Date());
                ea.setStatut(cbStatut.getValue());
                ea.setUrgent(cbUrgent.isSelected());
                service.ajouterExamen(ea);
                nbAjoutes++;
            }

            // ── Insérer l'examen Imagerie
            if (inclureImagerie) {
                Examens_demandes ei = new Examens_demandes();
                ei.setIdConsultation(idConsultation);
                // Préfixe [IMAGERIE] pour distinguer dans l'affichage
                ei.setTypeExamen("[IMAGERIE] " + tfTypeImagerie.getText().trim());
                ei.setDateDemande(new Date());
                ei.setStatut(cbStatut.getValue());
                ei.setUrgent(cbUrgent.isSelected());
                service.ajouterExamen(ei);
                nbAjoutes++;
            }

            // ── Marquer la consultation comme reçue
            service.marquerConsultationRecue(idConsultation);

            // ── Retirer le patient de la liste
            cbIdConsultation.getItems().remove(idx);
            idsConsultation.remove(idx);
            notifsList.remove(idx);

            if (!cbIdConsultation.getItems().isEmpty()) {
                cbIdConsultation.getSelectionModel().selectFirst();
            } else {
                cbIdConsultation.getSelectionModel().clearSelection();
                cbIdConsultation.setPromptText("— Aucune demande en attente —");
                masquerPanneauDemande();
            }

            afficherSucces("✅  " + nbAjoutes + " examen(s) ajouté(s) pour " + nomPatient + " !");
            afficherAlertSucces(nbAjoutes + " examen(s) ajouté(s) avec succès pour :\n" + nomPatient);

        } catch (Exception ex) {
            afficherErreur("Erreur lors de l'ajout : " + ex.getMessage());
            afficherAlertErreur("Erreur :\n" + ex.getMessage());
        }
    }

    @FXML
    private void handleVider() {
        if (!cbIdConsultation.getItems().isEmpty())
            cbIdConsultation.getSelectionModel().selectFirst();
        cbStatut.setValue("En attente");
        cbUrgent.setSelected(false);
        lblMessage.setText("");
    }

    // ── Helpers UI
    private void afficherAlertSucces(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Succès"); a.setHeaderText(null); a.setContentText("✅  " + msg); a.showAndWait();
    }
    private void afficherAlertErreur(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    private void afficherSucces(String msg) {
        lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }
    private void afficherErreur(String msg) {
        lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold; -fx-font-size: 13px;");
        lblMessage.setText(msg);
    }

    // ── Effets hover sur les boutons icônes
    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setScaleX(1.15); btn.setScaleY(1.15);
        }
    }
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setScaleX(1.0); btn.setScaleY(1.0);
        }
    }
}

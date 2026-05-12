package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.OrdonnanceService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class SupprimerConsultationController implements Initializable {

    @FXML private Label lblPatient, lblId, lblMedecin, lblOrientation, lblOrdos;

    private final ConsultationService cs = new ConsultationService();
    private final OrdonnanceService   os = new OrdonnanceService();
    private Consultation consultation;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    /** Appelé depuis AfficherConsultationController */
    public void setConsultation(Consultation c) {
        this.consultation = c;
        lblPatient.setText(c.getNomPatient()   != null ? c.getNomPatient()   : "—");
        lblId.setText("#" + c.getIdConsultation());
        lblMedecin.setText(c.getNomMedecin()   != null ? c.getNomMedecin()   : "—");
        lblOrientation.setText(c.getOrientation() != null ? c.getOrientation() : "En cours");
        int nb = os.getByConsultation(c.getIdConsultation()).size();
        lblOrdos.setText(nb + " ordonnance(s) seront supprimées");
    }

    @FXML
    private void handleSupprimer() {
        if (consultation == null) return;
        cs.deleteEntity(consultation);
        System.out.println("Consultation supprimée : #" + consultation.getIdConsultation());
        naviguer("AfficherConsultation.fxml");
    }

    @FXML private void handleAnnuler()   { naviguer("AfficherConsultation.fxml"); }
    @FXML private void handleNavListe()  { naviguer("AfficherConsultation.fxml"); }

    private void naviguer(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/ResourcesMed/module3/fxml/" + fxml));
            ((Stage) lblPatient.getScene().getWindow()).setScene(new Scene(root));
        } catch (IOException e) { e.printStackTrace(); }
    }
}

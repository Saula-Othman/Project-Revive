package pro.revive.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import pro.revive.Navigator;
import pro.revive.entities.Triage;
import pro.revive.services.RoomAssignmentService;
import pro.revive.services.TriageService;

import java.net.URL;
import java.util.ResourceBundle;

public class TriageDeleteController implements Initializable {

    @FXML private Label lblPatient, lblNiveau, lblSalle, lblDate, lblWarning;

    private final TriageService service = new TriageService();
    private final RoomAssignmentService roomService = new RoomAssignmentService();
    private Triage current;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        current = service.getById(Navigator.currentTriageId);
        if (current == null) { Navigator.goTo("Triage_List"); return; }

        lblPatient.setText(current.getNomPatient() + " " + current.getPrenomPatient());
        lblNiveau.setText("Niveau " + current.getNiveauFinal());
        lblSalle.setText(current.getNomSalle() != null ? current.getNomSalle() : "En Attente");
        lblDate.setText(current.getDateHeureTriage() != null ? current.getDateHeureTriage().toString() : "N/A");

        if (current.getNomSalle() != null) {
            lblWarning.setText("Attention: La suppression de ce triage libera la salle '" + current.getNomSalle() +
                "'. Cette action est irreversible !");
        } else {
            lblWarning.setText("Attention: Cette action est irreversible. Le triage sera definitivement supprime.");
        }
    }

    @FXML
    public void confirmerSuppression() {
        try {
            // Fix: free the room first if patient was assigned one,
            // so room capacity is decremented and next waiting patient is auto-assigned
            if (current.getIdSalle() > 0) {
                roomService.freeRoom(current.getIdTriage());
            }
            service.deleteEntity(current);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Triage supprime avec succes.");
            a.setTitle("Succes"); a.showAndWait();
            Navigator.goTo("Triage_List");
        } catch (Exception e) {
            Alert a = new Alert(Alert.AlertType.ERROR, "Erreur suppression: " + e.getMessage());
            a.setTitle("Erreur"); a.showAndWait();
        }
    }

    @FXML public void goDashboard()  { Navigator.goTo("DashboardTriage"); }
    @FXML public void goTriageList() { Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()  { Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()  { Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()  { Navigator.goTo("DashboardTriage"); } // placeholder until login screen exists
    @FXML public void goSurveillance() { Navigator.goTo("Surveillance"); }
}

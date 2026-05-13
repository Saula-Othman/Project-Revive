package pro.revive.controllers.ControllersAdmission;

import pro.revive.Navigator;
import pro.revive.SessionManager;
import pro.revive.daoAdmission.AdmissionDAO;
import pro.revive.daoAdmission.AmbulanceDAO;
import pro.revive.entities.EntitiesUser.Personne;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.Timer;
import java.util.TimerTask;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Label userNameLabel;
    @FXML private Label avatarLabel;
    @FXML private Label statAdmissions;
    @FXML private Label statAttente;
    @FXML private Label statAmbulances;
    @FXML private Button btnDashboard;
    @FXML private Button btnPatients;
    @FXML private Button btnAdmissions;
    @FXML private Button btnAmbulances;

    private static MainController instance;
    private static int currentPersonnelId;
    private Timer statsTimer;

    public static MainController getInstance() { return instance; }
    public static int getPersonnelId() { return currentPersonnelId; }

    public void initData() {
        instance = this;
        // Pull the real logged-in user from the global session
        Personne user = SessionManager.getUser();
        if (user != null) {
            currentPersonnelId = user.getIdPersonnel();
            String fullName = user.getPrenom() + " " + user.getNom();
            if (userNameLabel != null) userNameLabel.setText(fullName);
            if (avatarLabel != null) {
                String initials = "";
                if (user.getPrenom() != null && !user.getPrenom().isEmpty())
                    initials += user.getPrenom().substring(0, 1).toUpperCase();
                if (user.getNom() != null && !user.getNom().isEmpty())
                    initials += user.getNom().substring(0, 1).toUpperCase();
                avatarLabel.setText(initials);
            }
        } else {
            currentPersonnelId = -1;
            if (userNameLabel != null) userNameLabel.setText("Agent Accueil");
        }
        showDashboard();
        startStatsRefresh();
    }

    private void startStatsRefresh() {
        refreshStats();
        statsTimer = new Timer(true);
        statsTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { Platform.runLater(() -> refreshStats()); }
        }, 30000, 30000);
    }

    public void refreshStats() {
        try {
            AdmissionDAO admDAO = new AdmissionDAO();
            AmbulanceDAO ambDAO = new AmbulanceDAO();
            int adm = admDAO.countToday();
            int att = admDAO.countWaiting();
            int amb = ambDAO.findActiveAmbulances().size();
            statAdmissions.setText(String.valueOf(adm));
            statAttente.setText(String.valueOf(att));
            statAmbulances.setText(String.valueOf(amb));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // PUBLIC so other controllers can call them
    @FXML public void showDashboard() { loadView("/ResourceAdmission/urgence/fxml/DashboardAdmission.fxml", btnDashboard); }
    @FXML public void showPatients()  { loadView("/ResourceAdmission/urgence/fxml/PatientsCards.fxml", btnPatients); }
    @FXML public void showAdmissions(){ loadView("/ResourceAdmission/urgence/fxml/AdmissionsCards.fxml", btnAdmissions); }
    @FXML public void showAmbulances(){ loadView("/ResourceAdmission/urgence/fxml/AmbulancesAdmission.fxml", btnAmbulances); }

    public void refreshStatsNow() { refreshStats(); }

    public void loadView(String fxmlPath, Button activeBtn) {
        try {
            for (Button b : new Button[]{btnDashboard, btnPatients, btnAdmissions, btnAmbulances}) {
                if (b != null) b.getStyleClass().remove("sidebar-btn-active");
            }
            if (activeBtn != null) activeBtn.getStyleClass().add("sidebar-btn-active");
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML private void handleLogout() {
        if (statsTimer != null) statsTimer.cancel();
        instance = null;
        Navigator.logout();   // clears SessionManager and navigates back to Login.fxml
    }
}

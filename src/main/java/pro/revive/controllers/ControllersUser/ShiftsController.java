package pro.revive.controllers.ControllersUser;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.services.ServicesUser.EmailService;
import pro.revive.services.ServicesUser.PersonneService;
import pro.revive.services.ServicesUser.ShiftService;
import pro.revive.utils.UtilsUser.AnimationUtil;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.ResourceBundle;

public class ShiftsController implements Initializable {

    @FXML private DatePicker       dpWeekStart;
    @FXML private ComboBox<String> cbAgent;
    @FXML private ComboBox<String> cbTypeShift;
    @FXML private ScrollPane       spTimetable;
    @FXML private Label            lblUserName;
    @FXML private Label            lblUserRole;
    @FXML private Label            lblWeekTitle;

    private final ShiftService    shiftSvc    = new ShiftService();
    private final PersonneService personneSvc = new PersonneService();
    private Personne currentUser;

    private final Map<String, Integer>  agentIdMap    = new LinkedHashMap<>();
    private final Map<String, String>   agentEmailMap = new LinkedHashMap<>();
    private final Map<String, Personne> agentMap      = new LinkedHashMap<>();

    private static final String[] DAYS_FR = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbTypeShift.getItems().addAll(ShiftService.MATIN, ShiftService.SOIR, ShiftService.NUIT);
        cbTypeShift.setValue(ShiftService.MATIN);

        LocalDate monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        dpWeekStart.setValue(monday);

        // Block past dates and non-Mondays
        dpWeekStart.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now()) || date.getDayOfWeek() != DayOfWeek.MONDAY) {
                    setDisable(true);
                    setStyle("-fx-background-color: #F1F5F9; -fx-text-fill: #CBD5E1;");
                }
            }
        });

        dpWeekStart.valueProperty().addListener((obs, o, n) -> {
            if (n != null) loadTimetable(n);
        });

        loadAgents();
        loadTimetable(monday);
    }

    public void setCurrentUser(Personne user) {
        this.currentUser = user;
        if (lblUserName != null) lblUserName.setText(user.getNom() + " " + user.getPrenom());
        if (lblUserRole != null) lblUserRole.setText(user.getRole());
    }

    private void loadAgents() {
        agentIdMap.clear(); agentEmailMap.clear(); agentMap.clear();
        cbAgent.getItems().clear();
        List<Personne> agents = personneSvc.getData();
        for (Personne p : agents) {
            String display = p.getNom() + " " + p.getPrenom() + " (" + p.getRole() + ")";
            agentIdMap.put(display, p.getIdPersonnel());
            agentEmailMap.put(display, p.getEmail());
            agentMap.put(display, p);
            cbAgent.getItems().add(display);
        }
        if (!cbAgent.getItems().isEmpty()) cbAgent.setValue(cbAgent.getItems().get(0));
    }

    @FXML void addWeeklyShift() {
        LocalDate weekStart = dpWeekStart.getValue();
        String agentDisplay = cbAgent.getValue();
        String type = cbTypeShift.getValue();

        if (weekStart == null || agentDisplay == null || type == null) {
            showAlert("Veuillez remplir tous les champs.");
            return;
        }

        LocalDate monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Integer agentId = agentIdMap.get(agentDisplay);
        String agentEmail = agentEmailMap.get(agentDisplay);
        if (agentId == null) return;

        // Add shift for each day Mon-Sun
        LocalDate day = monday;
        for (int i = 0; i < 7; i++) {
            shiftSvc.addShift(agentId, day, type);
            day = day.plusDays(1);
        }

        // Send email
        if (agentEmail != null && !agentEmail.isBlank()) {
            Personne p = agentMap.get(agentDisplay);
            if (p != null) {
                EmailService.sendShiftNotification(agentEmail, p.getNom(), p.getPrenom(),
                        type, monday, monday.plusDays(6));
            }
        }

        loadTimetable(dpWeekStart.getValue());
    }

    private void loadTimetable(LocalDate weekStart) {
        if (weekStart == null) return;
        LocalDate monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);

        if (lblWeekTitle != null) {
            lblWeekTitle.setText("Emploi du temps — Semaine du "
                    + monday.format(FMT) + " au " + sunday.format(FMT));
        }

        // Build timetable grid
        GridPane grid = new GridPane();
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setStyle("-fx-background-color: #E5E7EB; -fx-padding: 2px;");

        // Column constraints
        ColumnConstraints agentCol = new ColumnConstraints(200);
        agentCol.setHgrow(Priority.NEVER);
        grid.getColumnConstraints().add(agentCol);
        for (int d = 0; d < 7; d++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(100);
            grid.getColumnConstraints().add(cc);
        }

        // Header row — days
        Label agentHeader = makeCell("Agent / Jour", "#0B4EA2", "#ffffff", true);
        grid.add(agentHeader, 0, 0);
        LocalDate day = monday;
        for (int d = 0; d < 7; d++) {
            String dayLabel = DAYS_FR[d] + "\n" + day.format(FMT);
            Label dayCell = makeCell(dayLabel, "#0B4EA2", "#ffffff", true);
            grid.add(dayCell, d + 1, 0);
            day = day.plusDays(1);
        }

        // Build a map: agentId -> dayOfWeek -> shiftType
        Map<Integer, Map<Integer, String>> shiftMap = new HashMap<>();
        for (int d = 0; d < 7; d++) {
            LocalDate currentDay = monday.plusDays(d);
            List<String[]> shifts = shiftSvc.getShiftsForDate(currentDay);
            for (String[] s : shifts) {
                // s: [id, nom_agent, role, type_shift]
                // We need agent id — get from DB via name match
                int agentId = getAgentIdByName(s[1]);
                if (agentId > 0) {
                    shiftMap.computeIfAbsent(agentId, k -> new HashMap<>()).put(d, s[3]);
                }
            }
        }

        // Agent rows
        List<Personne> agents = personneSvc.getData();
        int row = 1;
        for (Personne p : agents) {
            // Only show agents who have at least one shift this week
            Map<Integer, String> agentShifts = shiftMap.get(p.getIdPersonnel());

            // Agent name cell
            String agentName = p.getNom() + " " + p.getPrenom();
            Label nameCell = makeCell(agentName + "\n" + p.getRole(), "#F8FAFC", "#1A1D23", false);
            nameCell.setStyle(nameCell.getStyle() + "-fx-font-weight: bold;");
            grid.add(nameCell, 0, row);

            // Day cells
            for (int d = 0; d < 7; d++) {
                String shiftType = agentShifts != null ? agentShifts.get(d) : null;
                Label shiftCell;
                if (shiftType != null) {
                    String shortType = shiftType.startsWith("Matin") ? "Matin\n06h-14h"
                                     : shiftType.startsWith("Soir")  ? "Soir\n14h-22h"
                                     : "Nuit\n22h-06h";
                    shiftCell = makeCell(shortType, getShiftBg(shiftType), "#ffffff", false);
                } else {
                    shiftCell = makeCell("—", "#F9FAFB", "#CBD5E1", false);
                }
                grid.add(shiftCell, d + 1, row);
            }
            row++;
        }

        // If no agents, show message
        if (agents.isEmpty()) {
            Label empty = new Label("Aucun agent enregistre.");
            empty.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px; -fx-padding: 20px;");
            spTimetable.setContent(empty);
            return;
        }

        spTimetable.setContent(grid);
        spTimetable.setFitToWidth(true);
    }

    private Label makeCell(String text, String bgColor, String textColor, boolean isHeader) {
        Label lbl = new Label(text);
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setMaxHeight(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        lbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-background-color: " + bgColor + "; "
                + "-fx-text-fill: " + textColor + "; "
                + "-fx-padding: " + (isHeader ? "10px 8px" : "8px 6px") + "; "
                + "-fx-font-size: " + (isHeader ? "12px" : "11px") + "; "
                + (isHeader ? "-fx-font-weight: bold;" : ""));
        return lbl;
    }

    private String getShiftBg(String type) {
        if (type == null) return "#F9FAFB";
        if (type.startsWith("Matin"))  return "#059669";
        if (type.startsWith("Soir"))   return "#0891B2";
        if (type.startsWith("Nuit"))   return "#7C3AED";
        return "#94A3B8";
    }

    private int getAgentIdByName(String fullName) {
        for (Map.Entry<String, Integer> e : agentIdMap.entrySet()) {
            if (e.getKey().startsWith(fullName)) return e.getValue();
        }
        return -1;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Attention"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }

    @FXML void goDashboard() { navTo("/ResourcesUser/images/fxml/M6_Dashboard.fxml"); }
    @FXML void goPersonnel() { navTo("/ResourcesUser/images/fxml/M6_Personnel_List.fxml"); }
    @FXML void goHistorique() { navTo("/ResourcesUser/images/fxml/Historique.fxml"); }
    @FXML void deconnexion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResourcesUser/images/fxml/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) spTimetable.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navTo(String path) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent root = loader.load();
            if (path.contains("Dashboard") && currentUser != null)
                ((M6DashboardController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("List") && currentUser != null)
                ((M6PersonnelListController) loader.getController()).setCurrentUser(currentUser);
            else if (path.contains("Historique") && currentUser != null)
                ((HistoriqueController) loader.getController()).setCurrentUser(currentUser);
            Stage stage = (Stage) spTimetable.getScene().getWindow();
            AnimationUtil.navigateWithFade(stage, root, () -> {});
        } catch (Exception e) { e.printStackTrace(); }
    }
}

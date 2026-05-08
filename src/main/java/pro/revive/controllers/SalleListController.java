package pro.revive.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pro.revive.Navigator;
import pro.revive.entities.Salle;
import pro.revive.services.SalleService;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class SalleListController implements Initializable {

    @FXML private Label lblUserName, lblOccupation, lblSallesInfo;
    @FXML private Label statTotal, statDisponibles, statPleines, statPatients;
    @FXML private FlowPane salleGrid;
    @FXML private Button btnAll, btnDispo, btnPleines;

    private final SalleService service = new SalleService();
    private List<Salle> allSalles;
    private int activeFilter = 0; // 0=all 1=dispo 2=plein
    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        loadData();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    @FXML public void refresh() { loadData(); }

    private void loadData() {
        allSalles = service.getData();

        long dispo = allSalles.stream().filter(Salle::isAvailable).count();
        long pleine = allSalles.stream().filter(s -> !s.isAvailable()).count();
        int totalPat = allSalles.stream().mapToInt(Salle::getNombreActuel).sum();

        statTotal.setText(String.valueOf(allSalles.size()));
        statDisponibles.setText(String.valueOf(dispo));
        statPleines.setText(String.valueOf(pleine));
        statPatients.setText(String.valueOf(totalPat));
        lblOccupation.setText(dispo + " salle(s) disponible(s)");
        lblSallesInfo.setText(allSalles.size() + " salles au total");

        renderGrid(allSalles);
    }

    private void renderGrid(List<Salle> list) {
        salleGrid.getChildren().clear();
        for (Salle s : list) {
            salleGrid.getChildren().add(buildSalleCard(s));
        }
    }

    private void applyFilter() {
        List<Salle> filtered = allSalles.stream()
            .filter(s -> activeFilter == 0 ||
                (activeFilter == 1 && s.isAvailable()) ||
                (activeFilter == 2 && !s.isAvailable()))
            .collect(Collectors.toList());
        renderGrid(filtered);
    }

    @FXML public void filterAll()    { activeFilter = 0; setActive(btnAll);    applyFilter(); }
    @FXML public void filterDispo()  { activeFilter = 1; setActive(btnDispo);  applyFilter(); }
    @FXML public void filterPleines(){ activeFilter = 2; setActive(btnPleines); applyFilter(); }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnAll, btnDispo, btnPleines}) b.getStyleClass().remove("active");
        if (!active.getStyleClass().contains("active")) active.getStyleClass().add("active");
    }

    private VBox buildSalleCard(Salle s) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card");
        card.setPrefWidth(280);

        // Top
        HBox top = new HBox(10);
        top.getStyleClass().add("rcard-top");
        top.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("[H]"); icon.getStyleClass().add("rcard-icon");
        VBox nameBox = new VBox(2);
        Label rname = new Label(s.getNomSalle()); rname.getStyleClass().add("rcard-name");
        Label rtype = new Label(s.getTypeSalle()); rtype.getStyleClass().add("rcard-type");
        nameBox.getChildren().addAll(rname, rtype);
        top.getChildren().addAll(icon, nameBox);

        // Capacity row
        HBox capRow = new HBox(4);
        capRow.getStyleClass().add("cap-row");
        capRow.setAlignment(Pos.CENTER_LEFT);
        Label capLbl = new Label("Occupation:"); capLbl.getStyleClass().add("cap-txt");
        Label capNum = new Label(s.getNombreActuel() + "/" + s.getCapaciteMax()); capNum.getStyleClass().add("cap-num");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        double pct = s.getCapaciteMax() > 0 ? (double) s.getNombreActuel() / s.getCapaciteMax() : 0;
        int pctInt = (int)(pct * 100);
        Label pctLbl = new Label(pctInt + "%"); pctLbl.getStyleClass().add("pct");
        capRow.getChildren().addAll(capLbl, capNum, sp, pctLbl);

        // Progress
        ProgressBar pb = new ProgressBar(pct);
        pb.setMaxWidth(Double.MAX_VALUE);
        String pbStyle = pct >= 1.0 ? "pf-red" : pct >= 0.7 ? "pf-orange" : "pf-green";
        pb.getStyleClass().addAll("prog-wrap", pbStyle);

        // Footer
        HBox footer = new HBox(6);
        footer.getStyleClass().add("rcard-footer");
        footer.setAlignment(Pos.CENTER_LEFT);

        String statusText = s.isAvailable() ? "DISPONIBLE" : "PLEINE";
        String statusCss = s.isAvailable() ? "rs-ok" : "rs-full";
        Label statusLbl = new Label(statusText); statusLbl.getStyleClass().addAll("rstatus", statusCss);

        Label nivBadge = new Label("Niv." + s.getNiveauGraviteCible()); nivBadge.getStyleClass().add("niv-badge");
        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

        Button btnView = new Button("👁  Voir");
        btnView.getStyleClass().addAll("act-icon", "act-view");
        btnView.setOnAction(e -> { Navigator.currentSalleId = s.getIdSalle(); Navigator.goTo("Salle_View"); });

        footer.getChildren().addAll(statusLbl, nivBadge, sp2, btnView);

        card.getChildren().addAll(top, capRow, pb, footer);
        return card;
    }

    private void stopAutoRefresh() { if (autoRefresh != null) autoRefresh.stop(); }

    @FXML public void goDashboard()    { stopAutoRefresh(); Navigator.goTo("Dashboard"); }
    @FXML public void goTriageList()   { stopAutoRefresh(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { stopAutoRefresh(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { stopAutoRefresh(); Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { stopAutoRefresh(); Navigator.goTo("Dashboard"); }
    @FXML public void goSurveillance() { stopAutoRefresh(); Navigator.goTo("Surveillance"); }
}

package pro.revive.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.util.Duration;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pro.revive.Navigator;
import pro.revive.entities.Triage;
import pro.revive.services.TriageService;
import pro.revive.utils.AppExecutor;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class TriageListController implements Initializable {

    @FXML private Label lblUserName;
    @FXML private Label lblSubtitle;
    @FXML private Label alertCount;
    @FXML private TextField searchField;
    @FXML private VBox triageList;      // container injected from FXML

    @FXML private Button btnAll, btnN1, btnN2, btnN3, btnN45;

    private final TriageService service = new TriageService();
    private List<Triage> allTriages;
    private int activeFilter = 0;

    // Virtualized list — only visible rows are rendered
    private final ObservableList<Triage> displayedItems = FXCollections.observableArrayList();
    private ListView<Triage> listView;
    private Timeline autoRefresh;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        lblUserName.setText(Navigator.currentUserName);
        setupListView();
        loadData();
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> loadData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    // ── Set up ListView once and add it to the FXML VBox ────────
    private void setupListView() {
        listView = new ListView<>(displayedItems);
        listView.setMaxHeight(Double.MAX_VALUE);
        listView.getStyleClass().add("triage-listview");
        VBox.setVgrow(listView, Priority.ALWAYS);

        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Triage t, boolean empty) {
                super.updateItem(t, empty);
                if (empty || t == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0;");
                } else {
                    setGraphic(buildCard(t));
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4 0;");
                }
            }
        });

        triageList.getChildren().setAll(listView);
    }

    // ── Data loading ─────────────────────────────────────────────
    private void loadData() {
        AppExecutor.run(() -> {
            List<Triage> triages = service.getData();
            long crit = triages.stream().filter(t -> t.getNiveauFinal() == 1).count();

            long n2  = triages.stream().filter(t -> t.getNiveauFinal() == 2).count();
            long n3  = triages.stream().filter(t -> t.getNiveauFinal() == 3).count();
            long n45 = triages.stream().filter(t -> t.getNiveauFinal() >= 4).count();

            javafx.application.Platform.runLater(() -> {
                allTriages = triages;
                lblSubtitle.setText(triages.size() + " triage(s) enregistre(s)");
                alertCount.setText(crit + " alerte(s)");

                // Update filter button labels with live counts
                btnAll.setText("Tous (" + triages.size() + ")");
                btnN1.setText("🔴  Critique (" + crit + ")");
                btnN2.setText("🟠  Tres Urgent (" + n2 + ")");
                btnN3.setText("🟡  Urgent (" + n3 + ")");
                btnN45.setText("🟢  Standard/Mineur (" + n45 + ")");

                renderList(triages);
            });
        });
    }

    private void renderList(List<Triage> list) {
        displayedItems.setAll(list);
        // Show placeholder label if empty
        listView.setPlaceholder(new Label("Aucun triage trouve."));
    }

    private void applyFilter() {
        if (allTriages == null) return;
        String search = searchField.getText().toLowerCase().trim();
        List<Triage> filtered = allTriages.stream()
            .filter(t -> activeFilter == 0 ||
                (activeFilter == 1  && t.getNiveauFinal() == 1) ||
                (activeFilter == 2  && t.getNiveauFinal() == 2) ||
                (activeFilter == 3  && t.getNiveauFinal() == 3) ||
                (activeFilter == 45 && t.getNiveauFinal() >= 4))
            .filter(t -> search.isEmpty() ||
                (t.getNomPatient() + " " + t.getPrenomPatient()).toLowerCase().contains(search))
            .collect(Collectors.toList());
        renderList(filtered);
    }

    // ── Filter buttons ───────────────────────────────────────────
    @FXML public void filterAll() { activeFilter = 0;  setActive(btnAll);  applyFilter(); }
    @FXML public void filterN1()  { activeFilter = 1;  setActive(btnN1);   applyFilter(); }
    @FXML public void filterN2()  { activeFilter = 2;  setActive(btnN2);   applyFilter(); }
    @FXML public void filterN3()  { activeFilter = 3;  setActive(btnN3);   applyFilter(); }
    @FXML public void filterN45() { activeFilter = 45; setActive(btnN45);  applyFilter(); }
    @FXML public void onSearch()  { applyFilter(); }

    private void setActive(Button active) {
        for (Button b : new Button[]{btnAll, btnN1, btnN2, btnN3, btnN45}) {
            b.getStyleClass().remove("active");
        }
        if (!active.getStyleClass().contains("active")) active.getStyleClass().add("active");
    }

    // ── Card builder (same visual design, now used by ListCell) ──
    private HBox buildCard(Triage t) {
        HBox card = new HBox();
        card.getStyleClass().add("pcard");
        card.setSpacing(0);

        Region stripe = new Region();
        stripe.getStyleClass().addAll("stripe", "s" + t.getNiveauFinal());
        stripe.setPrefWidth(5); stripe.setMinWidth(5);

        VBox info = new VBox(3);
        info.getStyleClass().add("pinfo");
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(t.getNomPatient() + " " + t.getPrenomPatient());
        name.getStyleClass().add("pname");
        Label vitals = new Label(
            "Pouls: " + t.getConstancesPouls() + " bpm  |  " +
            "Temp: " + t.getConstancesTemperature() + "°C  |  " +
            "SpO2: " + t.getSpo2() + "%  |  Score: " + t.getScoreCalcule()
        );
        vitals.getStyleClass().add("pvitals");
        Label sym = new Label(t.getSymptomes() != null ? t.getSymptomes() : "");
        sym.getStyleClass().add("psymptom");
        info.getChildren().addAll(name, vitals, sym);

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.getStyleClass().add("pright");

        Label badge = new Label("Niveau " + t.getNiveauFinal());
        badge.getStyleClass().addAll("badge", "b" + t.getNiveauFinal());
        Label salleTag = new Label(t.getNomSalle() != null ? t.getNomSalle() : "En Attente");
        salleTag.getStyleClass().add("salle-tag");

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnView = new Button("👁  Voir");
        btnView.getStyleClass().addAll("act-icon", "act-view");
        btnView.setOnAction(e -> Navigator.goToTriage(t.getIdTriage()));

        Button btnEdit = new Button("✏  Modifier");
        btnEdit.getStyleClass().addAll("act-icon", "act-edit");
        btnEdit.setOnAction(e -> Navigator.goToTriageEdit(t.getIdTriage()));

        Button btnDel = new Button("🗑  Suppr.");
        btnDel.getStyleClass().addAll("act-icon", "act-del");
        btnDel.setOnAction(e -> Navigator.goToTriageDelete(t.getIdTriage()));

        actions.getChildren().addAll(btnView, btnEdit, btnDel);
        right.getChildren().addAll(badge, salleTag, actions);
        card.getChildren().addAll(stripe, info, right);
        return card;
    }

    // ── Navigation ───────────────────────────────────────────────
    private void stopAutoRefresh() { if (autoRefresh != null) autoRefresh.stop(); }

    @FXML public void refresh()        { loadData(); }
    @FXML public void goDashboard()    { stopAutoRefresh(); Navigator.goTo("Dashboard"); }
    @FXML public void goTriageList()   { stopAutoRefresh(); Navigator.goTo("Triage_List"); }
    @FXML public void goTriageAdd()    { stopAutoRefresh(); Navigator.goTo("Triage_Add"); }
    @FXML public void goSalleList()    { stopAutoRefresh(); Navigator.goTo("Salle_List"); }
    @FXML public void deconnexion()    { stopAutoRefresh(); Navigator.goTo("Dashboard"); }
    @FXML public void goSurveillance() { stopAutoRefresh(); Navigator.goTo("Surveillance"); }
}

package pro.revive.controllers.ControllersMed;



import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.OrdonnanceService;
import pro.revive.services.ServicesMed.PostConsultationEmailOrchestrator;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ConsultationListController implements Initializable {

    @FXML private TextField        searchField;
    @FXML private ComboBox<String> filterCombo;
    @FXML private Label            labelCount;
    @FXML private VBox             cardsContainer;
    @FXML private ScrollPane       cardsScrollPane;
    @FXML private Label            lblStatTotal;
    @FXML private Label            lblStatEnCours;
    @FXML private Label            lblStatCloturees;
    @FXML private Label            lblStatOrdos;
    @FXML private StackPane        overlayBackground;
    @FXML private StackPane        overlayContainer;
    @FXML private Label            lblSideUserName;

    private final ConsultationService cs = new ConsultationService();
    private final OrdonnanceService   os = new OrdonnanceService();
    private final ObservableList<Consultation> masterList = FXCollections.observableArrayList();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Afficher le nom de l'utilisateur connecté dans le chip sidebar
        if (lblSideUserName != null) {
            String nom = pro.revive.SessionManager.getFullName();
            lblSideUserName.setText(nom != null && !nom.isBlank() ? nom : "Médecin");
        }
        filterCombo.setItems(FXCollections.observableArrayList(
                "Tous", "En cours", "Cloturees", "Hospitalisation", "Transfert", "Sortie"));
        filterCombo.setValue("Tous");
        searchField.textProperty().addListener((obs, o, n) -> appliquerFiltres());
        filterCombo.valueProperty().addListener((obs, o, n) -> appliquerFiltres());

        if (cardsScrollPane != null) cardsScrollPane.setCache(false);

        chargerDonnees();

        // Injecter l'assistant vocal dès que la scène est attachée
        cardsContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && newScene.getRoot() instanceof javafx.scene.layout.StackPane sp) {
                boolean dejaPresent = sp.getChildren().stream()
                    .anyMatch(n -> "assistant".equals(n.getUserData()));
                if (!dejaPresent) {
                    GlobalAssistantController assistant = new GlobalAssistantController(sp);
                    assistant.setNavigationCallback(fxml -> {
                        try {
                            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/" + fxml);
                            if (fxmlUrl == null) return;
                            javafx.scene.Parent newRoot = FXMLLoader.load(fxmlUrl);
                            javafx.stage.Stage stage = (javafx.stage.Stage) cardsContainer.getScene().getWindow();
                            stage.setScene(creerScene(newRoot));
                            if (!stage.isMaximized()) stage.setMaximized(true);
                        } catch (Exception e) {
                            System.out.println("[ConsultList] Nav: " + e.getMessage());
                        }
                    });
                }
            }
        });
    }

    public void chargerDonnees() {
        List<Consultation> liste = cs.getData();
        masterList.setAll(liste);
        mettreAJourStats(liste);
        appliquerFiltres();
    }

    private void mettreAJourStats(List<Consultation> liste) {
        long enCours   = liste.stream().filter(c -> c.getDateHeureFin() == null).count();
        long cloturees = liste.stream().filter(c -> c.getDateHeureFin() != null).count();
        long ordos     = liste.stream().mapToLong(c -> os.getByConsultation(c.getIdConsultation()).size()).sum();
        if (lblStatTotal    != null) lblStatTotal.setText(String.valueOf(liste.size()));
        if (lblStatEnCours  != null) lblStatEnCours.setText(String.valueOf(enCours));
        if (lblStatCloturees!= null) lblStatCloturees.setText(String.valueOf(cloturees));
        if (lblStatOrdos    != null) lblStatOrdos.setText(String.valueOf(ordos));
    }

    private void appliquerFiltres() {
        String query  = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String filtre = filterCombo.getValue() == null ? "Tous" : filterCombo.getValue();
        List<Consultation> result = masterList.stream()
                .filter(c -> switch (filtre) {
                    case "En cours"        -> c.getDateHeureFin() == null;
                    case "Cloturees"       -> c.getDateHeureFin() != null;
                    case "Hospitalisation" -> "Hospitalisation".equalsIgnoreCase(c.getOrientation());
                    case "Transfert"       -> "Transfert".equalsIgnoreCase(c.getOrientation());
                    case "Sortie"          -> "Sortie".equalsIgnoreCase(c.getOrientation());
                    default                -> true;
                })
                .filter(c -> query.isEmpty()
                        || nvl(c.getNomPatient()).toLowerCase().contains(query)
                        || nvl(c.getNomMedecin()).toLowerCase().contains(query)
                        || nvl(c.getDiagnostic()).toLowerCase().contains(query))
                .collect(Collectors.toList());
        labelCount.setText(result.size() + " consultation(s)");
        populateCards(result);
    }

    private void populateCards(List<Consultation> liste) {
        cardsContainer.getChildren().clear();

        if (liste.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label icon = new Label("📋");
            icon.setStyle("-fx-font-size: 40px; -fx-opacity: 0.3;");
            Label msg = new Label("Aucune consultation trouvee.");
            msg.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            empty.getChildren().addAll(icon, msg);
            cardsContainer.getChildren().add(empty);
        } else {
            for (Consultation c : liste) {
                cardsContainer.getChildren().add(buildCard(c));
            }
        }

        // Forcer JavaFX à redessiner complètement
        cardsContainer.requestLayout();
        cardsScrollPane.setVvalue(0); // Remonter en haut
    }

    private HBox buildCard(Consultation c) {
        Region colorBar = new Region();
        colorBar.setPrefWidth(5);
        colorBar.setMinWidth(5);
        colorBar.setMaxWidth(5);
        colorBar.setPrefHeight(Double.MAX_VALUE);
        colorBar.getStyleClass().add(getStatusBarClass(c));

        VBox content = new VBox(6);
        content.setPadding(new Insets(12, 16, 12, 14));
        HBox.setHgrow(content, Priority.ALWAYS);

        HBox line1 = new HBox(10);
        line1.setAlignment(Pos.CENTER_LEFT);
        Label lblPatient = new Label(nvl(c.getNomPatient()));
        lblPatient.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a1a2e;");
        Label badge = new Label(getStatutLabel(c));
        badge.getStyleClass().add(getStatusBadgeClass(c));
        Region sp1 = new Region();
        HBox.setHgrow(sp1, Priority.ALWAYS);
        Label lblDate = new Label(c.getDateHeureDebut() != null ? c.getDateHeureDebut().format(FMT) : "-");
        lblDate.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        line1.getChildren().addAll(lblPatient, badge, sp1, lblDate);

        HBox line2 = new HBox(16);
        line2.setAlignment(Pos.CENTER_LEFT);
        Label lblMedecin = new Label("Dr. " + nvl(c.getNomMedecin()));
        lblMedecin.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12px;");
        String diagText = c.getDiagnostic() != null
                ? (c.getDiagnostic().length() > 55 ? c.getDiagnostic().substring(0, 55) + "..." : c.getDiagnostic())
                : "-";
        Label lblDiag = new Label(diagText);
        lblDiag.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        if (c.getDiagnostic() != null && c.getDiagnostic().length() > 55) {
            Tooltip tip = new Tooltip(c.getDiagnostic());
            tip.setWrapText(true);
            tip.setMaxWidth(420);
            Tooltip.install(lblDiag, tip);
        }
        Region sp2 = new Region();
        HBox.setHgrow(sp2, Priority.ALWAYS);
        Label lblFin = new Label(c.getDateHeureFin() != null
                ? "Fin : " + c.getDateHeureFin().format(FMT) : "");
        lblFin.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 10px;");
        line2.getChildren().addAll(lblMedecin, lblDiag, sp2, lblFin);
        content.getChildren().addAll(line1, line2);

        HBox actions = new HBox(6);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 14, 0, 8));

        boolean cloturee = c.getDateHeureFin() != null;

        Button btnEdit   = hoverIconBtn("✎",  "✎  Modifier",    "pill-edit");
        Button btnClose  = hoverIconBtn("✓",  "✓  Clôturer",    "pill-close");
        Button btnRx     = hoverIconBtn("💊", "💊  Ordonnance",  "pill-rx");
        Button btnExam   = hoverIconBtn("🔬", "🔬  Examens",     "pill-exam");
        Button btnDelete = hoverIconBtn("🗑", "🗑  Supprimer",   "pill-delete");

        btnEdit.setDisable(cloturee);
        btnClose.setDisable(cloturee);

        btnEdit.setOnAction(e   -> ouvrirModifier(c));
        btnClose.setOnAction(e  -> cloturer(c));
        btnRx.setOnAction(e     -> ouvrirOrdonnances(c));
        btnExam.setOnAction(e   -> ouvrirDemandeExamen(c));
        btnDelete.setOnAction(e -> confirmerSuppression(c));

        actions.getChildren().addAll(btnRx, btnExam, btnEdit, btnClose, btnDelete);

        HBox card = new HBox(0);
        card.getStyleClass().add("consultation-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().addAll(colorBar, content, actions);
        return card;
    }

    private Button pillBtn(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        return btn;
    }

    /** Symbole au repos, texte complet au hover */
    private void addHoverText(Button btn, String symbol, String fullText) {
        btn.setOnMouseEntered(e -> {
            btn.setText(fullText);
            btn.setStyle("-fx-pref-width: -1;");
        });
        btn.setOnMouseExited(e -> {
            btn.setText(symbol);
            btn.setStyle("-fx-pref-width: -1;");
        });
    }

    /** Bouton simple avec texte — au hover l'icône apparaît devant le texte */
    private Button hoverIconBtn(String normalText, String hoverText, String styleClass) {
        Button btn = new Button(normalText);
        btn.getStyleClass().add(styleClass);
        btn.setOnMouseEntered(e -> btn.setText(hoverText));
        btn.setOnMouseExited(e  -> btn.setText(normalText));
        return btn;
    }

    private Button iconBtn(String text, String styleClass, String tooltipText) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        Tooltip.install(btn, new Tooltip(tooltipText));
        return btn;
    }

    private String getStatusBarClass(Consultation c) {
        if (c.getDateHeureFin() != null)                  return "status-bar-cloturee";
        if ("Hospitalisation".equals(c.getOrientation())) return "status-bar-hospitalisation";
        if ("Transfert".equals(c.getOrientation()))       return "status-bar-transfert";
        if ("Sortie".equals(c.getOrientation()))          return "status-bar-sortie";
        return "status-bar-encours";
    }

    private String getStatusBadgeClass(Consultation c) {
        if (c.getDateHeureFin() != null)                  return "status-badge-cloturee";
        if ("Hospitalisation".equals(c.getOrientation())) return "status-badge-hospitalisation";
        if ("Transfert".equals(c.getOrientation()))       return "status-badge-transfert";
        if ("Sortie".equals(c.getOrientation()))          return "status-badge-sortie";
        return "status-badge-encours";
    }

    private String getStatutLabel(Consultation c) {
        if (c.getDateHeureFin() != null)                  return "Cloturee";
        if ("Hospitalisation".equals(c.getOrientation())) return "Hospitalisation";
        if ("Transfert".equals(c.getOrientation()))       return "Transfert";
        if ("Sortie".equals(c.getOrientation()))          return "Sortie";
        return "En cours";
    }

    @FXML private void onNouvelleConsultation() {
        ouvrirFormulaireConsultation(null);
    }

    /** Ouvre le formulaire de modification pour une consultation trouvée par voix */
    public void ouvrirModifierParVoix(Consultation c) {
        Platform.runLater(() -> ouvrirModifier(c));
    }

    /** Ouvre les ordonnances pour une consultation trouvée par voix */
    public void ouvrirOrdonnancesParVoix(Consultation c) {
        Platform.runLater(() -> ouvrirOrdonnances(c));
    }

    /** Appelé depuis le Dashboard triage — ouvre le formulaire avec l'admission pré-sélectionnée */
    public void ouvrirNouvelleConsultationPourAdmission(int idAdmission) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ResourcesMed/module3/fxml/ConsultationForm.fxml"));
            javafx.scene.Parent content = loader.load();
            ConsultationFormController ctrl = loader.getController();
            ctrl.setOnCloseCallback(this::fermerOverlay);
            ctrl.setAdmissionPreselect(idAdmission);

            content.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 40, 0, 0, 8);" +
                "-fx-max-width: 920px; -fx-max-height: 680px;"
            );
            overlayContainer.getChildren().setAll(content);
            overlayContainer.setUserData((Runnable) this::chargerDonnees);
            overlayBackground.setVisible(true);
            overlayContainer.setVisible(true);
            overlayBackground.setOpacity(0);
            overlayContainer.setOpacity(0);

            javafx.animation.FadeTransition f1 = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(180), overlayBackground);
            f1.setToValue(1); f1.play();
            javafx.animation.FadeTransition f2 = new javafx.animation.FadeTransition(
                javafx.util.Duration.millis(180), overlayContainer);
            f2.setToValue(1); f2.play();

        } catch (java.io.IOException e) {
            System.out.println("[ConsultationList] ouvrirPourAdmission: " + e.getMessage());
        }
    }

    // ══ OVERLAY — ouverture in-place avec fade ══════════════════════════

    private void ouvrirOverlay(String fxmlPath,
                               java.util.function.Consumer<Object> setup,
                               Runnable onClose) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            Object ctrl = loader.getController();

            // Injecter le callback de fermeture dans le contrôleur dialog
            if (ctrl instanceof OrdonnancesDialogController c)
                c.setOnCloseCallback(this::fermerOverlay);
            else if (ctrl instanceof ExamenDialogController c)
                c.setOnCloseCallback(this::fermerOverlay);
            else if (ctrl instanceof ConsultationFormController c)
                c.setOnCloseCallback(this::fermerOverlay);

            if (setup != null) setup.accept(ctrl);

            content.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16px;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 40, 0, 0, 8);" +
                "-fx-max-width: 920px; -fx-max-height: 680px;"
            );

            overlayContainer.getChildren().setAll(content);
            overlayContainer.setUserData(onClose);
            overlayBackground.setVisible(true);
            overlayContainer.setVisible(true);
            overlayBackground.setOpacity(0);
            overlayContainer.setOpacity(0);

            FadeTransition f1 = new FadeTransition(Duration.millis(180), overlayBackground);
            f1.setToValue(1); f1.play();
            FadeTransition f2 = new FadeTransition(Duration.millis(180), overlayContainer);
            f2.setToValue(1); f2.play();

        } catch (IOException e) { warn("Erreur", e.getMessage()); }
    }

    public void fermerOverlay() {
        FadeTransition f1 = new FadeTransition(Duration.millis(140), overlayBackground);
        f1.setToValue(0);
        FadeTransition f2 = new FadeTransition(Duration.millis(140), overlayContainer);
        f2.setToValue(0);
        f2.setOnFinished(e -> {
            overlayBackground.setVisible(false);
            overlayContainer.setVisible(false);
            overlayContainer.getChildren().clear();
            Runnable cb = (Runnable) overlayContainer.getUserData();
            if (cb != null) cb.run();
        });
        f1.play(); f2.play();
    }

    @FXML private void onOverlayClicked() { fermerOverlay(); }

    // ══ OUVERTURE DES DIALOGS ════════════════════════════════════════════

    private void ouvrirVoir(Consultation c) {
        ouvrirOverlay("/ResourcesMed/module3/fxml/ConsultationForm.fxml",
            ctrl -> ((ConsultationFormController) ctrl).setConsultationPourModification(c),
            null);
    }

    private void ouvrirModifier(Consultation c) {
        if (c.getDateHeureFin() != null) {
            warn("Cloturee", "Impossible de modifier une consultation cloturee.");
            return;
        }
        ouvrirOverlay("/ResourcesMed/module3/fxml/ConsultationForm.fxml",
            ctrl -> ((ConsultationFormController) ctrl).setConsultationPourModification(c),
            () -> chargerDonnees());
    }

    private void ouvrirOrdonnances(Consultation c) {
        ouvrirOverlay("/ResourcesMed/module3/fxml/OrdonnancesDialog.fxml",
            ctrl -> ((OrdonnancesDialogController) ctrl).setConsultation(c),
            null);
    }

    private void ouvrirDemandeExamen(Consultation c) {
        ouvrirOverlay("/ResourcesMed/module3/fxml/ExamenDialog.fxml",
            ctrl -> ((ExamenDialogController) ctrl).setConsultation(c),
            () -> chargerDonnees());
    }

    private void cloturer(Consultation c) {
        if (c.getDateHeureFin() != null) {
            warn("Deja cloturee", "Cette consultation est deja cloturee.");
            return;
        }
        cs.cloturerConsultation(c.getIdConsultation());
        
        // Envoi automatique de l'email post-consultation en arriere-plan
        PostConsultationEmailOrchestrator orchestrator = new PostConsultationEmailOrchestrator();
        orchestrator.sendAsync(c.getIdConsultation());
        
        chargerDonnees();
        showToast("Consultation cloturee.", true);
    }

    private void confirmerSuppression(Consultation c) {
        // Dialog compact dans l'overlay — pas de nouvelle fenêtre
        VBox dialog = new VBox(0);
        dialog.setStyle(
            "-fx-background-color: white; -fx-background-radius: 16px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 40, 0, 0, 8);" +
            "-fx-min-width: 360px; -fx-max-width: 380px;" +
            "-fx-min-height: 0; -fx-max-height: 260px;"
        );

        // Header rouge
        VBox header = new VBox(4);
        header.setStyle(
            "-fx-background-color: #FEF2F2; -fx-background-radius: 16px 16px 0 0;" +
            "-fx-padding: 14px 20px 12px 20px;" +
            "-fx-border-color: transparent transparent #FECACA transparent;" +
            "-fx-border-width: 0 0 1px 0;"
        );
        header.setAlignment(Pos.CENTER);
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size: 22px;");
        Label title = new Label("Confirmer la suppression");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #DC2626;");
        header.getChildren().addAll(icon, title);

        // Body
        VBox body = new VBox(6);
        body.setStyle("-fx-padding: 14px 20px 8px 20px;");
        Label msg = new Label("Supprimer la consultation de :");
        msg.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 11px;");
        Label patient = new Label(nvl(c.getNomPatient()));
        patient.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #111827;");
        Label warn = new Label("⚠  Les ordonnances associées seront aussi supprimées.");
        warn.setStyle("-fx-text-fill: #B45309; -fx-font-size: 11px; " +
                      "-fx-background-color: #FFFBEB; -fx-background-radius: 6px; " +
                      "-fx-padding: 5px 10px;");
        warn.setWrapText(true);
        body.getChildren().addAll(msg, patient, warn);

        // Boutons
        HBox btns = new HBox(10);
        btns.setStyle("-fx-padding: 12px 20px 16px 20px;");
        btns.setAlignment(Pos.CENTER_RIGHT);

        Button btnAnnuler = new Button("Annuler");
        btnAnnuler.getStyleClass().add("btn-ghost-navy");
        btnAnnuler.setPrefHeight(36);
        btnAnnuler.setOnAction(e -> fermerOverlay());

        Button btnSuppr = new Button("🗑  Supprimer");
        btnSuppr.getStyleClass().add("btn-ghost-red");
        btnSuppr.setPrefHeight(36);
        btnSuppr.setOnAction(e -> {
            cs.deleteEntity(c);
            fermerOverlay();
            chargerDonnees();
            showToast("Consultation supprimée.", false);
        });

        btns.getChildren().addAll(btnAnnuler, btnSuppr);
        dialog.getChildren().addAll(header, body, btns);

        // Afficher dans l'overlay
        overlayContainer.getChildren().setAll(dialog);
        overlayContainer.setUserData(null);
        overlayBackground.setVisible(true);
        overlayContainer.setVisible(true);
        overlayBackground.setOpacity(0);
        overlayContainer.setOpacity(0);
        FadeTransition f1 = new FadeTransition(Duration.millis(180), overlayBackground);
        f1.setToValue(1); f1.play();
        FadeTransition f2 = new FadeTransition(Duration.millis(180), overlayContainer);
        f2.setToValue(1); f2.play();
    }

    private void showToast(String message, boolean success) {
        Stage owner = (Stage) cardsContainer.getScene().getWindow();
        Popup popup = new Popup();
        popup.setAutoFix(true);
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(14, 20, 14, 16));
        box.setMinWidth(300);
        box.getStyleClass().add(success ? "toast-success" : "toast-error");
        Label lbl = new Label(message);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: "
                + (success ? "#15803d;" : "#dc2626;"));
        lbl.setWrapText(true);
        box.getChildren().add(lbl);
        popup.getContent().add(box);
        popup.show(owner);
        popup.setX(owner.getX() + owner.getWidth() - 340);
        popup.setY(owner.getY() + owner.getHeight() - 80);
        box.setTranslateY(20);
        box.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(220), box);
        slide.setToY(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), box);
        fadeIn.setToValue(1);
        slide.play();
        fadeIn.play();
        PauseTransition pause = new PauseTransition(Duration.seconds(2.5));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), box);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> popup.hide());
        new SequentialTransition(pause, fadeOut).play();
    }

    @FXML private void onActualiser() {
        searchField.clear();
        filterCombo.setValue("Tous");
        chargerDonnees();
    }

    @FXML private void onFiltrer() { appliquerFiltres(); }

    @FXML private void onDashboard() {
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/dashboardMed.fxml");
            if (fxmlUrl == null) return;
            Parent newRoot = FXMLLoader.load(fxmlUrl);
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            Scene newScene = creerScene(newRoot);
            stage.setScene(newScene);
            if (!stage.isMaximized()) stage.setMaximized(true);
        } catch (IOException e) { warn("Navigation", e.getMessage()); }
    }

    @FXML private void onStatistiques() {
        try {
            URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/Statistiques.fxml");
            if (fxmlUrl == null) return;
            Parent newRoot = FXMLLoader.load(fxmlUrl);
            Stage stage = (Stage) cardsContainer.getScene().getWindow();
            Scene newScene = creerScene(newRoot);
            stage.setScene(newScene);
            if (!stage.isMaximized()) stage.setMaximized(true);
        } catch (IOException e) { warn("Navigation", e.getMessage()); }
    }
    private void ouvrirFormulaireConsultation(Consultation consultation) {
        ouvrirOverlay("/ResourcesMed/module3/fxml/ConsultationForm.fxml",
            ctrl -> { if (consultation != null)
                ((ConsultationFormController) ctrl).setConsultationPourModification(consultation); },
            () -> {
                chargerDonnees();
                showToast(consultation == null ? "Consultation creee." : "Consultation modifiee.", true);
            });
    }

    private Stage creerStageModal(String titre) {
        Stage stage = new Stage();
        stage.setTitle(titre);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(cardsContainer.getScene().getWindow());
        stage.setResizable(true);
        return stage;
    }

    private Scene creerScene(Parent root) {
        double screenW = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
        double screenH = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
        Scene scene = new Scene(root, screenW, screenH);
        URL cssUrl = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        return scene;
    }

    private String nvl(String s) { return (s != null && !s.isBlank()) ? s : "—"; }

    private void warn(String t, String m) {
        javafx.scene.control.Alert a = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.WARNING);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        URL cssUrl = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
        if (cssUrl != null) try {
            a.getDialogPane().getStylesheets().add(cssUrl.toExternalForm());
        } catch (Exception ignored) {}
        a.showAndWait();
    }
}

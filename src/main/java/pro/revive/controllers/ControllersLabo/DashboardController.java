package pro.revive.controllers.ControllersLabo;

import pro.revive.Navigator;
import pro.revive.entities.EntitiesLabo.ConsultationNotif;
import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.services.ServicesLabo.Examens_demandesService;
import pro.revive.services.ServicesLabo.ResultatService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.fxml.Initializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private Label    lblDate;
    @FXML private Label    lblExamensJour;
    @FXML private Label    lblResultatsTotal;
    @FXML private Label    lblUrgentsAttente;
    @FXML private Label    lblResultatsCritiques;
    @FXML private PieChart pieResultats;
    @FXML private Label    lblPctPropre;
    @FXML private Label    lblPctCritique;
    @FXML private VBox     containerNotifsMedecins;
    @FXML private Label    lblBadgeNotifs;
    @FXML private Label    lblUserName;

    private final Examens_demandesService examenService  = new Examens_demandesService();
    private final ResultatService         resultatService = new ResultatService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE dd MMMM yyyy", java.util.Locale.FRENCH);
        lblDate.setText(sdf.format(new Date()));
        chargerStatistiques();
        chargerNotificationsMedecins();
        lblUserName.setText(Navigator.currentUserName);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATISTIQUES — uniquement les données du jour
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerStatistiques() {
        List<Examens_demandes> tousExamens   = examenService.getAllExamens();
        List<Resultats>        tousResultats = resultatService.afficher();

        String aujourdhui = new SimpleDateFormat("yyyy-MM-dd").format(new Date());

        // ✅ Examens ajoutés aujourd'hui uniquement
        long examensJour = tousExamens.stream()
                .filter(e -> e.getDateDemande() != null &&
                        new SimpleDateFormat("yyyy-MM-dd").format(e.getDateDemande()).equals(aujourdhui))
                .count();

        // ✅ Résultats ajoutés aujourd'hui uniquement
        List<Resultats> resultatsJour = tousResultats.stream()
                .filter(r -> r.getDateResultat() != null &&
                        new SimpleDateFormat("yyyy-MM-dd").format(r.getDateResultat()).equals(aujourdhui))
                .collect(Collectors.toList());

        // ✅ Cas urgents en attente ajoutés aujourd'hui uniquement
        long urgentsAttente = tousExamens.stream()
                .filter(e -> e.getDateDemande() != null &&
                        new SimpleDateFormat("yyyy-MM-dd").format(e.getDateDemande()).equals(aujourdhui) &&
                        e.isUrgent() &&
                        "En attente".equalsIgnoreCase(e.getStatut()))
                .count();

        // ✅ Résultats graves d'aujourd'hui uniquement
        long critiques = resultatsJour.stream()
                .filter(r -> "Grave".equalsIgnoreCase(r.getEtat()))
                .count();

        // Animations de comptage
        animerCompteur(lblExamensJour,        (int) examensJour,           800);
        animerCompteur(lblResultatsTotal,     (int) resultatsJour.size(),  1000);
        animerCompteur(lblUrgentsAttente,     (int) urgentsAttente,        900);
        animerCompteur(lblResultatsCritiques, (int) critiques,             1100);

        // ✅ Graphe basé sur les résultats du jour uniquement
        chargerGraphe(resultatsJour);
    }

    /**
     * Anime un Label de 0 jusqu'à targetValue en durationMs millisecondes.
     */
    private void animerCompteur(Label label, int targetValue, int durationMs) {
        if (targetValue == 0) {
            label.setText("0");
            return;
        }
        int steps      = Math.min(targetValue, 40);
        int stepValue  = Math.max(1, targetValue / steps);
        int intervalMs = durationMs / steps;

        final int[] current = {0};
        Timeline timeline = new Timeline();
        timeline.setCycleCount(steps);
        timeline.getKeyFrames().add(new KeyFrame(Duration.millis(intervalMs), e -> {
            current[0] = Math.min(current[0] + stepValue, targetValue);
            label.setText(String.valueOf(current[0]));
        }));
        timeline.setOnFinished(e -> label.setText(String.valueOf(targetValue)));
        timeline.play();
    }

    private void chargerGraphe(List<Resultats> resultats) {
        pieResultats.getData().clear();

        long propre = resultats.stream().filter(r -> "Propre".equalsIgnoreCase(r.getEtat())).count();
        long grave  = resultats.stream().filter(r -> "Grave".equalsIgnoreCase(r.getEtat())).count();
        long total  = propre + grave;

        if (total == 0) {
            pieResultats.getData().add(new PieChart.Data("Aucun résultat aujourd'hui", 1));
            lblPctPropre.setText("0%");
            lblPctCritique.setText("0%");
            return;
        }

        double pctPropre   = (propre * 100.0) / total;
        double pctCritique = (grave  * 100.0) / total;

        PieChart.Data dataPropre   = new PieChart.Data(String.format("Propre (%.0f%%)",   pctPropre),   propre);
        PieChart.Data dataCritique = new PieChart.Data(String.format("Critique (%.0f%%)", pctCritique), grave);

        pieResultats.getData().addAll(dataPropre, dataCritique);

        // Appliquer les couleurs après que les nœuds soient créés
        javafx.application.Platform.runLater(() -> {
            if (dataPropre.getNode() != null)
                dataPropre.getNode().setStyle("-fx-pie-color: #16A34A;");
            if (dataCritique.getNode() != null)
                dataCritique.getNode().setStyle("-fx-pie-color: #DC2626;");
        });

        // Animation fade-in du graphe
        pieResultats.setOpacity(0);
        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.ZERO,        e2 -> pieResultats.setOpacity(0)),
                new KeyFrame(Duration.millis(800), e2 -> pieResultats.setOpacity(1))
        );
        fadeIn.play();

        // Animation des pourcentages
        animerPourcentage(lblPctPropre,   pctPropre,   900);
        animerPourcentage(lblPctCritique, pctCritique, 1100);
    }

    private void animerPourcentage(Label label, double targetPct, int durationMs) {
        int steps      = 30;
        int intervalMs = durationMs / steps;
        final double[] current = {0};
        Timeline tl = new Timeline();
        tl.setCycleCount(steps);
        tl.getKeyFrames().add(new KeyFrame(Duration.millis(intervalMs), e -> {
            current[0] = Math.min(current[0] + targetPct / steps, targetPct);
            label.setText(String.format("%.0f%%", current[0]));
        }));
        tl.setOnFinished(e -> label.setText(String.format("%.0f%%", targetPct)));
        tl.play();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTIFICATIONS MÉDECINS — consultations statut_demande = 'Envoyée'
    // ─────────────────────────────────────────────────────────────────────────

    private void chargerNotificationsMedecins() {
        containerNotifsMedecins.getChildren().clear();

        List<ConsultationNotif> notifs = examenService.getConsultationsEnvoyees();

        if (lblBadgeNotifs != null) {
            lblBadgeNotifs.setText(String.valueOf(notifs.size()));
            lblBadgeNotifs.setVisible(!notifs.isEmpty());
        }

        if (notifs.isEmpty()) {
            VBox vide = new VBox(6);
            vide.setStyle("-fx-alignment: CENTER; -fx-padding: 20;");
            Label icon = new Label("✅");
            icon.setStyle("-fx-font-size: 24px;");
            Label msg = new Label("Aucune demande en attente.");
            msg.setStyle("-fx-text-fill: #16A34A; -fx-font-size: 13px; -fx-font-weight: bold;");
            Label sub = new Label("Toutes les demandes ont été traitées.");
            sub.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 11px;");
            vide.getChildren().addAll(icon, msg, sub);
            containerNotifsMedecins.getChildren().add(vide);
            return;
        }

        for (ConsultationNotif notif : notifs) {
            containerNotifsMedecins.getChildren().add(creerCarteNotif(notif));
        }
    }

    private VBox creerCarteNotif(ConsultationNotif notif) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-background-color: #F0F7FF;" +
                        "-fx-background-radius: 12;" +
                        "-fx-padding: 12 14;" +
                        "-fx-border-color: #BFDBFE;" +
                        "-fx-border-radius: 12;" +
                        "-fx-border-width: 1;"
        );

        // ── Ligne 1 : icône + nom patient
        HBox ligne1 = new HBox(8);
        ligne1.setStyle("-fx-alignment: CENTER_LEFT;");
        Label iconLbl = new Label("👨‍⚕️");
        iconLbl.setStyle("-fx-font-size: 16px;");
        Label nomLbl = new Label(notif.getNomPatient());
        nomLbl.setStyle("-fx-text-fill: #0B4EA2; -fx-font-size: 13px; -fx-font-weight: bold;");
        ligne1.getChildren().addAll(iconLbl, nomLbl);

        // ── Ligne 2 : Analyses
        HBox ligneAnalyses = new HBox(6);
        ligneAnalyses.setStyle("-fx-alignment: CENTER_LEFT;");
        Label labelA = new Label("🧪  Analyses :");
        labelA.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 90;");
        Label valA = new Label(notif.getAnalyses() != null && !notif.getAnalyses().isBlank()
                ? notif.getAnalyses() : "—");
        valA.setWrapText(true);
        valA.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px;");
        ligneAnalyses.getChildren().addAll(labelA, valA);

        // ── Ligne 3 : Imageries
        HBox ligneImageries = new HBox(6);
        ligneImageries.setStyle("-fx-alignment: CENTER_LEFT;");
        Label labelI = new Label("🩻  Imageries :");
        labelI.setStyle("-fx-text-fill: #475569; -fx-font-size: 11px; -fx-font-weight: bold; -fx-min-width: 90;");
        Label valI = new Label(notif.getImageries() != null && !notif.getImageries().isBlank()
                ? notif.getImageries() : "—");
        valI.setWrapText(true);
        valI.setStyle("-fx-text-fill: #334155; -fx-font-size: 11px;");
        ligneImageries.getChildren().addAll(labelI, valI);

        // ── Ligne 4 : Date + bouton "Marquer reçu"
        HBox ligne4 = new HBox(10);
        ligne4.setStyle("-fx-alignment: CENTER_LEFT;");

        String dateStr = notif.getDateDemande() != null
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(notif.getDateDemande())
                : "—";
        Label dateLbl = new Label("📅  " + dateStr);
        dateLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 10px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button btnCheck = new Button("✔  Reçu");
        btnCheck.setStyle(
                "-fx-background-color: #0B4EA2; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-padding: 5 14; -fx-font-size: 11px; -fx-cursor: hand;"
        );

        btnCheck.setOnAction(e -> {
            examenService.marquerConsultationRecue(notif.getIdConsultation());
            card.setStyle(
                    "-fx-background-color: #F1F5F9; -fx-background-radius: 12; -fx-padding: 12 14;" +
                            "-fx-border-color: #E2E8F0; -fx-border-radius: 12; -fx-border-width: 1; -fx-opacity: 0.5;"
            );
            btnCheck.setDisable(true);
            btnCheck.setText("✔  Reçu");

            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.millis(600));
            pause.setOnFinished(ev -> {
                containerNotifsMedecins.getChildren().remove(card);
                int restant = containerNotifsMedecins.getChildren().size();
                if (lblBadgeNotifs != null) {
                    lblBadgeNotifs.setText(String.valueOf(restant));
                    lblBadgeNotifs.setVisible(restant > 0);
                }
                if (restant == 0) {
                    chargerNotificationsMedecins();
                }
            });
            pause.play();
        });

        btnCheck.setOnMouseEntered(e -> btnCheck.setStyle(
                "-fx-background-color: #0E9B8A; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-padding: 5 14; -fx-font-size: 11px; -fx-cursor: hand;"
        ));
        btnCheck.setOnMouseExited(e -> btnCheck.setStyle(
                "-fx-background-color: #0B4EA2; -fx-text-fill: white;" +
                        "-fx-font-weight: bold; -fx-background-radius: 8;" +
                        "-fx-padding: 5 14; -fx-font-size: 11px; -fx-cursor: hand;"
        ));

        ligne4.getChildren().addAll(dateLbl, spacer, btnCheck);
        card.getChildren().addAll(ligne1, ligneAnalyses, ligneImageries, ligne4);

        VBox wrapper = new VBox(0);
        wrapper.getChildren().add(card);
        Region sep = new Region();
        sep.setPrefHeight(8);
        wrapper.getChildren().add(sep);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BOUTON ACTUALISER
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void actualiserNotifs() { chargerNotificationsMedecins(); }

    @FXML
    private void deconnexion() {
        System.out.println("Déconnexion...");
        Navigator.logout();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EFFETS HOVER
    // ─────────────────────────────────────────────────────────────────────────

    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setScaleX(1.15);
            btn.setScaleY(1.15);
        }
    }

    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        }
    }

    public void onSidebarHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white;" +
                            "-fx-font-size: 13px; -fx-padding: 13 30; -fx-alignment: CENTER_LEFT;" +
                            "-fx-cursor: hand; -fx-background-radius: 10;" +
                            "-fx-border-color: rgba(255,255,255,0.25); -fx-border-radius: 10; -fx-border-width: 1;"
            );
            btn.setTranslateX(4);
        }
    }

    public void onSidebarHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof javafx.scene.control.Button btn) {
            btn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.72);" +
                            "-fx-font-size: 13px; -fx-padding: 13 30; -fx-alignment: CENTER_LEFT;" +
                            "-fx-cursor: hand; -fx-background-radius: 10;"
            );
            btn.setTranslateX(0);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVIGATION SIDEBAR
    // ─────────────────────────────────────────────────────────────────────────

    @FXML private void showDashboard() {
        chargerStatistiques();
        chargerNotificationsMedecins();
    }

    @FXML private void showExamens()   { naviguerVers("/ResourcesLabo/GestionExamens.fxml"); }
    @FXML private void showResultats() { naviguerVers("/ResourcesLabo/GestionResultats.fxml"); }
    @FXML private void showRapport()   { naviguerVers("/ResourcesLabo/RapportBiologiste.fxml"); }

    private void naviguerVers(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) lblDate.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
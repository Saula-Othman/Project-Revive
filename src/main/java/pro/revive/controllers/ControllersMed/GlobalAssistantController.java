package pro.revive.controllers.ControllersMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.services.ServicesMed.ConsultationService;
import pro.revive.services.ServicesMed.SpeechToTextService;
import pro.revive.services.ServicesMed.VoiceCommandService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

/**
 * Assistant vocal global — bouton flottant ★ en bas à droite.
 * Peut être injecté dans n'importe quel StackPane (Dashboard, ConsultationList, Statistiques).
 *
 * Utilisation :
 *   GlobalAssistantController assistant = new GlobalAssistantController(rootStackPane);
 *   assistant.setNavigationCallback(fxml -> naviguer(fxml));
 */
public class GlobalAssistantController {

    private final StackPane          root;
    private final SpeechToTextService speechService       = new SpeechToTextService();
    private final VoiceCommandService voiceCommandService = new VoiceCommandService();
    private final ConsultationService consultationService = new ConsultationService();

    private boolean  isActive   = false;
    private boolean  panelOpen  = false;

    // UI éléments
    private Button   btnFloat;
    private javafx.stage.Popup popup;   // Popup au lieu de VBox dans StackPane
    private VBox     panel;
    private Label    lblStatus;
    private Label    lblTranscript;
    private ComboBox<String> cbLang;

    // Callbacks navigation
    private java.util.function.Consumer<String> onNavigate;

    // ── Constructeur ──────────────────────────────────────────────────────

    public GlobalAssistantController(StackPane root) {
        this.root = root;
        construireUI();
    }

    public void setNavigationCallback(java.util.function.Consumer<String> cb) {
        this.onNavigate = cb;
    }

    /** Navigue vers un FXML en récupérant le Stage depuis le bouton flottant */
    private void naviguerVers(String fxml) {
        try {
            if (btnFloat == null || btnFloat.getScene() == null) return;
            javafx.stage.Stage stage = (javafx.stage.Stage) btnFloat.getScene().getWindow();
            if (stage == null) return;

            java.net.URL fxmlUrl = getClass().getResource("/ResourcesMed/module3/fxml/" + fxml);
            if (fxmlUrl == null) return;
            javafx.scene.Parent newRoot = javafx.fxml.FXMLLoader.load(fxmlUrl);
            double w = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double h = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            javafx.scene.Scene scene = new javafx.scene.Scene(newRoot, w, h);
            java.net.URL css = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            if (!stage.isMaximized()) stage.setMaximized(true);
        } catch (Exception e) {
            System.out.println("[Assistant] Navigation erreur : " + e.getMessage());
        }
    }

    // ── Construction UI ───────────────────────────────────────────────────

    private void construireUI() {
        // ── Bouton flottant ───────────────────────────────────────────────
        btnFloat = new Button("★");
        btnFloat.setStyle(styleBtnNormal());
        btnFloat.setUserData("assistant");
        btnFloat.setTooltip(new Tooltip("Assistant vocal"));
        btnFloat.setOnAction(e -> toggleAssistant());

        StackPane.setAlignment(btnFloat, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(btnFloat, new Insets(0, 24, 24, 0));
        root.getChildren().add(btnFloat);

        // ── Popup — taille exacte, pas d'étirement ────────────────────────
        popup = new javafx.stage.Popup();
        popup.setAutoFix(true);
        popup.setAutoHide(false);

        panel = new VBox(6);
        panel.setPrefWidth(210);
        panel.setMaxWidth(210);
        panel.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 12px;" +
            "-fx-border-width: 1px;" +
            "-fx-padding: 10px 14px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20), 14, 0, 0, 4);"
        );

        // Langue
        cbLang = new ComboBox<>();
        cbLang.getItems().addAll("fr-FR", "ar-TN");
        cbLang.setValue("fr-FR");
        cbLang.setPrefWidth(90);
        cbLang.setStyle("-fx-font-size: 11px;");

        HBox langRow = new HBox(8);
        langRow.setAlignment(Pos.CENTER_LEFT);
        Label lblL = new Label("Langue :");
        lblL.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748B;");
        langRow.getChildren().addAll(lblL, cbLang);

        // Statut
        lblStatus = new Label("Prêt — cliquez sur ★");
        lblStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #7C3AED; -fx-font-weight: bold;");
        lblStatus.setWrapText(true);
        lblStatus.setMaxWidth(190);

        // Transcript
        lblTranscript = new Label("");
        lblTranscript.setStyle(
            "-fx-font-size: 10px; -fx-text-fill: #475569;" +
            "-fx-background-color: #F8FAFF;" +
            "-fx-background-radius: 6px; -fx-padding: 3px 6px;");
        lblTranscript.setMaxWidth(190);
        lblTranscript.setWrapText(false);

        panel.getChildren().addAll(langRow, lblStatus, lblTranscript);
        popup.getContent().add(panel);
    }

    // ── Toggle assistant (1 clic = démarrer, 2e clic = arrêter) ──────────

    private void toggleAssistant() {
        if (isActive) {
            arreterAssistant();
        } else {
            demarrerAssistant();
        }
    }

    // ── Assistant vocal ───────────────────────────────────────────────────

    private void demarrerAssistant() {
        if (isActive) return;
        isActive = true;
        String langue = cbLang != null && cbLang.getValue() != null ? cbLang.getValue() : "fr-FR";

        // Afficher le popup juste au-dessus du bouton ★
        panel.setVisible(true);
        panel.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), panel);
        ft.setToValue(1);
        ft.play();

        // Positionner le popup au-dessus du bouton
        if (btnFloat.getScene() != null) {
            javafx.geometry.Bounds b = btnFloat.localToScreen(btnFloat.getBoundsInLocal());
            if (b != null) {
                popup.show(btnFloat.getScene().getWindow(),
                    b.getMaxX() - 215,
                    b.getMinY() - 110);
            }
        }

        btnFloat.setStyle(styleBtnEcoute());
        btnFloat.setText("■");
        btnFloat.setTooltip(new Tooltip("Cliquez pour arrêter l'assistant"));
        setStatus("Écoute en cours (" + langue + ")...", "white");

        speechService.startListening(langue,
            texte -> {
                if (!isActive) return;
                Platform.runLater(() -> {
                    if (lblTranscript != null) {
                        // Tronquer si trop long
                        String t = texte.trim();
                        lblTranscript.setText(t.length() > 30 ? t.substring(0, 30) + "…" : t);
                    }
                    setStatus("Analyse...", "rgba(255,255,255,0.80)");
                });

                Thread t = new Thread(() -> {
                    String lower = texte.toLowerCase().trim();

                    // ── Ignorer les phrases trop longues (bruit) ──────────
                    if (lower.split("\\s+").length > 8) return;

                    // ── Navigation stricte ────────────────────────────────
                    if (lower.equals("consultations") || lower.equals("consultation")
                            || lower.equals("aller aux consultations")
                            || lower.equals("ouvrir consultations")
                            || lower.equals("nouvelle consultation")
                            || lower.equals("nouvelles consultations")) {
                        Platform.runLater(() -> {
                            setStatus("Navigation → Consultations", "white");
                            naviguerVers("ConsultationList.fxml");
                        });
                        return;
                    }
                    if (lower.equals("tableau de bord") || lower.equals("tableaux de bord")
                            || lower.equals("dashboard") || lower.equals("accueil")
                            || lower.equals("aller au tableau de bord")) {
                        Platform.runLater(() -> {
                            setStatus("Navigation → Tableau de Bord", "white");
                            naviguerVers("dashboardMed.fxml");
                        });
                        return;
                    }
                    if (lower.equals("statistiques") || lower.equals("statistique")
                            || lower.equals("aller aux statistiques")) {
                        Platform.runLater(() -> {
                            setStatus("Navigation → Statistiques", "white");
                            naviguerVers("Statistiques.fxml");
                        });
                        return;
                    }

                    // ── Recherche patient par nom ─────────────────────────
                    // Détecter si c'est une commande patient
                    boolean ouvrirOrdonnance = lower.contains("ordonnance") || lower.contains("ordonnances");
                    boolean ouvrirConsult    = lower.contains("consultation") || lower.contains("dossier")
                                           || lower.contains("ouvrir") || lower.contains("voir");

                    // Extraire le nom du patient depuis la phrase
                    String nomPatient = extraireNomPatient(lower);

                    if (!nomPatient.isEmpty()) {
                        // Chercher dans les consultations
                        List<Consultation> toutes = consultationService.getData();
                        Consultation trouvee = toutes.stream()
                            .filter(c -> c.getNomPatient() != null &&
                                normaliser(c.getNomPatient()).contains(normaliser(nomPatient)))
                            .findFirst()
                            .orElse(null);

                        if (trouvee != null) {
                            final Consultation c = trouvee;
                            if (ouvrirOrdonnance) {
                                // Naviguer vers consultations et ouvrir les ordonnances
                                Platform.runLater(() -> {
                                    setStatus("Ordonnances de " + c.getNomPatient(), "white");
                                    naviguerEtOuvrirOrdonnances(c);
                                });
                            } else {
                                // Naviguer vers consultations et ouvrir le dossier
                                Platform.runLater(() -> {
                                    setStatus("Consultation de " + c.getNomPatient(), "white");
                                    naviguerEtOuvrirConsultation(c);
                                });
                            }
                            return;
                        } else {
                            Platform.runLater(() ->
                                setStatus("Patient introuvable : " + nomPatient, "rgba(255,200,100,1)"));
                            return;
                        }
                    }

                    // ── Commandes IA pour les autres cas ──────────────────
                    VoiceCommandService.Command cmd = voiceCommandService.parseCommand(texte);
                    Platform.runLater(() -> {
                        String feedback = switch (cmd.action) {
                            case "inconnu" -> "Non compris : " + texte;
                            default        -> cmd.action + " : " + cmd.getParam("valeur");
                        };
                        setStatus(feedback, "rgba(255,255,255,0.80)");
                    });
                }, "global-cmd-thread");
                t.setDaemon(true);
                t.start();
            },
            err -> {
                isActive = false;
                Platform.runLater(() -> {
                    btnFloat.setText("★");
                    btnFloat.setStyle(styleBtnNormal());
                    btnFloat.setTooltip(new Tooltip("Assistant vocal — cliquez pour démarrer"));
                    cacherPanel();
                });
            }
        );
    }

    private void arreterAssistant() {
        isActive = false;
        speechService.stopListening();
        btnFloat.setText("★");
        btnFloat.setStyle(styleBtnNormal());
        btnFloat.setTooltip(new Tooltip("Assistant vocal — cliquez pour démarrer"));
        cacherPanel();
    }

    private void cacherPanel() {
        FadeTransition ft = new FadeTransition(Duration.millis(180), panel);
        ft.setToValue(0);
        ft.setOnFinished(e -> {
            panel.setVisible(false);
            popup.hide();
        });
        ft.play();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────

    private void setStatus(String msg, String color) {
        if (lblStatus != null) {
            lblStatus.setText(msg);
            // Thème blanc — adapter les couleurs
            String c = switch (color) {
                case "white"                    -> "#7C3AED";
                case "rgba(255,255,255,0.80)"   -> "#64748B";
                case "rgba(255,200,100,1)"      -> "#D35400";
                default -> color.startsWith("rgba") ? "#7C3AED" : color;
            };
            lblStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: " + c + "; -fx-font-weight: bold;");
        }
    }

    private String styleBtnNormal() {
        return "-fx-background-color: #7C3AED; -fx-text-fill: white;" +
               "-fx-font-size: 18px; -fx-font-weight: bold;" +
               "-fx-background-radius: 50%; -fx-border-radius: 50%;" +
               "-fx-min-width: 52px; -fx-min-height: 52px;" +
               "-fx-max-width: 52px; -fx-max-height: 52px;" +
               "-fx-cursor: hand;" +
               "-fx-effect: dropshadow(gaussian, rgba(124,58,237,0.45), 14, 0, 0, 4);";
    }

    private String styleBtnActif() {
        return "-fx-background-color: #5B21B6; -fx-text-fill: white;" +
               "-fx-font-size: 18px; -fx-font-weight: bold;" +
               "-fx-background-radius: 50%; -fx-border-radius: 50%;" +
               "-fx-min-width: 52px; -fx-min-height: 52px;" +
               "-fx-max-width: 52px; -fx-max-height: 52px;" +
               "-fx-cursor: hand;" +
               "-fx-effect: dropshadow(gaussian, rgba(91,33,182,0.55), 18, 0, 0, 5);";
    }

    private String styleBtnEcoute() {
        return "-fx-background-color: #C0392B; -fx-text-fill: white;" +
               "-fx-font-size: 18px; -fx-font-weight: bold;" +
               "-fx-background-radius: 50%; -fx-border-radius: 50%;" +
               "-fx-min-width: 52px; -fx-min-height: 52px;" +
               "-fx-max-width: 52px; -fx-max-height: 52px;" +
               "-fx-cursor: hand;" +
               "-fx-effect: dropshadow(gaussian, rgba(192,57,43,0.55), 18, 0, 0, 5);";
    }

    // ── Recherche patient ─────────────────────────────────────────────────

    /**
     * Extrait un nom de patient depuis une phrase vocale.
     * Supprime les mots-clés de commande pour isoler le nom.
     */
    private String extraireNomPatient(String lower) {
        // Supprimer les mots-clés de commande
        String[] motsCles = {
            "ouvrir l ordonnance de", "ouvrir les ordonnances de",
            "ordonnance de", "ordonnances de",
            "ouvrir la consultation de", "ouvrir le dossier de",
            "consultation de", "dossier de",
            "voir le dossier de", "voir la consultation de",
            "ouvrir", "voir", "afficher", "chercher",
            "le patient", "la patiente", "patient", "patiente"
        };

        String texte = lower.trim();
        for (String mot : motsCles) {
            if (texte.startsWith(mot)) {
                texte = texte.substring(mot.length()).trim();
            }
            texte = texte.replace(mot, " ").trim();
        }

        // Nettoyer les articles résiduels
        texte = texte.replaceAll("^(de |du |le |la |les |l'|l )", "").trim();
        texte = texte.replaceAll("\\s+", " ").trim();

        // Valider : doit avoir au moins 2 caractères et ressembler à un nom
        if (texte.length() < 2) return "";

        // Ignorer les mots de navigation
        String[] motsNav = {"tableau", "bord", "statistique", "consultation", "ordonnance",
                            "dashboard", "accueil", "navigation", "aller", "ouvrir"};
        for (String m : motsNav) {
            if (texte.equals(m)) return "";
        }

        return texte;
    }

    /**
     * Normalise un nom pour la comparaison (minuscules, sans accents).
     */
    private String normaliser(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD)
            .replaceAll("[^\\p{ASCII}]", "")
            .replaceAll("\\s+", " ")
            .trim();
    }

    /**
     * Navigue vers ConsultationList et ouvre le formulaire de la consultation.
     */
    private void naviguerEtOuvrirConsultation(Consultation c) {
        try {
            if (btnFloat == null || btnFloat.getScene() == null) return;
            javafx.stage.Stage stage = (javafx.stage.Stage) btnFloat.getScene().getWindow();
            if (stage == null) return;

            java.net.URL fxmlUrl = getClass().getResource(
                "/ResourcesMed/module3/fxml/ConsultationList.fxml");
            if (fxmlUrl == null) return;

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent newRoot = loader.load();
            ConsultationListController ctrl = loader.getController();

            double w = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double h = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            javafx.scene.Scene scene = new javafx.scene.Scene(newRoot, w, h);
            java.net.URL css = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            if (!stage.isMaximized()) stage.setMaximized(true);

            // Ouvrir le formulaire de modification après rendu
            Platform.runLater(() -> ctrl.ouvrirModifierParVoix(c));

        } catch (Exception e) {
            System.out.println("[Assistant] Erreur ouverture consultation : " + e.getMessage());
        }
    }

    /**
     * Navigue vers ConsultationList et ouvre les ordonnances du patient.
     */
    private void naviguerEtOuvrirOrdonnances(Consultation c) {
        try {
            if (btnFloat == null || btnFloat.getScene() == null) return;
            javafx.stage.Stage stage = (javafx.stage.Stage) btnFloat.getScene().getWindow();
            if (stage == null) return;

            java.net.URL fxmlUrl = getClass().getResource(
                "/ResourcesMed/module3/fxml/ConsultationList.fxml");
            if (fxmlUrl == null) return;

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent newRoot = loader.load();
            ConsultationListController ctrl = loader.getController();

            double w = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
            double h = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
            javafx.scene.Scene scene = new javafx.scene.Scene(newRoot, w, h);
            java.net.URL css = getClass().getResource("/ResourcesMed/module3/css/revive-dark.css");
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
            stage.setScene(scene);
            if (!stage.isMaximized()) stage.setMaximized(true);

            // Ouvrir les ordonnances après rendu
            Platform.runLater(() -> ctrl.ouvrirOrdonnancesParVoix(c));

        } catch (Exception e) {
            System.out.println("[Assistant] Erreur ouverture ordonnances : " + e.getMessage());
        }
    }

    private String styleBtnStart() {
        return "-fx-background-color: #7C3AED; -fx-text-fill: white;" +
               "-fx-font-size: 12px; -fx-font-weight: bold;" +
               "-fx-background-radius: 8px; -fx-padding: 8px 18px; -fx-cursor: hand;";
    }

    private String styleBtnStop() {
        return "-fx-background-color: #C0392B; -fx-text-fill: white;" +
               "-fx-font-size: 12px; -fx-font-weight: bold;" +
               "-fx-background-radius: 8px; -fx-padding: 8px 18px; -fx-cursor: hand;";
    }
}

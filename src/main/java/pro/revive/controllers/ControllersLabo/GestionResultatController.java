package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.services.ServicesLabo.GraviteService;
import pro.revive.services.ServicesLabo.ResultatService;
import pro.revive.utils.UtilsLabo.PdfGenerator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class GestionResultatController {

    @FXML private VBox   containerResultats;
    @FXML private Label  lblMessage;
    @FXML private TextField tfRecherche;

    // ── Boutons de filtre
    @FXML private Button btnFiltreAll;
    @FXML private Button btnFiltreGrave;
    @FXML private Button btnFiltrePropre;

    private final ResultatService service = new ResultatService();

    private static Resultats resultatSelectionne;
    private List<Resultats>  tousLesResultats;
    private String            filtreActif = "Tous";
    private String            termeRecherche = "";

    // ── Sélection multiple
    private final java.util.Set<Integer> idsSelectionnes = new java.util.HashSet<>();

    public static Resultats getResultatSelectionne() {
        return resultatSelectionne;
    }

    @FXML
    public void initialize() {
        chargerResultats();

        // Recherche en temps réel
        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                termeRecherche = newVal != null ? newVal.trim().toLowerCase() : "";
                appliquerFiltreEtRecherche();
            });
        }
    }

    // ── Charge depuis la BD
    private void chargerResultats() {
        tousLesResultats = service.afficher();
        idsSelectionnes.clear(); // réinitialiser la sélection
        mettreAJourBoutonSuppression();
        // Tri : plus récent en premier (date décroissante)
        tousLesResultats.sort((a, b) -> {
            if (a.getDateResultat() != null && b.getDateResultat() != null)
                return b.getDateResultat().compareTo(a.getDateResultat());
            if (a.getDateResultat() != null) return -1;
            if (b.getDateResultat() != null) return  1;
            return 0;
        });
        appliquerFiltreEtRecherche();
    }

    /** Met à jour le texte du bouton supprimer selon la sélection */
    private void mettreAJourBoutonSuppression() {
        // Chercher le bouton "Tout sélectionner / Supprimer sélection" dans le FXML
        // On utilise lblMessage pour afficher le compteur
        if (lblMessage != null) {
            if (!idsSelectionnes.isEmpty()) {
                lblMessage.setStyle("-fx-text-fill: #DC2626; -fx-font-weight: bold; -fx-font-size: 12px;");
                lblMessage.setText(idsSelectionnes.size() + " sélectionné(s)");
            } else {
                lblMessage.setStyle("-fx-text-fill: #0B4EA2; -fx-font-weight: bold; -fx-font-size: 12px;");
                lblMessage.setText("");
            }
        }
    }

    /** Supprime tous les résultats sélectionnés */
    @FXML
    private void supprimerSelection() {
        if (idsSelectionnes.isEmpty()) {
            afficherMessage("⚠  Sélectionnez au moins un résultat à supprimer.");
            return;
        }

        // Confirmation
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer " + idsSelectionnes.size() + " résultat(s) sélectionné(s) ?\n" +
                "Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    service.supprimerResultats(new java.util.ArrayList<>(idsSelectionnes));
                    int nb = idsSelectionnes.size();
                    idsSelectionnes.clear();
                    resultatSelectionne = null;
                    chargerResultats();
                    lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    lblMessage.setText("✅  " + nb + " résultat(s) supprimé(s).");
                } catch (Exception e) {
                    afficherMessage("❌  Erreur lors de la suppression : " + e.getMessage());
                }
            }
        });
    }

    /** Sélectionne / désélectionne tous les résultats affichés */
    @FXML
    private void toutSelectionner() {
        // Si tous sont sélectionnés → tout désélectionner, sinon tout sélectionner
        boolean tousCoches = tousLesResultats.stream()
                .allMatch(r -> idsSelectionnes.contains(r.getIdResultat()));
        if (tousCoches) {
            idsSelectionnes.clear();
        } else {
            tousLesResultats.forEach(r -> idsSelectionnes.add(r.getIdResultat()));
        }
        appliquerFiltreEtRecherche(); // redessiner les cartes
    }

    // ── Applique filtre + recherche
    private void appliquerFiltreEtRecherche() {
        mettreAJourBoutonsFiltres();

        List<Resultats> affichage = tousLesResultats;

        // Filtre par état (Propre / Grave)
        if (!"Tous".equals(filtreActif)) {
            final String f = filtreActif;
            affichage = affichage.stream()
                    .filter(r -> f.equalsIgnoreCase(r.getEtat()))
                    .collect(Collectors.toList());
        }

        // Filtre par recherche
        if (!termeRecherche.isEmpty()) {
            final List<Resultats> avant = affichage;
            affichage = avant.stream()
                    .filter(r -> {
                        String patient = r.getNomPatient() != null ? r.getNomPatient().toLowerCase() : "";
                        String cr = r.getCompteRendu() != null ? r.getCompteRendu().toLowerCase() : "";
                        return patient.contains(termeRecherche) || cr.contains(termeRecherche);
                    })
                    .collect(Collectors.toList());
        }

        containerResultats.getChildren().clear();

        if (affichage == null || affichage.isEmpty()) {
            Label emptyLabel = new Label(termeRecherche.isEmpty()
                    ? "Aucun résultat pour le moment."
                    : "Aucun résultat pour « " + termeRecherche + " ».");
            emptyLabel.setStyle("-fx-text-fill: #8A94A6; -fx-font-size: 14px; -fx-padding: 20;");
            containerResultats.getChildren().add(emptyLabel);
            return;
        }

        lblMessage.setText(affichage.size() + " résultat(s) affiché(s)");
        for (Resultats r : affichage) {
            containerResultats.getChildren().add(creerCarteResultat(r));
        }
    }

    // ── Gestion visuelle des boutons de filtre
    private void mettreAJourBoutonsFiltres() {
        String inactif = "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B; " +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 20; " +
                "-fx-font-size: 12px; -fx-cursor: hand;";
        if (btnFiltreAll    != null) btnFiltreAll.setStyle(inactif);
        if (btnFiltreGrave  != null) btnFiltreGrave.setStyle(inactif);
        if (btnFiltrePropre != null) btnFiltrePropre.setStyle(inactif);

        String actifBase = "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 20; " +
                "-fx-font-size: 12px; -fx-cursor: hand;";
        switch (filtreActif) {
            case "Grave"  -> { if (btnFiltreGrave  != null) btnFiltreGrave.setStyle(actifBase +
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #DC2626;"); }
            case "Propre" -> { if (btnFiltrePropre != null) btnFiltrePropre.setStyle(actifBase +
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;"); }
            default       -> { if (btnFiltreAll    != null) btnFiltreAll.setStyle(actifBase +
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;"); }
        }
    }

    // ── Handlers des boutons de filtre
    @FXML private void filtrerTous()   { filtreActif = "Tous";   appliquerFiltreEtRecherche(); }
    @FXML private void filtrerPropre() { filtreActif = "Propre"; appliquerFiltreEtRecherche(); }
    @FXML private void filtrerGrave()  { filtreActif = "Grave";  appliquerFiltreEtRecherche(); }

    // ── Handlers recherche
    @FXML private void handleRecherche() {
        termeRecherche = tfRecherche != null ? tfRecherche.getText().trim().toLowerCase() : "";
        appliquerFiltreEtRecherche();
    }

    @FXML private void handleReinitialiser() {
        if (tfRecherche != null) tfRecherche.clear();
        termeRecherche = "";
        filtreActif = "Tous";
        appliquerFiltreEtRecherche();
    }

    // ── Crée une carte visuelle selon l'état (Propre / Grave) avec checkbox
    private VBox creerCarteResultat(Resultats r) {
        String etat  = r.getEtat() != null ? r.getEtat() : "Propre";
        boolean grave = "Grave".equalsIgnoreCase(etat);

        String borderColor = grave ? "#DC2626" : "#16A34A";
        String bgCard      = grave ? "#FFF8F8" : "white";
        String bgHover     = grave ? "#FFF0F0" : "#F0FDF4";
        String badgeBg     = grave ? "#FEE2E2" : "#DCFCE7";
        String badgeFg     = grave ? "#DC2626" : "#16A34A";
        String badgeIcon   = grave ? "🔴" : "🟢";

        VBox card = new VBox(10);
        boolean estSelectionne = idsSelectionnes.contains(r.getIdResultat());
        card.setStyle(
                "-fx-background-color: " + (estSelectionne ? (grave ? "#FEE2E2" : "#DCFCE7") : bgCard) + ";" +
                "-fx-background-radius: 16; -fx-padding: 16 18;" +
                "-fx-border-color: " + (estSelectionne ? (grave ? "#DC2626" : "#16A34A") : borderColor) + ";" +
                "-fx-border-width: " + (estSelectionne ? "2" : "0 0 0 5") + "; -fx-border-radius: 16;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);"
        );

        // Ligne 1 : Checkbox + Patient + badge état
        HBox ligne1 = new HBox(10);
        ligne1.setStyle("-fx-alignment: CENTER_LEFT;");

        // Checkbox de sélection
        javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox();
        cb.setSelected(estSelectionne);
        cb.setStyle("-fx-cursor: hand;");
        cb.selectedProperty().addListener((obs, wasSelected, isNow) -> {
            if (isNow) {
                idsSelectionnes.add(r.getIdResultat());
            } else {
                idsSelectionnes.remove(r.getIdResultat());
            }
            mettreAJourBoutonSuppression();
            // Mettre à jour le style de la carte
            card.setStyle(
                    "-fx-background-color: " + (isNow ? (grave ? "#FEE2E2" : "#DCFCE7") : bgCard) + ";" +
                    "-fx-background-radius: 16; -fx-padding: 16 18;" +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: " + (isNow ? "2" : "0 0 0 5") + "; -fx-border-radius: 16;" +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);"
            );
        });

        String patientLabel = (r.getNomPatient() != null && !r.getNomPatient().isBlank())
                ? r.getNomPatient() : "Demande #" + r.getIdDemande();
        Label title = new Label("👤  " + patientLabel);
        title.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label badge = new Label(badgeIcon + "  " + etat);
        badge.setStyle(
                "-fx-background-color: " + badgeBg + "; -fx-text-fill: " + badgeFg + ";" +
                "-fx-font-weight: bold; -fx-padding: 4 14; -fx-background-radius: 20; -fx-font-size: 11px;");
        ligne1.getChildren().addAll(cb, title, badge);

        // Ligne 2 : Compte rendu (tronqué)
        String cr = r.getCompteRendu() != null ? r.getCompteRendu() : "—";
        String crAffiche = cr.length() > 120 ? cr.substring(0, 120) + "…" : cr;
        Label compteRendu = new Label("📝  " + crAffiche);
        compteRendu.setWrapText(true);
        compteRendu.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");

        // Ligne 3 : Fichier + Date
        HBox ligne3 = new HBox(12);
        ligne3.setStyle("-fx-alignment: CENTER_LEFT;");
        String fichier = (r.getFichierJoint() != null && !r.getFichierJoint().isBlank())
                ? r.getFichierJoint() : "—";
        Label fichierLbl = new Label("📎  " + fichier);
        fichierLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        Label dateLbl = new Label("📅  " + (r.getDateResultat() != null
                ? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(r.getDateResultat()) : "—"));
        dateLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        ligne3.getChildren().addAll(fichierLbl, dateLbl);

        // Ligne 4 : Score de gravité + recommandation
        HBox ligne4 = new HBox(10);
        ligne4.setStyle("-fx-alignment: CENTER_LEFT;");
        if (r.getNiveauGravite() != null && !r.getNiveauGravite().isBlank()) {
            String niv = r.getNiveauGravite();
            Label scoreBadge = new Label(
                    GraviteService.emoji(niv) + "  Score : " + r.getScoreGravite() + "/100  —  " + niv);
            scoreBadge.setStyle(
                    "-fx-background-color: " + GraviteService.couleurFondClair(niv) + ";" +
                    "-fx-text-fill: " + GraviteService.couleurTexte(niv) + ";" +
                    "-fx-font-weight: bold; -fx-font-size: 11px;" +
                    "-fx-padding: 4 12; -fx-background-radius: 20;" +
                    "-fx-border-color: " + GraviteService.couleurFond(niv) + ";" +
                    "-fx-border-radius: 20; -fx-border-width: 1;");
            String recoTxt = r.getRecommandation() != null
                    ? (r.getRecommandation().length() > 60
                        ? r.getRecommandation().substring(0, 60) + "…"
                        : r.getRecommandation()) : "";
            Label recoLbl = new Label(recoTxt);
            recoLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 10px; -fx-font-style: italic;");
            ligne4.getChildren().addAll(scoreBadge, recoLbl);
        }

        card.getChildren().addAll(ligne1, compteRendu, ligne3);
        if (!ligne4.getChildren().isEmpty()) card.getChildren().add(ligne4);

        final String bgFinal = bgCard, bgHoverFinal = bgHover;
        card.setOnMouseClicked(event -> {
            resultatSelectionne = r;
            lblMessage.setStyle("-fx-text-fill: #0B4EA2; -fx-font-weight: bold;");
            lblMessage.setText("✔  Sélectionné : " + patientLabel + " [" + etat + "]");
        });
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: " + bgHoverFinal + ";" +
                "-fx-background-radius: 16; -fx-padding: 16 18;" +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 5; -fx-border-radius: 16;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.15),14,0,0,5); -fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + bgFinal + ";" +
                "-fx-background-radius: 16; -fx-padding: 16 18;" +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 5; -fx-border-radius: 16;" +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);"
        ));

        return card;
    }

    // ── Actions CRUD
    @FXML private void ouvrirAjouter() {
        resultatSelectionne = null;
        ouvrirFenetre("/ResourcesLabo/AjouterResultat.fxml", "Ajouter un résultat");
    }

    @FXML private void ouvrirModifier() {
        if (resultatSelectionne == null) { afficherMessage("⚠  Veuillez sélectionner un résultat à modifier."); return; }
        ouvrirFenetre("/ResourcesLabo/ModifierResultat.fxml", "Modifier un résultat");
    }

    @FXML private void ouvrirSupprimer() {
        // ── Cas 1 : sélection multiple → suppression directe en lot
        if (!idsSelectionnes.isEmpty()) {
            supprimerSelection();
            return;
        }
        // ── Cas 2 : sélection simple → fenêtre de confirmation habituelle
        if (resultatSelectionne == null) { afficherMessage("⚠  Veuillez sélectionner un résultat à supprimer."); return; }
        ouvrirFenetre("/ResourcesLabo/SupprimerResultat.fxml", "Supprimer un résultat");
    }

    @FXML private void handleActualiser() {
        lblMessage.setText("");
        resultatSelectionne = null;
        chargerResultats();
    }

    // ── Télécharger PDF du résultat sélectionné
    @FXML private void handleTelechargerPdf() {
        if (resultatSelectionne == null) {
            afficherMessage("⚠  Veuillez sélectionner un résultat pour générer le PDF.");
            return;
        }

        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport PDF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String patient = (resultatSelectionne.getNomPatient() != null && !resultatSelectionne.getNomPatient().isBlank())
                ? resultatSelectionne.getNomPatient().replace(" ", "_") : "rapport";
        fc.setInitialFileName("rapport_" + patient + ".pdf");

        Stage stage = (Stage) containerResultats.getScene().getWindow();
        File fichier = fc.showSaveDialog(stage);

        if (fichier != null) {
            try {
                PdfGenerator.genererRapport(resultatSelectionne, fichier.getAbsolutePath());
                lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                lblMessage.setText("✅  PDF généré : " + fichier.getName());
            } catch (Exception e) {
                lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
                lblMessage.setText("❌  Erreur PDF : " + e.getMessage());
            }
        }
    }

    // ── Ouvrir l'assistant IA d'analyse
    @FXML private void handleAnalyserResultat() {
        if (resultatSelectionne == null) {
            afficherMessage("⚠  Veuillez sélectionner un résultat à analyser.");
            return;
        }
        ouvrirFenetre("/ResourcesLabo/AnalyserResultat.fxml", "Assistant Médical IA — Analyse du Résultat");
    }

    private void ouvrirFenetre(String fxml, String titre) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setTitle(titre);
            // Taille adaptée selon la fenêtre
            if (fxml.contains("Analyser")) {
                stage.setScene(new Scene(root, 700, 580));
            } else {
                stage.setScene(new Scene(root, 900, 620));
            }
            stage.setResizable(true);
            stage.setOnHidden(event -> chargerResultats());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            afficherMessage("Erreur ouverture : " + fxml);
        }
    }

    private void afficherMessage(String message) {
        lblMessage.setText(message);
        lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
    }

    // ── Navigation sidebar
    @FXML void showDashboard()  { naviguerVers("/ResourcesLabo/DashboardLabo.fxml"); }
    @FXML void showExamens()    { naviguerVers("/ResourcesLabo/GestionExamens.fxml"); }
    @FXML void showResultats()  { chargerResultats(); }
    @FXML void showRapport()    { naviguerVers("/ResourcesLabo/RapportBiologiste.fxml"); }

    // ── Effets hover sur les boutons icônes
    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setScaleX(1.15);
            btn.setScaleY(1.15);
        }
    }
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        }
    }

    // ── Effets hover sur les boutons du sidebar
    public void onSidebarHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
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
        if (e.getSource() instanceof Button btn) {
            btn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: rgba(255,255,255,0.72);" +
                "-fx-font-size: 13px; -fx-padding: 13 30; -fx-alignment: CENTER_LEFT;" +
                "-fx-cursor: hand; -fx-background-radius: 10;"
            );
            btn.setTranslateX(0);
        }
    }

    private void naviguerVers(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) containerResultats.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
        } catch (Exception e) {
            e.printStackTrace();
            afficherMessage("Erreur navigation.");
        }
    }
}

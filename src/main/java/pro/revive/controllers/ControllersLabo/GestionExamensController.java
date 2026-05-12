package pro.revive.controllers.ControllersLabo;

import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.services.ServicesLabo.Examens_demandesService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

public class GestionExamensController {

    @FXML private VBox      containerExamens;
    @FXML private Label     lblMessage;
    @FXML private TextField tfRecherche;

    @FXML private Button btnFiltreAll;
    @FXML private Button btnFiltreAttente;
    @FXML private Button btnFiltreRealise;
    @FXML private Button btnFiltreUrgent;

    private final Examens_demandesService service = new Examens_demandesService();

    private static Examens_demandes examenSelectionne;
    private List<Examens_demandes>  tousLesExamens;
    private String filtreActif    = "Tous";
    private String termeRecherche = "";

    // ── Sélection multiple
    private final java.util.Set<Integer> idsSelectionnes = new java.util.HashSet<>();

    public static Examens_demandes getExamenSelectionne() { return examenSelectionne; }

    @FXML
    public void initialize() {
        chargerExamens();
        if (tfRecherche != null) {
            tfRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
                termeRecherche = newVal != null ? newVal.trim().toLowerCase() : "";
                appliquerFiltreEtRecherche();
            });
        }
    }

    private void chargerExamens() {
        tousLesExamens = service.getAllExamens();
        idsSelectionnes.clear();
        tousLesExamens.sort((a, b) -> {
            if (a.getDateDemande() != null && b.getDateDemande() != null)
                return b.getDateDemande().compareTo(a.getDateDemande());
            if (a.getDateDemande() != null) return -1;
            if (b.getDateDemande() != null) return  1;
            return 0;
        });
        appliquerFiltreEtRecherche();
    }

    /** Met à jour le compteur de sélection */
    private void mettreAJourCompteurSelection() {
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

    /** Supprime tous les examens sélectionnés */
    @FXML
    private void supprimerSelection() {
        if (idsSelectionnes.isEmpty()) {
            afficherMessage("⚠  Sélectionnez au moins un examen à supprimer.");
            return;
        }
        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText(null);
        confirm.setContentText("Supprimer " + idsSelectionnes.size() + " examen(s) sélectionné(s) ?\n" +
                "Cette action est irréversible.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                try {
                    service.supprimerExamens(new java.util.ArrayList<>(idsSelectionnes));
                    int nb = idsSelectionnes.size();
                    idsSelectionnes.clear();
                    examenSelectionne = null;
                    chargerExamens();
                    lblMessage.setStyle("-fx-text-fill: #16A34A; -fx-font-weight: bold;");
                    lblMessage.setText("✅  " + nb + " examen(s) supprimé(s).");
                } catch (Exception e) {
                    afficherMessage("❌  Erreur : " + e.getMessage());
                }
            }
        });
    }

    /** Sélectionne / désélectionne tous */
    @FXML
    private void toutSelectionner() {
        boolean tousCoches = tousLesExamens.stream()
                .allMatch(e -> idsSelectionnes.contains(e.getIdDemande()));
        if (tousCoches) idsSelectionnes.clear();
        else tousLesExamens.forEach(e -> idsSelectionnes.add(e.getIdDemande()));
        appliquerFiltreEtRecherche();
    }

    private void appliquerFiltreEtRecherche() {
        mettreAJourBoutonsFiltres();

        List<Examens_demandes> affichage = tousLesExamens;

        if ("En attente".equals(filtreActif)) {
            affichage = affichage.stream()
                    .filter(e -> "En attente".equalsIgnoreCase(e.getStatut()))
                    .collect(Collectors.toList());
        } else if ("Realise".equals(filtreActif)) {
            affichage = affichage.stream()
                    .filter(e -> "Realise".equalsIgnoreCase(e.getStatut()))
                    .collect(Collectors.toList());
        } else if ("Urgent".equals(filtreActif)) {
            affichage = affichage.stream()
                    .filter(Examens_demandes::isUrgent)
                    .collect(Collectors.toList());
        }

        if (!termeRecherche.isEmpty()) {
            final List<Examens_demandes> avant = affichage;
            affichage = avant.stream()
                    .filter(e -> {
                        String type    = e.getTypeExamen() != null ? e.getTypeExamen().toLowerCase() : "";
                        String patient = e.getNomPatient() != null ? e.getNomPatient().toLowerCase() : "";
                        return type.contains(termeRecherche) || patient.contains(termeRecherche);
                    })
                    .collect(Collectors.toList());
        }

        containerExamens.getChildren().clear();

        if (affichage == null || affichage.isEmpty()) {
            Label emptyLabel = new Label(termeRecherche.isEmpty()
                    ? "Aucun examen pour le moment."
                    : "Aucun résultat pour « " + termeRecherche + " ».");
            emptyLabel.setStyle("-fx-text-fill: #8A94A6; -fx-font-size: 14px; -fx-padding: 20;");
            containerExamens.getChildren().add(emptyLabel);
            lblMessage.setText("");
            return;
        }

        // ── Séparer en 3 groupes : Analyses, Imageries, Autres (sans préfixe)
        List<Examens_demandes> analyses  = affichage.stream()
                .filter(e -> e.getTypeExamen() != null && e.getTypeExamen().startsWith("[ANALYSE]"))
                .collect(Collectors.toList());

        List<Examens_demandes> imageries = affichage.stream()
                .filter(e -> e.getTypeExamen() != null && e.getTypeExamen().startsWith("[IMAGERIE]"))
                .collect(Collectors.toList());

        List<Examens_demandes> autres    = affichage.stream()
                .filter(e -> e.getTypeExamen() == null
                        || (!e.getTypeExamen().startsWith("[ANALYSE]")
                            && !e.getTypeExamen().startsWith("[IMAGERIE]")))
                .collect(Collectors.toList());

        lblMessage.setText(affichage.size() + " examen(s) — "
                + analyses.size() + " analyse(s), "
                + imageries.size() + " imagerie(s)");

        // ── Section Analyses
        if (!analyses.isEmpty()) {
            containerExamens.getChildren().add(creerEnteteSection(
                    "🔬  Analyses de Laboratoire", analyses.size(),
                    "#1E40AF", "#EFF6FF", "#BFDBFE"));
            for (Examens_demandes e : analyses) {
                containerExamens.getChildren().add(creerCarteExamen(e, "ANALYSE"));
            }
            containerExamens.getChildren().add(creerEspaceur());
        }

        // ── Section Imageries
        if (!imageries.isEmpty()) {
            containerExamens.getChildren().add(creerEnteteSection(
                    "🩻  Imageries Médicales", imageries.size(),
                    "#065F46", "#ECFDF5", "#6EE7B7"));
            for (Examens_demandes e : imageries) {
                containerExamens.getChildren().add(creerCarteExamen(e, "IMAGERIE"));
            }
            containerExamens.getChildren().add(creerEspaceur());
        }

        // ── Section Autres (examens sans préfixe, anciens ou saisis manuellement)
        if (!autres.isEmpty()) {
            containerExamens.getChildren().add(creerEnteteSection(
                    "📋  Autres Examens", autres.size(),
                    "#475569", "#F8FAFC", "#CBD5E1"));
            for (Examens_demandes e : autres) {
                containerExamens.getChildren().add(creerCarteExamen(e, "AUTRE"));
            }
        }
    }

    // ── En-tête de section coloré
    private HBox creerEnteteSection(String titre, int count,
                                    String couleurTexte, String bgColor, String borderColor) {
        HBox header = new HBox(10);
        header.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 10;" +
                "-fx-padding: 10 16;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 10;" +
                "-fx-border-width: 1;"
        );

        Label titreLbl = new Label(titre);
        titreLbl.setStyle("-fx-text-fill: " + couleurTexte + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label compteur = new Label(count + " examen(s)");
        compteur.setStyle(
                "-fx-background-color: " + couleurTexte + "; -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 11px;"
        );

        header.getChildren().addAll(titreLbl, spacer, compteur);
        return header;
    }

    // ── Espaceur entre sections
    private Region creerEspaceur() {
        Region r = new Region();
        r.setPrefHeight(6);
        return r;
    }

    // ── Crée une carte visuelle pour un examen
    private VBox creerCarteExamen(Examens_demandes examen, String categorie) {
        VBox card = new VBox(10);

        // Couleurs selon catégorie et urgence
        String borderColor, bgColor, bgHover;
        if (examen.isUrgent()) {
            borderColor = "#EF4444"; bgColor = "#FFF8F8"; bgHover = "#FFF0F0";
        } else if ("ANALYSE".equals(categorie)) {
            borderColor = "#3B82F6"; bgColor = "white"; bgHover = "#F0F7FF";
        } else if ("IMAGERIE".equals(categorie)) {
            borderColor = "#10B981"; bgColor = "white"; bgHover = "#F0FDF4";
        } else {
            borderColor = "#94A3B8"; bgColor = "white"; bgHover = "#F8FAFC";
        }

        card.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                "-fx-background-radius: 14;" +
                "-fx-padding: 14 16;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 0 0 0 4;" +
                "-fx-border-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(15,23,42,0.08),8,0,0,2);"
        );

        // Nettoyer le préfixe pour l'affichage
        String typeAffiche = examen.getTypeExamen() != null
                ? examen.getTypeExamen()
                        .replace("[ANALYSE] ", "")
                        .replace("[IMAGERIE] ", "")
                : "—";

        // Ligne 1 : Checkbox + Icône catégorie + type + badge urgent
        HBox ligne1 = new HBox(10);
        ligne1.setStyle("-fx-alignment: CENTER_LEFT;");

        // Checkbox de sélection
        javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox();
        cb.setSelected(idsSelectionnes.contains(examen.getIdDemande()));
        cb.setStyle("-fx-cursor: hand;");
        cb.selectedProperty().addListener((obs, wasSelected, isNow) -> {
            if (isNow) idsSelectionnes.add(examen.getIdDemande());
            else       idsSelectionnes.remove(examen.getIdDemande());
            mettreAJourCompteurSelection();
            card.setStyle(
                    "-fx-background-color: " + (isNow ? (examen.isUrgent() ? "#FFF0F0" : "ANALYSE".equals(categorie) ? "#EFF6FF" : "#ECFDF5") : bgColor) + ";" +
                    "-fx-background-radius: 14; -fx-padding: 14 16;" +
                    "-fx-border-color: " + borderColor + ";" +
                    "-fx-border-width: " + (isNow ? "2" : "0 0 0 4") + "; -fx-border-radius: 14;" +
                    "-fx-effect: dropshadow(gaussian,rgba(15,23,42,0.08),8,0,0,2);"
            );
        });

        String icone = "ANALYSE".equals(categorie) ? "🔬" : "IMAGERIE".equals(categorie) ? "🩻" : "📋";
        Label title = new Label(icone + "  " + typeAffiche);
        title.setStyle("-fx-text-fill: #0F172A; -fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        if (examen.isUrgent()) {
            Label urgentBadge = new Label("⚠  URGENT");
            urgentBadge.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444;" +
                    "-fx-font-weight: bold; -fx-padding: 3 10; -fx-background-radius: 20; -fx-font-size: 11px;");
            ligne1.getChildren().addAll(cb, title, spacer, urgentBadge);
        } else {
            ligne1.getChildren().addAll(cb, title, spacer);
        }

        // Ligne 2 : Patient + Date
        String patientLabel = (examen.getNomPatient() != null && !examen.getNomPatient().isBlank())
                ? examen.getNomPatient() : "Consultation #" + examen.getIdConsultation();

        HBox ligne2 = new HBox(20);
        Label patientLbl = new Label("👤  " + patientLabel);
        patientLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px;");
        Label dateLbl = new Label("📅  " + (examen.getDateDemande() != null
                ? new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(examen.getDateDemande()) : "—"));
        dateLbl.setStyle("-fx-text-fill: #64748B; -fx-font-size: 12px;");
        ligne2.getChildren().addAll(patientLbl, dateLbl);

        // Ligne 3 : Badge statut
        String statut    = examen.getStatut() != null ? examen.getStatut() : "En attente";
        boolean realise  = "Realise".equalsIgnoreCase(statut);
        Label lblStatut  = new Label((realise ? "✅" : "⏳") + "  " + statut);
        lblStatut.setStyle(
                "-fx-text-fill: " + (realise ? "#16A34A" : "#92400E") + ";" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-background-color: " + (realise ? "#DCFCE7" : "#FEF9C3") + ";" +
                "-fx-padding: 3 10; -fx-background-radius: 10;"
        );

        card.getChildren().addAll(ligne1, ligne2, lblStatut);

        // Sélection
        final String bgFinal = bgColor, bgHoverFinal = bgHover;
        card.setOnMouseClicked(event -> {
            examenSelectionne = examen;
            lblMessage.setStyle("-fx-text-fill: #0B4EA2; -fx-font-weight: bold;");
            lblMessage.setText("✔  Sélectionné : " + typeAffiche + " — " + patientLabel);
        });
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: " + bgHoverFinal + ";" +
                "-fx-background-radius: 14; -fx-padding: 14 16;" +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(15,23,42,0.15),12,0,0,4); -fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: " + bgFinal + ";" +
                "-fx-background-radius: 14; -fx-padding: 14 16;" +
                "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 14;" +
                "-fx-effect: dropshadow(gaussian,rgba(15,23,42,0.08),8,0,0,2);"
        ));

        return card;
    }

    private void mettreAJourBoutonsFiltres() {
        String inactif = "-fx-background-color: #F1F5F9; -fx-text-fill: #64748B;" +
                "-fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 7 20;" +
                "-fx-font-size: 12px; -fx-cursor: hand;";
        if (btnFiltreAll != null)     btnFiltreAll.setStyle(inactif);
        if (btnFiltreAttente != null) btnFiltreAttente.setStyle(inactif);
        if (btnFiltreRealise != null) btnFiltreRealise.setStyle(inactif);
        if (btnFiltreUrgent != null)  btnFiltreUrgent.setStyle(inactif);

        String actifBase = "-fx-font-weight: bold; -fx-background-radius: 20;" +
                "-fx-padding: 7 20; -fx-font-size: 12px; -fx-cursor: hand;";
        switch (filtreActif) {
            case "En attente" -> { if (btnFiltreAttente != null) btnFiltreAttente.setStyle(actifBase +
                    "-fx-background-color: #FEF9C3; -fx-text-fill: #92400E;"); }
            case "Realise"    -> { if (btnFiltreRealise != null) btnFiltreRealise.setStyle(actifBase +
                    "-fx-background-color: #DCFCE7; -fx-text-fill: #16A34A;"); }
            case "Urgent"     -> { if (btnFiltreUrgent != null) btnFiltreUrgent.setStyle(actifBase +
                    "-fx-background-color: #FEE2E2; -fx-text-fill: #EF4444;"); }
            default           -> { if (btnFiltreAll != null) btnFiltreAll.setStyle(actifBase +
                    "-fx-background-color: #DBEAFE; -fx-text-fill: #1E40AF;"); }
        }
    }

    @FXML private void filtrerTous()      { filtreActif = "Tous";       appliquerFiltreEtRecherche(); }
    @FXML private void filtrerEnAttente() { filtreActif = "En attente"; appliquerFiltreEtRecherche(); }
    @FXML private void filtrerRealise()   { filtreActif = "Realise";    appliquerFiltreEtRecherche(); }
    @FXML private void filtrerUrgent()    { filtreActif = "Urgent";     appliquerFiltreEtRecherche(); }

    @FXML private void handleRecherche() {
        termeRecherche = tfRecherche != null ? tfRecherche.getText().trim().toLowerCase() : "";
        appliquerFiltreEtRecherche();
    }
    @FXML private void handleReinitialiser() {
        if (tfRecherche != null) tfRecherche.clear();
        termeRecherche = ""; filtreActif = "Tous";
        appliquerFiltreEtRecherche();
    }

    @FXML private void ouvrirAjouter() {
        examenSelectionne = null;
        ouvrirFenetre("/ResourcesLabo/AjouterExamen.fxml", "Ajouter un examen");
    }
    @FXML private void ouvrirModifier() {
        if (examenSelectionne == null) { afficherMessage("⚠  Veuillez sélectionner un examen."); return; }
        ouvrirFenetre("/ResourcesLabo/ModifierExamen.fxml", "Modifier un examen");
    }
    @FXML private void ouvrirSupprimer() {
        // ── Cas 1 : sélection multiple → suppression directe en lot
        if (!idsSelectionnes.isEmpty()) {
            supprimerSelection();
            return;
        }
        // ── Cas 2 : sélection simple → fenêtre de confirmation habituelle
        if (examenSelectionne == null) { afficherMessage("⚠  Veuillez sélectionner un examen."); return; }
        ouvrirFenetre("/ResourcesLabo/SupprimerExamen.fxml", "Supprimer un examen");
    }
    @FXML private void handleActualiser() {
        lblMessage.setText(""); examenSelectionne = null; chargerExamens();
    }

    private void ouvrirFenetre(String fxml, String titre) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = new Stage();
            stage.setTitle(titre);
            stage.setScene(new Scene(root, 920, 780));
            stage.setResizable(true);
            stage.setOnHidden(event -> chargerExamens());
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            afficherMessage("Erreur ouverture : " + fxml);
        }
    }

    private void afficherMessage(String msg) {
        lblMessage.setText(msg);
        lblMessage.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
    }

    @FXML private void showDashboard()  { naviguerVers("/ResourcesLabo/DashboardLabo.fxml"); }
    @FXML private void showExamens()    { chargerExamens(); }
    @FXML private void showResultats()  { naviguerVers("/ResourcesLabo/GestionResultats.fxml"); }
    @FXML private void showRapport()    { naviguerVers("/ResourcesLabo/RapportBiologiste.fxml"); }

    // ── Effets hover sur les boutons icônes
    @FXML
    public void onBtnHoverEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setScaleX(1.15);
            btn.setScaleY(1.15);
        }
    }

    @FXML
    public void onBtnHoverExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof Button btn) {
            btn.setScaleX(1.0);
            btn.setScaleY(1.0);
        }
    }

    // ── Effets hover sur les boutons du sidebar
    @FXML
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

    @FXML
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
            Stage stage = (Stage) containerExamens.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            stage.setScene(new Scene(root, w, h));
        } catch (Exception e) {
            e.printStackTrace();
            afficherMessage("Erreur navigation.");
        }
    }
}

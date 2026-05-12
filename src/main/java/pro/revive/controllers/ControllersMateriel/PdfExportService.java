package pro.revive.controllers.ControllersMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.AlerteMaintenance;
import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Export Matériel ───────────────────────────────────────────────
    public static void exporterMateriel(ObservableList<MaterielUrgence> materiels, Stage ownerStage) {
        if (materiels.isEmpty()) { afficherInfo("Export PDF", "Aucun équipement à exporter."); return; }

        File fichier = choisirFichier(ownerStage, "inventaire_materiel_");
        if (fichier == null) return;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fichier), "UTF-8"))) {
            String ligne = "=".repeat(80);
            String tiret = "-".repeat(80);
            pw.println(ligne);
            pw.println("  REVIVE — Inventaire du Matériel d'Urgence");
            pw.println("  Généré le : " + LocalDate.now().format(FMT));
            pw.println(ligne);
            pw.println();
            pw.printf("%-5s  %-30s  %-20s  %-14s  %-12s  %s%n", "ID", "Nom", "Salle", "Maintenance", "État", "Qté");
            pw.println(tiret);
            for (MaterielUrgence m : materiels) {
                String date = m.getDateDerniereMaintenance() != null ? m.getDateDerniereMaintenance().format(FMT) : "N/A";
                pw.printf("%-5d  %-30s  %-20s  %-14s  %-12s  %d%n",
                    m.getIdMateriel(), tronquer(m.getNom(), 30), tronquer(m.getNomSalle(), 20),
                    date, m.getEtat(), m.getQuantite());
            }
            pw.println(tiret);
            long fonc = materiels.stream().filter(m -> "Fonctionnel".equals(m.getEtat())).count();
            long rev  = materiels.stream().filter(m -> "A reviser".equals(m.getEtat())).count();
            pw.println("\n  RÉSUMÉ");
            pw.println("  Total : " + materiels.size() + "  |  Fonctionnels : " + fonc + "  |  À réviser : " + rev);
            pw.println("\n" + ligne);
            afficherSucces("Export réussi", "Fichier : " + fichier.getAbsolutePath());
        } catch (IOException e) { afficherErreur("Erreur d'export", e.getMessage()); }
    }

    // ── Export Ambulances ─────────────────────────────────────────────
    public static void exporterAmbulances(List<Ambulance> ambulances, Stage ownerStage) {
        if (ambulances.isEmpty()) { afficherInfo("Export", "Aucune ambulance à exporter."); return; }

        File fichier = choisirFichier(ownerStage, "rapport_ambulances_");
        if (fichier == null) return;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fichier), "UTF-8"))) {
            String ligne = "=".repeat(90);
            String tiret = "-".repeat(90);
            pw.println(ligne);
            pw.println("  REVIVE — Rapport de la Flotte d'Ambulances");
            pw.println("  Généré le : " + LocalDate.now().format(FMT));
            pw.println(ligne);
            pw.println();
            pw.printf("%-15s  %-18s  %-18s  %-6s  %-12s  %-12s  %s%n",
                "N° Série", "Marque", "Modèle", "Année", "État", "KM Total", "Dernière Vidange");
            pw.println(tiret);
            for (Ambulance a : ambulances) {
                pw.printf("%-15s  %-18s  %-18s  %-6s  %-12s  %-12s  %s%n",
                    a.getNumeroSerie(),
                    tronquer(a.getMarque(), 18),
                    tronquer(a.getModele() != null ? a.getModele() : "-", 18),
                    a.getAnneeFabrication() != null ? a.getAnneeFabrication() : "-",
                    a.getEtat(),
                    String.format("%.0f km", a.getKmTotal()),
                    a.getDateDerniereVidange() != null ? a.getDateDerniereVidange().format(FMT) : "N/A");
            }
            pw.println(tiret);
            long dispo = ambulances.stream().filter(a -> "Disponible".equals(a.getEtat())).count();
            long route = ambulances.stream().filter(a -> "En route".equals(a.getEtat())).count();
            long panne = ambulances.stream().filter(a -> "En panne".equals(a.getEtat())).count();
            pw.println("\n  RÉSUMÉ");
            pw.println("  Total : " + ambulances.size() + "  |  Disponibles : " + dispo +
                       "  |  En route : " + route + "  |  En panne : " + panne);
            pw.println("\n" + ligne);
            afficherSucces("Export réussi", "Fichier : " + fichier.getAbsolutePath());
        } catch (IOException e) { afficherErreur("Erreur d'export", e.getMessage()); }
    }

    // ── Export Alertes Maintenance ────────────────────────────────────
    public static void exporterAlertesMaintenance(List<AlerteMaintenance> alertes,
                                                   List<Ambulance> ambulances, Stage ownerStage) {
        if (alertes.isEmpty()) { afficherInfo("Export", "Aucune alerte à exporter."); return; }

        File fichier = choisirFichier(ownerStage, "alertes_maintenance_");
        if (fichier == null) return;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fichier), "UTF-8"))) {
            String ligne = "=".repeat(90);
            String tiret = "-".repeat(90);
            pw.println(ligne);
            pw.println("  REVIVE — Rapport des Alertes de Maintenance IA");
            pw.println("  Généré le : " + LocalDate.now().format(FMT));
            pw.println(ligne);
            pw.println();
            pw.printf("%-15s  %-20s  %-10s  %-12s  %-12s  %s%n",
                "Ambulance", "Type", "Priorité", "KM Actuel", "KM Recommandé", "Description");
            pw.println(tiret);
            for (AlerteMaintenance al : alertes) {
                String numSerie = ambulances.stream()
                    .filter(a -> a.getIdAmbulance() == al.getIdAmbulance())
                    .map(Ambulance::getNumeroSerie).findFirst().orElse("?");
                pw.printf("%-15s  %-20s  %-10s  %-12s  %-12s  %s%n",
                    numSerie, al.getTypeMaintenance(), al.getPriorite(),
                    String.format("%.0f km", al.getKmActuel()),
                    String.format("%.0f km", al.getKmRecommande()),
                    tronquer(al.getDescription(), 40));
            }
            pw.println(tiret);
            long critique = alertes.stream().filter(a -> "Critique".equals(a.getPriorite())).count();
            long elevee   = alertes.stream().filter(a -> "Élevée".equals(a.getPriorite())).count();
            pw.println("\n  RÉSUMÉ");
            pw.println("  Total alertes : " + alertes.size() + "  |  Critiques : " + critique + "  |  Élevées : " + elevee);
            pw.println("\n" + ligne);
            afficherSucces("Export réussi", "Fichier : " + fichier.getAbsolutePath());
        } catch (IOException e) { afficherErreur("Erreur d'export", e.getMessage()); }
    }

    // ── Export Historique Missions ────────────────────────────────────
    public static void exporterHistoriqueMissions(
            ObservableList<HistoriqueMissionsController.TrajetRow> missions, Stage ownerStage) {
        if (missions.isEmpty()) { afficherInfo("Export", "Aucune mission à exporter."); return; }

        File fichier = choisirFichier(ownerStage, "historique_missions_");
        if (fichier == null) return;

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fichier), "UTF-8"))) {
            String ligne = "=".repeat(100);
            String tiret = "-".repeat(100);
            pw.println(ligne);
            pw.println("  REVIVE — Historique des Missions Ambulances");
            pw.println("  Généré le : " + LocalDate.now().format(FMT));
            pw.println(ligne);
            pw.println();
            pw.printf("%-18s  %-15s  %-25s  %-25s  %-10s  %-8s  %s%n",
                "Date", "Ambulance", "Départ", "Urgence", "Distance", "Durée", "Statut");
            pw.println(tiret);
            for (var m : missions) {
                pw.printf("%-18s  %-15s  %-25s  %-25s  %-10s  %-8s  %s%n",
                    m.getDate(), tronquer(m.getAmbulance(), 15),
                    tronquer(m.getDepart(), 25), tronquer(m.getUrgence(), 25),
                    m.getDistance(), m.getDuree(), m.getStatut());
            }
            pw.println(tiret);
            double distTotale = missions.stream()
                .mapToDouble(r -> { try { return Double.parseDouble(r.getDistance().replace(" km","")); } catch(Exception e){return 0;} })
                .sum();
            pw.println("\n  RÉSUMÉ");
            pw.println("  Total missions : " + missions.size() + "  |  Distance totale : " + String.format("%.1f km", distTotale));
            pw.println("\n" + ligne);
            afficherSucces("Export réussi", "Fichier : " + fichier.getAbsolutePath());
        } catch (IOException e) { afficherErreur("Erreur d'export", e.getMessage()); }
    }

    // ── Utilitaires ───────────────────────────────────────────────────
    private static File choisirFichier(Stage owner, String prefix) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le rapport");
        chooser.setInitialFileName(prefix + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier texte (*.txt)", "*.txt"));
        return chooser.showSaveDialog(owner);
    }

    private static String tronquer(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    private static void afficherSucces(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private static void afficherInfo(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private static void afficherErreur(String titre, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(titre); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}

package pro.revive.services.ServicesMed;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service de statistiques pour le tableau de bord.
 * Toutes les requêtes sont en lecture seule.
 */
public class StatistiquesService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── Compteurs globaux ─────────────────────────────────────────────────

    public int getTotalConsultations() {
        return countQuery("SELECT COUNT(*) FROM consultations");
    }

    public int getConsultationsEnCours() {
        return countQuery("SELECT COUNT(*) FROM consultations WHERE date_heure_fin IS NULL");
    }

    public int getConsultationsCloturees() {
        return countQuery("SELECT COUNT(*) FROM consultations WHERE date_heure_fin IS NOT NULL");
    }

    public int getTotalOrdonnances() {
        return countQuery("SELECT COUNT(*) FROM ordonnances");
    }

    public int getConsultationsAujourdhui() {
        return countQuery(
            "SELECT COUNT(*) FROM consultations WHERE DATE(date_heure_debut) = CURDATE()");
    }

    public int getConsultationsCetteSemaine() {
        return countQuery(
            "SELECT COUNT(*) FROM consultations "
          + "WHERE YEARWEEK(date_heure_debut, 1) = YEARWEEK(CURDATE(), 1)");
    }

    // ── Répartition par orientation ───────────────────────────────────────

    /**
     * Retourne le nombre de consultations par orientation.
     * Clé = orientation (ou "En cours"), Valeur = count.
     */
    public Map<String, Integer> getRepartitionOrientation() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT COALESCE(orientation, 'En cours') AS orient, COUNT(*) AS nb "
                   + "FROM consultations GROUP BY orient ORDER BY nb DESC";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("orient"), rs.getInt("nb"));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return map;
    }

    // ── Activité par jour (7 derniers jours) ──────────────────────────────

    /**
     * Retourne le nombre de consultations par jour sur les 7 derniers jours.
     * Clé = date (dd/MM), Valeur = count.
     */
    public Map<String, Integer> getActivite7Jours() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT DATE_FORMAT(date_heure_debut, '%d/%m') AS jour, COUNT(*) AS nb "
                   + "FROM consultations "
                   + "WHERE date_heure_debut >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) "
                   + "GROUP BY DATE(date_heure_debut), jour "
                   + "ORDER BY DATE(date_heure_debut)";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("jour"), rs.getInt("nb"));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return map;
    }

    // ── Top médecins ──────────────────────────────────────────────────────

    /**
     * Retourne les 5 médecins avec le plus de consultations.
     * Clé = "Prénom Nom", Valeur = count.
     */
    public Map<String, Integer> getTopMedecins() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT CONCAT(p.prenom, ' ', p.nom) AS medecin, COUNT(*) AS nb "
                   + "FROM consultations c "
                   + "JOIN personnel p ON p.id_personnel = c.id_personnel_medecin "
                   + "GROUP BY c.id_personnel_medecin, medecin "
                   + "ORDER BY nb DESC LIMIT 5";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString("medecin"), rs.getInt("nb"));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return map;
    }

    // ── Durée moyenne ─────────────────────────────────────────────────────

    /**
     * Retourne la durée moyenne des consultations clôturées en minutes.
     */
    public double getDureeMoyenneMinutes() {
        String sql = "SELECT AVG(TIMESTAMPDIFF(MINUTE, date_heure_debut, date_heure_fin)) "
                   + "FROM consultations WHERE date_heure_fin IS NOT NULL";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    // ── Consultations récentes ────────────────────────────────────────────

    /**
     * Retourne les 5 dernières consultations (id, patient, médecin, orientation, statut).
     */
    public java.util.List<String[]> getDernieresConsultations() {
        java.util.List<String[]> list = new java.util.ArrayList<>();
        String sql = "SELECT c.id_consultation, "
                   + "CONCAT(pt.prenom,' ',pt.nom) AS patient, "
                   + "CONCAT(p.prenom,' ',p.nom) AS medecin, "
                   + "COALESCE(c.orientation,'—') AS orientation, "
                   + "CASE WHEN c.date_heure_fin IS NULL THEN 'En cours' ELSE 'Clôturée' END AS statut, "
                   + "DATE_FORMAT(c.date_heure_debut,'%d/%m %H:%i') AS debut "
                   + "FROM consultations c "
                   + "LEFT JOIN personnel p ON p.id_personnel = c.id_personnel_medecin "
                   + "LEFT JOIN admissions a ON a.id_admission = c.id_admission "
                   + "LEFT JOIN patients pt ON pt.id_patient = a.id_patient "
                   + "ORDER BY c.date_heure_debut DESC LIMIT 5";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                    "#" + rs.getInt("id_consultation"),
                    rs.getString("patient"),
                    rs.getString("medecin"),
                    rs.getString("orientation"),
                    rs.getString("statut"),
                    rs.getString("debut")
                });
            }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return list;
    }

    // ── Utilitaire ────────────────────────────────────────────────────────

    private int countQuery(String sql) {
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    // ── Feature 7 — Statistiques enrichies ───────────────────────────────

    /** Nombre de demandes labo/imagerie envoyees. */
    public int getDemandesEnvoyees() {
        return countQuery("SELECT COUNT(*) FROM consultations WHERE statut_demande = 'Envoyee'");
    }

    /** Top 5 diagnostics les plus frequents. */
    public Map<String, Integer> getTopDiagnostics() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT diagnostic, COUNT(*) AS nb FROM consultations "
                   + "WHERE diagnostic IS NOT NULL AND diagnostic != '' "
                   + "GROUP BY diagnostic ORDER BY nb DESC LIMIT 5";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                String diag = rs.getString("diagnostic");
                if (diag.length() > 40) diag = diag.substring(0, 40) + "...";
                map.put(diag, rs.getInt("nb"));
            }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return map;
    }

    /** Taux de cloture (consultations cloturees / total). */
    public double getTauxCloture() {
        int total    = getTotalConsultations();
        int cloturees = getConsultationsCloturees();
        return total > 0 ? (double) cloturees / total * 100 : 0;
    }

    /** Nombre moyen d'ordonnances par consultation. */
    public double getMoyenneOrdonnancesParConsultation() {
        String sql = "SELECT AVG(nb) FROM ("
                   + "SELECT COUNT(*) AS nb FROM ordonnances GROUP BY id_consultation) t";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return 0;
    }

    /** Consultations par heure de la journee (0-23). */
    public Map<String, Integer> getConsultationsParHeure() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT HOUR(date_heure_debut) AS heure, COUNT(*) AS nb "
                   + "FROM consultations GROUP BY heure ORDER BY heure";
        try (ResultSet rs = cnx.createStatement().executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getInt("heure") + "h", rs.getInt("nb"));
            }
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        return map;
    }

    /** Nombre de consultations ce mois-ci. */
    public int getConsultationsCeMois() {
        return countQuery(
            "SELECT COUNT(*) FROM consultations "
          + "WHERE MONTH(date_heure_debut) = MONTH(CURDATE()) "
          + "AND YEAR(date_heure_debut) = YEAR(CURDATE())");
    }
}

package pro.revive.services.ServicesMed;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service auxiliaire pour les admissions.
 * Utilise la connexion singleton — PAS de try-with-resources sur getCnx()
 * pour ne pas fermer la connexion partagée.
 */
public class AdmissionService {

    /**
     * Retourne la liste des admissions actives formatées "id - Prénom Nom".
     * Utilisé pour alimenter la ComboBox du formulaire de consultation.
     * Récupère toutes les admissions non terminées (Critique, Urgent, Active, etc.)
     */
    public static List<String> getAllActiveAdmissions() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT a.id_admission, p.prenom, p.nom "
                   + "FROM admissions a "
                   + "JOIN patients p ON p.id_patient = a.id_patient "
                   + "WHERE a.statut NOT IN ('Terminé', 'Sortie', 'Décédé') "
                   + "ORDER BY a.id_admission DESC";
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id_admission");
                String label = id + " - " + rs.getString("prenom") + " " + rs.getString("nom");
                result.add(label);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("[AdmissionService] getAllActiveAdmissions : " + e.getMessage());
        }
        return result;
    }

    /**
     * Retourne les détails d'une admission (infos patient + motif).
     * Colonnes réelles : motif_admission, date_admission.
     */
    public static Map<String, Object> getAdmissionDetails(int idAdmission) {
        Map<String, Object> details = new LinkedHashMap<>();
        String sql = "SELECT p.nom, p.prenom, p.date_naissance, "
                   + "a.motif_admission AS motif, a.statut "
                   + "FROM admissions a "
                   + "JOIN patients p ON p.id_patient = a.id_patient "
                   + "WHERE a.id_admission = ?";
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                details.put("nom",           rs.getString("nom"));
                details.put("prenom",        rs.getString("prenom"));
                details.put("dateNaissance", rs.getDate("date_naissance"));
                details.put("motif",         rs.getString("motif"));
                details.put("statut",        rs.getString("statut"));
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("[AdmissionService] getAdmissionDetails : " + e.getMessage());
        }
        return details;
    }

    /**
     * Extrait l'id numérique depuis une entrée formatée "id - Nom Patient".
     */
    public static int parseIdFromLabel(String label) {
        if (label == null || label.isBlank()) return -1;
        try {
            return Integer.parseInt(label.split(" - ")[0].trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Méthode de debug pour récupérer toutes les admissions (tous statuts).
     */
    public static List<String> getAllAdmissionsDebug() {
        List<String> result = new ArrayList<>();
        String sql = "SELECT a.id_admission, p.prenom, p.nom, a.statut "
                   + "FROM admissions a "
                   + "JOIN patients p ON p.id_patient = a.id_patient "
                   + "ORDER BY a.id_admission DESC";
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id_admission");
                String statut = rs.getString("statut");
                String label = id + " - " + rs.getString("prenom") + " " + rs.getString("nom") + " (" + statut + ")";
                result.add(label);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("[AdmissionService] getAllAdmissionsDebug : " + e.getMessage());
        }
        return result;
    }
}

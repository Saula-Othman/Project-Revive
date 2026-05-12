package pro.revive.services.ServicesMed;

import pro.revive.entities.EntitiesMed.TriagePatient;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service pour récupérer les données de triage depuis la table `triage`
 * avec les informations patient associées.
 */
public class TriageDataService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    /**
     * Récupère tous les triages avec les infos patient,
     * triés par date décroissante (les plus récents en premier).
     */
    public List<TriagePatient> getAllTriages() {
        List<TriagePatient> list = new ArrayList<>();
        String sql =
            "SELECT t.*, " +
            "  pt.nom AS nom_patient, pt.prenom AS prenom_patient " +
            "FROM triage t " +
            "LEFT JOIN admissions a  ON a.id_admission = t.id_admission " +
            "LEFT JOIN patients pt   ON pt.id_patient  = a.id_patient " +
            "ORDER BY t.date_heure_triage DESC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("[TriageDataService] Erreur getAllTriages: " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère les triages critiques et urgents uniquement.
     */
    public List<TriagePatient> getTriagesCritiquesEtUrgents() {
        List<TriagePatient> list = new ArrayList<>();
        String sql =
            "SELECT t.*, " +
            "  pt.nom AS nom_patient, pt.prenom AS prenom_patient " +
            "FROM triage t " +
            "LEFT JOIN admissions a  ON a.id_admission = t.id_admission " +
            "LEFT JOIN patients pt   ON pt.id_patient  = a.id_patient " +
            "WHERE t.niveau_final IN (1, 2) " +
            "   OR t.patient_state IN ('CRITIQUE', 'URGENT') " +
            "ORDER BY t.score_calcule DESC, t.date_heure_triage DESC";
        try {
            ResultSet rs = cnx.createStatement().executeQuery(sql);
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("[TriageDataService] Erreur getCritiques: " + e.getMessage());
        }
        return list;
    }

    /**
     * Récupère le dernier triage pour une admission donnée.
     */
    public TriagePatient getByAdmission(int idAdmission) {
        String sql =
            "SELECT t.*, " +
            "  pt.nom AS nom_patient, pt.prenom AS prenom_patient " +
            "FROM triage t " +
            "LEFT JOIN admissions a  ON a.id_admission = t.id_admission " +
            "LEFT JOIN patients pt   ON pt.id_patient  = a.id_patient " +
            "WHERE t.id_admission = ? " +
            "ORDER BY t.date_heure_triage DESC LIMIT 1";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.out.println("[TriageDataService] Erreur getByAdmission: " + e.getMessage());
        }
        return null;
    }

    /**
     * Compte les patients par niveau.
     * niveau_final est stocké comme INTEGER (1=CRITIQUE, 2=URGENT, 3=MODERE, 4=STABLE, 5=NON_URGENT)
     */
    public int countByNiveau(String niveau) {
        int niveauInt = niveauStringToInt(niveau);
        String sql = "SELECT COUNT(*) FROM triage WHERE niveau_final = ? OR patient_state = ?";
        try {
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, niveauInt);
            ps.setString(2, niveau.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("[TriageDataService] Erreur countByNiveau: " + e.getMessage());
        }
        return 0;
    }

    /** Convertit un niveau string en entier pour la BDD */
    private int niveauStringToInt(String niveau) {
        if (niveau == null) return 5;
        switch (niveau.toUpperCase()) {
            case "CRITIQUE":   return 1;
            case "URGENT":     return 2;
            case "MODERE":
            case "MODÉRÉ":     return 3;
            case "STABLE":     return 4;
            default:           return 5;
        }
    }

    /** Convertit un entier BDD en libellé niveau */
    private String niveauIntToString(int n) {
        switch (n) {
            case 1: return "CRITIQUE";
            case 2: return "URGENT";
            case 3: return "MODERE";
            case 4: return "STABLE";
            default: return "NON_URGENT";
        }
    }

    private TriagePatient map(ResultSet rs) throws SQLException {
        TriagePatient t = new TriagePatient();
        t.setIdTriage(rs.getInt("id_triage"));
        t.setIdAdmission(rs.getInt("id_admission"));

        try { t.setNomPatient(rs.getString("nom_patient")); }     catch (SQLException ignored) {}
        try { t.setPrenomPatient(rs.getString("prenom_patient")); } catch (SQLException ignored) {}

        try { t.setTaSys(rs.getDouble("constantes_ta_sys")); }    catch (SQLException ignored) {}
        try { t.setTaDia(rs.getDouble("constantes_ta_dia")); }    catch (SQLException ignored) {}
        try { t.setPoids(rs.getDouble("constantes_pouls")); }     catch (SQLException ignored) {}
        try { t.setTemperature(rs.getDouble("constantes_temperature")); } catch (SQLException ignored) {}
        try { t.setSpo2(rs.getDouble("spo2")); }                  catch (SQLException ignored) {}
        try { t.setGlycemie(rs.getDouble("glycemie")); }          catch (SQLException ignored) {}
        try { t.setScoreDouleur(rs.getDouble("score_douleur")); } catch (SQLException ignored) {}
        try { t.setGcsScore(rs.getInt("gcs_score")); }            catch (SQLException ignored) {}
        try { t.setFrequenceRespiratoire(rs.getInt("frequence_respiratoire")); } catch (SQLException ignored) {}

        try { t.setSymptomes(rs.getString("symptomes")); }        catch (SQLException ignored) {}
        try { t.setScoreCalcule(rs.getInt("score_calcule")); }    catch (SQLException ignored) {}
        try { t.setNiveauAuto(rs.getString("niveau_auto")); }     catch (SQLException ignored) {}
        // niveau_final est stocké comme INTEGER — on le convertit en libellé lisible
        try {
            int nf = rs.getInt("niveau_final");
            if (!rs.wasNull()) {
                t.setNiveauFinal(niveauIntToString(nf));
            } else {
                String nfStr = rs.getString("niveau_final");
                if (nfStr != null && !nfStr.isEmpty()) t.setNiveauFinal(nfStr);
            }
        } catch (SQLException ignored) {}
        try { t.setAnalyseAuto(rs.getString("analyse_auto")); }   catch (SQLException ignored) {}
        try { t.setPatientState(rs.getString("patient_state")); } catch (SQLException ignored) {}

        try {
            Timestamp ts = rs.getTimestamp("date_heure_triage");
            if (ts != null) t.setDateHeureTriage(
                ts.toLocalDateTime().format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        } catch (SQLException ignored) {
        }
        return t;
    }
}

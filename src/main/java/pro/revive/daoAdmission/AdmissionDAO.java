package pro.revive.daoAdmission;

import pro.revive.entities.EntitiesAdmission.Admission;
import pro.revive.entities.EntitiesAdmission.Patient;
import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdmissionDAO {

    private static final String MODULE_1_FILTER = "a.agent_accueil_id IS NOT NULL";
    private static final String ACTIVE_PATIENT_FILTER = "(p.actif = 1 OR p.actif IS NULL)";

    // Colonnes EXACTES de la table `admissions` dans la BDD :
    //   id_admission, id_patient, date_admission, mode_arrivee,
    //   motif_admission, statut, priorite_initiale, agent_accueil_id,
    //   notes, ambulance_id, patient_inconnu
    //
    // ⚠️ PAS de colonne "actif" dans admissions (elle existe dans patients, pas ici)
    // ⚠️ La colonne date s'appelle "date_admission" (pas "date_heure_arrivee")
    // ⚠️ Le motif s'appelle "motif_admission" (pas "motif_consultation")

    public int save(Admission a) throws SQLException {
        String sql = "INSERT INTO admissions (id_patient, date_admission, mode_arrivee, motif_admission, " +
                "statut, priorite_initiale, agent_accueil_id, notes, ambulance_id, patient_inconnu) " +
                "VALUES (?,NOW(),?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1,    a.getPatientId());
            stmt.setString(2, a.getModeArrivee());
            stmt.setString(3, a.getMotifAdmission());
            stmt.setString(4, a.getStatut());
            stmt.setString(5, a.getPrioriteInitiale());
            stmt.setInt(6,    a.getAgentAccueilId());
            stmt.setString(7, a.getNotes());
            if (a.getAmbulanceId() != null) stmt.setInt(8, a.getAmbulanceId());
            else                            stmt.setNull(8, Types.INTEGER);
            stmt.setBoolean(9, a.isPatientInconnu());
            stmt.executeUpdate();
            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public void update(Admission a) throws SQLException {
        String sql = "UPDATE admissions SET mode_arrivee=?, motif_admission=?, statut=?, " +
                "priorite_initiale=?, notes=? WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getModeArrivee());
            stmt.setString(2, a.getMotifAdmission());
            stmt.setString(3, a.getStatut());
            stmt.setString(4, a.getPrioriteInitiale());
            stmt.setString(5, a.getNotes());
            stmt.setInt(6,    a.getId());
            stmt.executeUpdate();
        }
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE admissions SET statut=? WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut);
            stmt.setInt(2,    id);
            stmt.executeUpdate();
        }
    }

    // -------------------------------------------------------------------------
    // La colonne "actif" n'existe PAS dans admissions.
    // Pour simuler la désactivation, on utilise le statut "Sorti".
    // Cette méthode met à jour le statut en conséquence.
    // -------------------------------------------------------------------------
    public void updateActif(int id, boolean actif) throws SQLException {
        // actif=false → statut "Sorti"  |  actif=true → statut "En attente triage"
        String nouveauStatut = actif ? "En attente triage" : "Sorti";
        updateStatut(id, nouveauStatut);
    }

    public List<Admission> findByPatient(int patientId) throws SQLException {
        List<Admission> list = new ArrayList<>();
        String sql = "SELECT a.*, p.nom, p.prenom FROM admissions a " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "WHERE a.id_patient = ? AND " + ACTIVE_PATIENT_FILTER + " ORDER BY a.date_admission DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<Admission> findTodayAdmissions() throws SQLException {
        List<Admission> list = new ArrayList<>();
        String sql = "SELECT a.*, p.nom, p.prenom FROM admissions a " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "WHERE DATE(a.date_admission) = CURDATE() AND " + MODULE_1_FILTER +
                " AND " + ACTIVE_PATIENT_FILTER +
                " ORDER BY a.date_admission DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Admission> findAll() throws SQLException {
        List<Admission> list = new ArrayList<>();
        String sql = "SELECT a.*, p.nom, p.prenom FROM admissions a " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "WHERE " + MODULE_1_FILTER + " AND " + ACTIVE_PATIENT_FILTER + " " +
                "ORDER BY a.date_admission DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public int countToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admissions a " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "WHERE DATE(a.date_admission) = CURDATE() AND " + MODULE_1_FILTER +
                " AND " + ACTIVE_PATIENT_FILTER;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countWaiting() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admissions a " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "WHERE a.statut = 'En attente triage' AND " + MODULE_1_FILTER +
                " AND " + ACTIVE_PATIENT_FILTER;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM admissions WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    private Admission mapResultSet(ResultSet rs) throws SQLException {
        Admission a = new Admission();
        a.setId(rs.getInt("id_admission"));
        a.setPatientId(rs.getInt("id_patient"));
        a.setModeArrivee(rs.getString("mode_arrivee"));
        a.setMotifAdmission(rs.getString("motif_admission"));       // ← colonne réelle
        a.setStatut(rs.getString("statut"));
        try { a.setPrioriteInitiale(rs.getString("priorite_initiale")); } catch (SQLException ignored) {}
        try { a.setAgentAccueilId(rs.getInt("agent_accueil_id")); }      catch (SQLException ignored) {}
        try { a.setNotes(rs.getString("notes")); }                        catch (SQLException ignored) {}
        try {
            int ambId = rs.getInt("ambulance_id");
            if (!rs.wasNull()) a.setAmbulanceId(ambId);
        } catch (SQLException ignored) {}
        try { a.setPatientInconnu(rs.getBoolean("patient_inconnu")); }   catch (SQLException ignored) {}

        // Pas de colonne "actif" dans admissions.
        // On déduit l'état actif depuis le statut : "Sorti" = désactivé.
        String statut = a.getStatut();
        a.setActif(statut == null || !statut.equals("Sorti"));

        Timestamp ts = rs.getTimestamp("date_admission");               // ← colonne réelle
        if (ts != null) a.setDateAdmission(ts.toLocalDateTime());

        // Infos patient embarquées via le JOIN
        Patient p = new Patient();
        p.setId(rs.getInt("id_patient"));
        try { p.setNom(rs.getString("nom")); }       catch (SQLException ignored) {}
        try { p.setPrenom(rs.getString("prenom")); } catch (SQLException ignored) {}
        a.setPatient(p);
        return a;
    }
}

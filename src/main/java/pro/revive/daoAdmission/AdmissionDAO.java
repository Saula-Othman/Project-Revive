package pro.revive.daoAdmission;

import pro.revive.entities.EntitiesAdmission.Admission;
import pro.revive.entities.EntitiesAdmission.Patient;
import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AdmissionDAO {

    // Real DB column names:
    //   PK  : id_admission   (not "id")
    //   FK  : id_patient     (not "patient_id")
    //   date: date_heure_arrivee  (not "date_admission")
    //   motif: motif_consultation (not "motif_admission")

    public int save(Admission a) throws SQLException {
        String sql = "INSERT INTO admissions (id_patient, date_heure_arrivee, mode_arrivee, motif_consultation, statut, " +
                     "priorite_initiale, agent_accueil_id, notes, ambulance_id, patient_inconnu, actif) " +
                     "VALUES (?,NOW(),?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, a.getPatientId());
            stmt.setString(2, a.getModeArrivee());
            stmt.setString(3, a.getMotifAdmission());
            stmt.setString(4, a.getStatut());
            stmt.setString(5, a.getPrioriteInitiale());
            stmt.setInt(6, a.getAgentAccueilId());
            stmt.setString(7, a.getNotes());
            if (a.getAmbulanceId() != null) stmt.setInt(8, a.getAmbulanceId());
            else stmt.setNull(8, Types.INTEGER);
            stmt.setBoolean(9, a.isPatientInconnu());
            stmt.setBoolean(10, true);
            stmt.executeUpdate();
            try (ResultSet gk = stmt.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public void update(Admission a) throws SQLException {
        String sql = "UPDATE admissions SET mode_arrivee=?, motif_consultation=?, statut=?, priorite_initiale=?, notes=? " +
                     "WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, a.getModeArrivee());
            stmt.setString(2, a.getMotifAdmission());
            stmt.setString(3, a.getStatut());
            stmt.setString(4, a.getPrioriteInitiale());
            stmt.setString(5, a.getNotes());
            stmt.setInt(6, a.getId());
            stmt.executeUpdate();
        }
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE admissions SET statut=? WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, statut);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public void updateActif(int id, boolean actif) throws SQLException {
        String sql = "UPDATE admissions SET actif=? WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, actif);
            stmt.setInt(2, id);
            stmt.executeUpdate();
        }
    }

    public List<Admission> findByPatient(int patientId) throws SQLException {
        List<Admission> list = new ArrayList<>();
        String sql = "SELECT a.*, p.nom, p.prenom FROM admissions a " +
                     "JOIN patients p ON a.id_patient = p.id_patient " +
                     "WHERE a.id_patient = ? ORDER BY a.date_heure_arrivee DESC";
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
                     "WHERE DATE(a.date_heure_arrivee) = CURDATE() ORDER BY a.date_heure_arrivee DESC";
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
                     "ORDER BY a.date_heure_arrivee DESC LIMIT 100";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public int countToday() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admissions WHERE DATE(date_heure_arrivee) = CURDATE()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countWaiting() throws SQLException {
        String sql = "SELECT COUNT(*) FROM admissions WHERE statut = 'En attente triage'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    private Admission mapResultSet(ResultSet rs) throws SQLException {
        Admission a = new Admission();
        a.setId(rs.getInt("id_admission"));
        a.setPatientId(rs.getInt("id_patient"));
        a.setModeArrivee(rs.getString("mode_arrivee"));
        a.setMotifAdmission(rs.getString("motif_consultation"));
        a.setStatut(rs.getString("statut"));
        // New columns added by migration — tolerate absence gracefully
        try { a.setPrioriteInitiale(rs.getString("priorite_initiale")); } catch (SQLException ignored) {}
        try { a.setAgentAccueilId(rs.getInt("agent_accueil_id")); }      catch (SQLException ignored) {}
        try { a.setNotes(rs.getString("notes")); }                        catch (SQLException ignored) {}
        try {
            int ambId = rs.getInt("ambulance_id");
            if (!rs.wasNull()) a.setAmbulanceId(ambId);
        } catch (SQLException ignored) {}
        try { a.setPatientInconnu(rs.getBoolean("patient_inconnu")); }   catch (SQLException ignored) {}
        try { a.setActif(rs.getBoolean("actif")); } catch (SQLException ignored) { a.setActif(true); }
        Timestamp ts = rs.getTimestamp("date_heure_arrivee");
        if (ts != null) a.setDateAdmission(ts.toLocalDateTime());
        // Embedded patient info
        Patient p = new Patient();
        p.setId(rs.getInt("id_patient"));
        try {
            p.setNom(rs.getString("nom"));
            p.setPrenom(rs.getString("prenom"));
        } catch (SQLException ignored) {}
        a.setPatient(p);
        return a;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM admissions WHERE id_admission=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}

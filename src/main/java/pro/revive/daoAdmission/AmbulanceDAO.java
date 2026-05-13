package pro.revive.daoAdmission;

import pro.revive.entities.EntitiesAdmission.AmbulanceSuivi;
import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AmbulanceDAO {

    public List<AmbulanceSuivi> findActiveAmbulances() throws SQLException {
        List<AmbulanceSuivi> list = new ArrayList<>();
        String sql = "SELECT * FROM ambulance_suivi WHERE statut = 'En route' ORDER BY eta_minutes ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public List<AmbulanceSuivi> findAll() throws SQLException {
        List<AmbulanceSuivi> list = new ArrayList<>();
        String sql = "SELECT * FROM ambulance_suivi ORDER BY date_mise_a_jour DESC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    private AmbulanceSuivi mapResultSet(ResultSet rs) throws SQLException {
        AmbulanceSuivi a = new AmbulanceSuivi();
        a.setId(rs.getInt("id"));
        a.setMatricule(rs.getString("matricule"));
        a.setLatitude(rs.getDouble("latitude"));
        a.setLongitude(rs.getDouble("longitude"));
        a.setEtaMinutes(rs.getInt("eta_minutes"));
        a.setNiveauUrgence(rs.getString("niveau_urgence"));
        a.setPatientInfoProvisoire(rs.getString("patient_info_provisoire"));
        a.setStatut(rs.getString("statut"));
        Timestamp ts = rs.getTimestamp("date_mise_a_jour");
        if (ts != null) a.setDateMiseAJour(ts.toLocalDateTime());
        Timestamp td = rs.getTimestamp("date_depart");
        if (td != null) a.setDateDepart(td.toLocalDateTime());
        Timestamp ta = rs.getTimestamp("date_arrivee_prevue");
        if (ta != null) a.setDateArriveePrevue(ta.toLocalDateTime());
        int pid = rs.getInt("personnel_id");
        if (!rs.wasNull()) a.setPersonnelId(pid);
        int aid = rs.getInt("admission_id");
        if (!rs.wasNull()) a.setAdmissionId(aid);
        return a;
    }
}

package pro.revive.daoAdmission;

import pro.revive.entities.EntitiesAdmission.HistoriquePatient;
import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistoriqueDAO {

    /**
     * Lit depuis la vue v_historique_patient_complet qui fusionne
     * historique_patient + consultations + ordonnances + examens.
     * Fallback sur findByPatient() si la vue n'existe pas encore.
     */
    public List<HistoriquePatient> findAllByPatient(int patientId) throws SQLException {
        List<HistoriquePatient> list = new ArrayList<>();
        String sql = "SELECT * FROM v_historique_patient_complet WHERE patient_id = ? ORDER BY date_consultation DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
            return list;
        } catch (SQLException e) {
            // Vue non disponible — fallback sur table directe
            return findByPatient(patientId);
        }
    }

    public List<HistoriquePatient> findByPatient(int patientId) throws SQLException {
        List<HistoriquePatient> list = new ArrayList<>();
        // Try view first, fallback to direct table
        String sql = "SELECT * FROM historique_patient WHERE patient_id = ? ORDER BY date_consultation DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapResultSet(rs));
            }
        }
        return list;
    }

    public void createHistoriqueForAdmission(int patientId, int admissionId, String motif) throws SQLException {
        HistoriquePatient h = new HistoriquePatient();
        h.setPatientId(patientId);
        h.setAdmissionId(admissionId);
        h.setDateConsultation(java.time.LocalDate.now());
        h.setTypeDocument("Compte-rendu");
        h.setTitre("Admission aux urgences");
        h.setContenu("Motif: " + motif);
        h.setEtablissement("Service des Urgences");
        h.setSource("LOCAL");
        save(h);
    }

    public void save(HistoriquePatient h) throws SQLException {
        String sql = "INSERT INTO historique_patient (patient_id, admission_id, date_consultation, " +
            "type_document, titre, contenu, medecin_nom, etablissement, source) VALUES (?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, h.getPatientId());
            if (h.getAdmissionId() != null) stmt.setInt(2, h.getAdmissionId());
            else stmt.setNull(2, Types.INTEGER);
            stmt.setObject(3, h.getDateConsultation());
            stmt.setString(4, h.getTypeDocument());
            stmt.setString(5, h.getTitre());
            stmt.setString(6, h.getContenu());
            stmt.setString(7, h.getMedecinNom());
            stmt.setString(8, h.getEtablissement());
            stmt.setString(9, h.getSource() != null ? h.getSource() : "LOCAL");
            stmt.executeUpdate();
        }
    }

    private HistoriquePatient mapResultSet(ResultSet rs) throws SQLException {
        HistoriquePatient h = new HistoriquePatient();
        h.setId(rs.getInt("id"));
        h.setPatientId(rs.getInt("patient_id"));
        // admission_id peut être NULL
        int admId = rs.getInt("admission_id");
        if (!rs.wasNull()) h.setAdmissionId(admId);
        h.setTypeDocument(rs.getString("type_document"));
        h.setTitre(rs.getString("titre"));
        h.setContenu(rs.getString("contenu"));
        h.setMedecinNom(rs.getString("medecin_nom"));
        h.setEtablissement(rs.getString("etablissement"));
        h.setSource(rs.getString("source"));
        Date d = rs.getDate("date_consultation");
        if (d != null) h.setDateConsultation(d.toLocalDate());
        Timestamp ts = rs.getTimestamp("date_import");
        if (ts != null) h.setDateImport(ts.toLocalDateTime());
        return h;
    }
}

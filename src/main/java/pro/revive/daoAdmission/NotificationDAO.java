package pro.revive.daoAdmission;

import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;

public class NotificationDAO {

    public void sendNotification(String sourceModule, String cibleModule, String type,
                                  String titre, String message, Integer patientId, Integer admissionId) throws SQLException {
        sendNotification(sourceModule, cibleModule, type, titre, message, patientId, admissionId, null);
    }

    public void sendNotification(String sourceModule, String cibleModule, String type,
                                  String titre, String message, Integer patientId, Integer admissionId,
                                  Integer ambulanceId) throws SQLException {
        String sql = "INSERT INTO notifications (source_module, cible_module, type_notif, titre, message, patient_id, admission_id, ambulance_id) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sourceModule);
            stmt.setString(2, cibleModule);
            stmt.setString(3, type);
            stmt.setString(4, titre);
            stmt.setString(5, message);
            if (patientId != null) stmt.setInt(6, patientId); else stmt.setNull(6, Types.INTEGER);
            if (admissionId != null) stmt.setInt(7, admissionId); else stmt.setNull(7, Types.INTEGER);
            if (ambulanceId != null) stmt.setInt(8, ambulanceId); else stmt.setNull(8, Types.INTEGER);
            stmt.executeUpdate();
        }
    }
}

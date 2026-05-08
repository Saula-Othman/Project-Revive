package pro.revive.services.ServicesUser;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ShiftService {

    public static final String MATIN = "Matin (06h-14h)";
    public static final String SOIR  = "Soir (14h-22h)";
    public static final String NUIT  = "Nuit (22h-06h)";

    private Connection getConn() {
        return MyConnection.getInstance().getCnx();
    }

    /** Add a shift for an agent. */
    public void addShift(int idPersonnel, LocalDate date, String typeShift) {
        String sql = "INSERT INTO shifts (id_personnel, date_shift, type_shift) VALUES (?, ?, ?)";
        try {
            Connection conn = getConn();
            if (conn == null) return;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idPersonnel);
            pst.setDate(2, Date.valueOf(date));
            pst.setString(3, typeShift);
            pst.executeUpdate();
            System.out.println("Shift ajoute pour agent id=" + idPersonnel);
        } catch (SQLException e) {
            System.err.println("addShift error: " + e.getMessage());
        }
    }

    /** Delete a shift by id. */
    public void deleteShift(int shiftId) {
        String sql = "DELETE FROM shifts WHERE id = ?";
        try {
            Connection conn = getConn();
            if (conn == null) return;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, shiftId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteShift error: " + e.getMessage());
        }
    }

    /** Get all shifts for a specific agent. */
    public List<String[]> getShiftsForAgent(int idPersonnel) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, date_shift, type_shift FROM shifts WHERE id_personnel = ? ORDER BY date_shift ASC";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idPersonnel);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getDate("date_shift").toString(),
                    rs.getString("type_shift")
                });
            }
        } catch (SQLException e) {
            System.err.println("getShiftsForAgent error: " + e.getMessage());
        }
        return list;
    }

    /** Get all shifts for a specific date (all agents). */
    public List<String[]> getShiftsForDate(LocalDate date) {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT s.id, p.nom, p.prenom, p.role, s.type_shift " +
                     "FROM shifts s JOIN personnel p ON s.id_personnel = p.id_personnel " +
                     "WHERE s.date_shift = ? ORDER BY s.type_shift ASC";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setDate(1, Date.valueOf(date));
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                list.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("nom") + " " + rs.getString("prenom"),
                    rs.getString("role"),
                    rs.getString("type_shift")
                });
            }
        } catch (SQLException e) {
            System.err.println("getShiftsForDate error: " + e.getMessage());
        }
        return list;
    }
}

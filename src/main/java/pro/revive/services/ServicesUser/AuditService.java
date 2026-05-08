package pro.revive.services.ServicesUser;

import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AuditService {

    private Connection getConn() {
        return MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════════
    // Log an action with a full snapshot of the agent
    // ══════════════════════════════════════════
    public void log(int idPersonnel, String action, String details, String faitPar, Personne snapshot) {
        String sql = "INSERT INTO audit_log (id_personnel, action, details, fait_par, snapshot) VALUES (?, ?, ?, ?, ?)";
        try {
            Connection conn = getConn();
            if (conn == null) return;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idPersonnel);
            pst.setString(2, action);
            pst.setString(3, details);
            pst.setString(4, faitPar);
            pst.setString(5, snapshot != null ? toJson(snapshot) : null);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("AuditService.log error: " + e.getMessage());
        }
    }

    // Overload without snapshot (for CONNEXION)
    public void log(int idPersonnel, String action, String details, String faitPar) {
        log(idPersonnel, action, details, faitPar, null);
    }

    // ══════════════════════════════════════════
    // Get all logs
    // ══════════════════════════════════════════
    public List<String[]> getLogs() {
        List<String[]> logs = new ArrayList<>();
        String sql = "SELECT a.id, a.id_personnel, p.nom, p.prenom, a.action, a.details, " +
                     "a.fait_par, a.date_action, a.snapshot " +
                     "FROM audit_log a LEFT JOIN personnel p ON a.id_personnel = p.id_personnel " +
                     "ORDER BY a.date_action DESC LIMIT 200";
        try {
            Connection conn = getConn();
            if (conn == null) return logs;
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String nom = rs.getString("nom") != null
                        ? rs.getString("nom") + " " + rs.getString("prenom")
                        : "Agent supprime";
                logs.add(new String[]{
                    String.valueOf(rs.getInt("id")),       // 0: log id
                    nom,                                    // 1: agent name
                    rs.getString("action"),                 // 2: action
                    rs.getString("details"),                // 3: details
                    rs.getString("fait_par"),               // 4: done by
                    rs.getString("date_action"),            // 5: date
                    rs.getString("snapshot") != null ? rs.getString("snapshot") : "" // 6: snapshot
                });
            }
        } catch (SQLException e) {
            System.err.println("AuditService.getLogs error: " + e.getMessage());
        }
        return logs;
    }

    // ══════════════════════════════════════════
    // Revert an action by log id
    // ══════════════════════════════════════════
    public boolean revert(int logId, String faitPar) {
        // Get the log entry
        String sql = "SELECT action, id_personnel, snapshot FROM audit_log WHERE id = ?";
        try {
            Connection conn = getConn();
            if (conn == null) return false;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, logId);
            ResultSet rs = pst.executeQuery();
            if (!rs.next()) return false;

            String action    = rs.getString("action");
            int idPersonnel  = rs.getInt("id_personnel");
            String snapshot  = rs.getString("snapshot");

            switch (action) {
                case "SUPPRESSION":
                    // Restore the deleted agent from snapshot
                    if (snapshot == null || snapshot.isBlank()) return false;
                    Personne p = fromJson(snapshot);
                    if (p == null) return false;
                    restoreAgent(p);
                    log(idPersonnel, "ANNULATION", "Annulation de SUPPRESSION — agent restaure", faitPar);
                    return true;

                case "AJOUT":
                    // Delete the added agent
                    deleteAgent(idPersonnel);
                    log(idPersonnel, "ANNULATION", "Annulation de AJOUT — agent supprime", faitPar);
                    return true;

                case "MODIFICATION":
                    // Restore previous values from snapshot
                    if (snapshot == null || snapshot.isBlank()) return false;
                    Personne prev = fromJson(snapshot);
                    if (prev == null) return false;
                    restoreModification(idPersonnel, prev);
                    log(idPersonnel, "ANNULATION", "Annulation de MODIFICATION — valeurs restaurees", faitPar);
                    return true;

                default:
                    return false;
            }
        } catch (Exception e) {
            System.err.println("AuditService.revert error: " + e.getMessage());
            return false;
        }
    }

    // ── Private helpers ────────────────────────────────────────────

    private void restoreAgent(Personne p) throws SQLException {
        String sql = "INSERT INTO personnel (id_personnel, nom, prenom, role, identifiant, mot_de_passe, " +
                     "date_naissance, telephone, email) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = getConn();
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setInt(1, p.getIdPersonnel());
        pst.setString(2, p.getNom());
        pst.setString(3, p.getPrenom());
        pst.setString(4, p.getRole());
        pst.setString(5, p.getIdentifiant());
        pst.setString(6, p.getMotDePasse());
        pst.setDate(7, p.getDateNaissance() != null ? Date.valueOf(p.getDateNaissance()) : null);
        pst.setString(8, p.getTelephone());
        pst.setString(9, p.getEmail());
        pst.executeUpdate();
    }

    private void deleteAgent(int idPersonnel) throws SQLException {
        Connection conn = getConn();
        PreparedStatement pst = conn.prepareStatement("DELETE FROM personnel WHERE id_personnel = ?");
        pst.setInt(1, idPersonnel);
        pst.executeUpdate();
    }

    private void restoreModification(int idPersonnel, Personne prev) throws SQLException {
        String sql = "UPDATE personnel SET nom=?, prenom=?, role=?, identifiant=?, " +
                     "date_naissance=?, telephone=?, email=? WHERE id_personnel=?";
        Connection conn = getConn();
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, prev.getNom());
        pst.setString(2, prev.getPrenom());
        pst.setString(3, prev.getRole());
        pst.setString(4, prev.getIdentifiant());
        pst.setDate(5, prev.getDateNaissance() != null ? Date.valueOf(prev.getDateNaissance()) : null);
        pst.setString(6, prev.getTelephone());
        pst.setString(7, prev.getEmail());
        pst.setInt(8, idPersonnel);
        pst.executeUpdate();
    }

    // ── JSON serialization (manual, no external lib) ───────────────

    public String toJson(Personne p) {
        return "{" +
            "\"id\":" + p.getIdPersonnel() + "," +
            "\"nom\":\"" + esc(p.getNom()) + "\"," +
            "\"prenom\":\"" + esc(p.getPrenom()) + "\"," +
            "\"role\":\"" + esc(p.getRole()) + "\"," +
            "\"identifiant\":\"" + esc(p.getIdentifiant()) + "\"," +
            "\"motDePasse\":\"" + esc(p.getMotDePasse()) + "\"," +
            "\"dateNaissance\":\"" + (p.getDateNaissance() != null ? p.getDateNaissance().toString() : "") + "\"," +
            "\"telephone\":\"" + esc(p.getTelephone()) + "\"," +
            "\"email\":\"" + esc(p.getEmail()) + "\"" +
            "}";
    }

    private Personne fromJson(String json) {
        try {
            Personne p = new Personne();
            p.setIdPersonnel(Integer.parseInt(extractJson(json, "id")));
            p.setNom(extractJson(json, "nom"));
            p.setPrenom(extractJson(json, "prenom"));
            p.setRole(extractJson(json, "role"));
            p.setIdentifiant(extractJson(json, "identifiant"));
            p.setMotDePasse(extractJson(json, "motDePasse"));
            String dn = extractJson(json, "dateNaissance");
            if (dn != null && !dn.isBlank()) p.setDateNaissance(LocalDate.parse(dn));
            p.setTelephone(extractJson(json, "telephone"));
            p.setEmail(extractJson(json, "email"));
            return p;
        } catch (Exception e) {
            System.err.println("fromJson error: " + e.getMessage());
            return null;
        }
    }

    private String extractJson(String json, String key) {
        // Handles both "key":"value" and "key":number
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            return json.substring(start + 1, end);
        } else {
            int end = json.indexOf(',', start);
            if (end < 0) end = json.indexOf('}', start);
            return json.substring(start, end).trim();
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

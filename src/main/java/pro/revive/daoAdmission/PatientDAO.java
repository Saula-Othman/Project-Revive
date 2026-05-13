package pro.revive.daoAdmission;

import pro.revive.entities.EntitiesAdmission.Patient;
import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDAO {

    // Real DB column names:
    //   PK   : id_patient           (not "id")
    //   secu : num_secu             (not "num_securite_sociale")
    //   phone: telephone_urgence    (not "telephone")
    // New columns added by migration: sexe, adresse, allergies, antecedents,
    //   nationalite, num_cin, contact_urgence_nom, contact_urgence_tel, actif, date_creation

    public List<Patient> findAll() throws SQLException {
        List<Patient> patients = new ArrayList<>();
        // actif column may not exist before migration; fall back to all rows
        String sql = "SELECT * FROM patients ORDER BY nom, prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) patients.add(mapResultSet(rs));
        }
        return patients;
    }

    public Patient findById(int id) throws SQLException {
        String sql = "SELECT * FROM patients WHERE id_patient = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    public List<Patient> search(String query) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        // num_cin and num_secu may not exist before migration — search gracefully
        String sql = "SELECT * FROM patients WHERE " +
                     "(nom LIKE ? OR prenom LIKE ?) ORDER BY nom, prenom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String q = "%" + query + "%";
            stmt.setString(1, q);
            stmt.setString(2, q);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) patients.add(mapResultSet(rs));
            }
        }
        return patients;
    }

    public int save(Patient p) throws SQLException {
        // Core columns that always exist; new columns added conditionally
        String sql = "INSERT INTO patients (nom, prenom, date_naissance, groupe_sanguin, num_secu, telephone_urgence, " +
                     "sexe, adresse, allergies, antecedents, nationalite, num_cin, contact_urgence_nom, contact_urgence_tel) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, p.getNom());
            stmt.setString(2, p.getPrenom());
            stmt.setObject(3, p.getDateNaissance());
            stmt.setString(4, p.getGroupeSanguin());
            stmt.setString(5, p.getNumSecuriteSociale());
            stmt.setString(6, p.getTelephone());
            stmt.setString(7, p.getSexe());
            stmt.setString(8, p.getAdresse());
            stmt.setString(9, p.getAllergies());
            stmt.setString(10, p.getAntecedents());
            stmt.setString(11, p.getNationalite());
            stmt.setString(12, p.getNumCin());
            stmt.setString(13, p.getContactUrgenceNom());
            stmt.setString(14, p.getContactUrgenceTel());
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) return generatedKeys.getInt(1);
            }
        }
        return -1;
    }

    public void update(Patient p) throws SQLException {
        String sql = "UPDATE patients SET nom=?, prenom=?, date_naissance=?, groupe_sanguin=?, num_secu=?, " +
                     "telephone_urgence=?, sexe=?, adresse=?, allergies=?, antecedents=?, nationalite=?, " +
                     "num_cin=?, contact_urgence_nom=?, contact_urgence_tel=? WHERE id_patient=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, p.getNom());
            stmt.setString(2, p.getPrenom());
            stmt.setObject(3, p.getDateNaissance());
            stmt.setString(4, p.getGroupeSanguin());
            stmt.setString(5, p.getNumSecuriteSociale());
            stmt.setString(6, p.getTelephone());
            stmt.setString(7, p.getSexe());
            stmt.setString(8, p.getAdresse());
            stmt.setString(9, p.getAllergies());
            stmt.setString(10, p.getAntecedents());
            stmt.setString(11, p.getNationalite());
            stmt.setString(12, p.getNumCin());
            stmt.setString(13, p.getContactUrgenceNom());
            stmt.setString(14, p.getContactUrgenceTel());
            stmt.setInt(15, p.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        // Soft-delete via actif flag (column added by migration); fallback hard-delete
        try {
            String sql = "UPDATE patients SET actif = 0 WHERE id_patient = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            // actif column not yet added — skip; row remains visible
        }
    }

    public int countAdmissions(int patientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM admissions WHERE id_patient = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    private Patient mapResultSet(ResultSet rs) throws SQLException {
        Patient p = new Patient();
        p.setId(rs.getInt("id_patient"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        Date dn = rs.getDate("date_naissance");
        if (dn != null) p.setDateNaissance(dn.toLocalDate());
        p.setGroupeSanguin(rs.getString("groupe_sanguin"));
        p.setNumSecuriteSociale(rs.getString("num_secu"));
        p.setTelephone(rs.getString("telephone_urgence"));
        // Columns added by migration — tolerate absence
        try { p.setSexe(rs.getString("sexe")); }                           catch (SQLException ignored) {}
        try { p.setAdresse(rs.getString("adresse")); }                     catch (SQLException ignored) {}
        try { p.setAllergies(rs.getString("allergies")); }                 catch (SQLException ignored) {}
        try { p.setAntecedents(rs.getString("antecedents")); }             catch (SQLException ignored) {}
        try { p.setNationalite(rs.getString("nationalite")); }             catch (SQLException ignored) {}
        try { p.setNumCin(rs.getString("num_cin")); }                      catch (SQLException ignored) {}
        try { p.setContactUrgenceNom(rs.getString("contact_urgence_nom")); } catch (SQLException ignored) {}
        try { p.setContactUrgenceTel(rs.getString("contact_urgence_tel")); } catch (SQLException ignored) {}
        try { p.setActif(rs.getBoolean("actif")); } catch (SQLException ignored) { p.setActif(true); }
        try {
            Timestamp ts = rs.getTimestamp("date_creation");
            if (ts != null) p.setDateCreation(ts.toLocalDateTime());
        } catch (SQLException ignored) {}
        return p;
    }
}

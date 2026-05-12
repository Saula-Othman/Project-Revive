package pro.revive.services.ServicesMed;

import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.interfaces.IService;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConsultationService implements IService<Consultation> {

    // Always get a fresh connection — never cache as a field
    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    @Override
    public void addEntity(Consultation c) {
        c.calculerStatutDemande();
        String sql = "INSERT INTO consultations "
                   + "(id_admission, id_personnel_medecin, date_heure_debut, "
                   + "diagnostic, orientation, analyses, imageries, statut_demande) "
                   + "VALUES (?, ?, NOW(), ?, ?, ?, ?, ?)";
        try {
            PreparedStatement ps = getCnx().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, c.getIdAdmission());
            ps.setInt(2, c.getIdPersonnelMedecin());
            ps.setString(3, c.getDiagnostic());
            ps.setString(4, c.getOrientation());
            ps.setString(5, c.getAnalyses());
            ps.setString(6, c.getImageries());
            ps.setString(7, c.getStatutDemande());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) c.setIdConsultation(keys.getInt(1));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void deleteEntity(Consultation c) {
        try {
            PreparedStatement ps1 = getCnx().prepareStatement("DELETE FROM ordonnances WHERE id_consultation=?");
            ps1.setInt(1, c.getIdConsultation()); ps1.executeUpdate();
            PreparedStatement ps2 = getCnx().prepareStatement("DELETE FROM consultations WHERE id_consultation=?");
            ps2.setInt(1, c.getIdConsultation()); ps2.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    @Override
    public void updateEntity(int id, Consultation c) {
        c.calculerStatutDemande();
        String sql = "UPDATE consultations "
                   + "SET diagnostic=?, orientation=?, analyses=?, imageries=?, statut_demande=? "
                   + "WHERE id_consultation=?";
        try {
            PreparedStatement ps = getCnx().prepareStatement(sql);
            ps.setString(1, c.getDiagnostic());
            ps.setString(2, c.getOrientation());
            ps.setString(3, c.getAnalyses());
            ps.setString(4, c.getImageries());
            ps.setString(5, c.getStatutDemande());
            ps.setInt(6, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void updateExamens(int id, String analyses, String imageries) {
        String statut = (analyses != null && !analyses.trim().isEmpty())
                     || (imageries != null && !imageries.trim().isEmpty())
                     ? "Envoyee" : "Non envoyee";
        String sql = "UPDATE consultations SET analyses=?, imageries=?, statut_demande=? WHERE id_consultation=?";
        try {
            PreparedStatement ps = getCnx().prepareStatement(sql);
            ps.setString(1, analyses  != null && !analyses.trim().isEmpty()  ? analyses  : null);
            ps.setString(2, imageries != null && !imageries.trim().isEmpty() ? imageries : null);
            ps.setString(3, statut);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    public void cloturerConsultation(int id) {
        String sql = "UPDATE consultations SET date_heure_fin=NOW() WHERE id_consultation=?";
        try {
            PreparedStatement ps = getCnx().prepareStatement(sql);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }

    // ── Get patient name — uses a SEPARATE connection call to avoid
    //    interfering with the main ResultSet iteration ──────────────────────
    private String getPatientName(int idAdmission) {
        if (idAdmission <= 0) return null;
        // Strategy 1: standard admissions → patients via id_patient
        try (PreparedStatement ps = getCnx().prepareStatement(
                "SELECT CONCAT(p.prenom,' ',p.nom) AS nom "
              + "FROM admissions a JOIN patients p ON p.id_patient = a.id_patient "
              + "WHERE a.id_admission = ?")) {
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                if (nom != null && !nom.trim().isEmpty() && !nom.trim().equals(" "))
                    return nom.trim();
            }
        } catch (SQLException ignored) {}

        // Strategy 2: patients.id = admissions.patient_id
        try (PreparedStatement ps = getCnx().prepareStatement(
                "SELECT CONCAT(p.prenom,' ',p.nom) AS nom "
              + "FROM admissions a JOIN patients p ON p.id = a.patient_id "
              + "WHERE a.id_admission = ?")) {
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                if (nom != null && !nom.trim().isEmpty()) return nom.trim();
            }
        } catch (SQLException ignored) {}

        // Strategy 3: patients.id = admissions.id_patient
        try (PreparedStatement ps = getCnx().prepareStatement(
                "SELECT CONCAT(p.prenom,' ',p.nom) AS nom "
              + "FROM admissions a JOIN patients p ON p.id = a.id_patient "
              + "WHERE a.id_admission = ?")) {
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                if (nom != null && !nom.trim().isEmpty()) return nom.trim();
            }
        } catch (SQLException ignored) {}

        return null;
    }

    @Override
    public List<Consultation> getData() {
        List<Consultation> list = new ArrayList<>();
        String sql = "SELECT c.id_consultation, c.id_admission, c.id_personnel_medecin, "
                   + "c.date_heure_debut, c.date_heure_fin, c.diagnostic, c.orientation, "
                   + "c.analyses, c.imageries, c.statut_demande, c.icd_code, "
                   + "COALESCE(CONCAT(p.prenom,' ',p.nom), '') AS nom_medecin "
                   + "FROM consultations c "
                   + "LEFT JOIN personnel p ON p.id_personnel = c.id_personnel_medecin "
                   + "ORDER BY c.date_heure_debut DESC";
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            // Step 1: collect ALL rows into memory first — ResultSet fully consumed
            while (rs.next()) {
                list.add(mapBase(rs));
            }
        } catch (SQLException e) {
            System.out.println("[ConsultationService] getData: " + e.getMessage());
        }
        // Step 2: resolve patient names AFTER ResultSet is fully closed
        // (opening new statements while iterating a ResultSet closes it in MySQL)
        for (Consultation c : list) {
            String nom = getPatientName(c.getIdAdmission());
            c.setNomPatient(nom != null ? nom : "Admission #" + c.getIdAdmission());
        }
        return list;
    }

    public Consultation getById(int id) {
        Consultation c = null;
        try (PreparedStatement ps = getCnx().prepareStatement(
                "SELECT c.id_consultation, c.id_admission, c.id_personnel_medecin, "
              + "c.date_heure_debut, c.date_heure_fin, c.diagnostic, c.orientation, "
              + "c.analyses, c.imageries, c.statut_demande, c.icd_code, "
              + "COALESCE(CONCAT(p.prenom,' ',p.nom),'') AS nom_medecin "
              + "FROM consultations c "
              + "LEFT JOIN personnel p ON p.id_personnel = c.id_personnel_medecin "
              + "WHERE c.id_consultation=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) c = mapBase(rs);
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        if (c != null) {
            String nom = getPatientName(c.getIdAdmission());
            c.setNomPatient(nom != null ? nom : "Admission #" + c.getIdAdmission());
        }
        return c;
    }

    public List<Consultation> getByAdmission(int idAdmission) {
        List<Consultation> list = new ArrayList<>();
        try (PreparedStatement ps = getCnx().prepareStatement(
                "SELECT c.id_consultation, c.id_admission, c.id_personnel_medecin, "
              + "c.date_heure_debut, c.date_heure_fin, c.diagnostic, c.orientation, "
              + "c.analyses, c.imageries, c.statut_demande, c.icd_code, "
              + "COALESCE(CONCAT(p.prenom,' ',p.nom),'') AS nom_medecin "
              + "FROM consultations c "
              + "LEFT JOIN personnel p ON p.id_personnel = c.id_personnel_medecin "
              + "WHERE c.id_admission=? ORDER BY c.date_heure_debut DESC")) {
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapBase(rs));
        } catch (SQLException e) { System.out.println(e.getMessage()); }
        for (Consultation c : list) {
            String nom = getPatientName(c.getIdAdmission());
            c.setNomPatient(nom != null ? nom : "Admission #" + c.getIdAdmission());
        }
        return list;
    }

    private Consultation mapBase(ResultSet rs) throws SQLException {
        Consultation c = new Consultation();
        c.setIdConsultation(rs.getInt("id_consultation"));
        c.setIdAdmission(rs.getInt("id_admission"));
        c.setIdPersonnelMedecin(rs.getInt("id_personnel_medecin"));
        c.setDiagnostic(rs.getString("diagnostic"));
        c.setOrientation(rs.getString("orientation"));
        Timestamp d = rs.getTimestamp("date_heure_debut");
        if (d != null) c.setDateHeureDebut(d.toLocalDateTime());
        Timestamp f = rs.getTimestamp("date_heure_fin");
        if (f != null) c.setDateHeureFin(f.toLocalDateTime());
        String nomMed = rs.getString("nom_medecin");
        c.setNomMedecin(nomMed != null && !nomMed.trim().isEmpty() ? nomMed.trim() : null);
        try { c.setAnalyses(rs.getString("analyses")); }         catch (SQLException ignored) {}
        try { c.setImageries(rs.getString("imageries")); }       catch (SQLException ignored) {}
        try { c.setStatutDemande(rs.getString("statut_demande")); } catch (SQLException ignored) {}
        try { c.setIcdCode(rs.getString("icd_code")); }          catch (SQLException ignored) {}
        return c;
    }

    private Consultation map(ResultSet rs) throws SQLException {
        return mapBase(rs);
    }
}

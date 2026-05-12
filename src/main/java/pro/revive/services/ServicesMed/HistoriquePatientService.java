package pro.revive.services.ServicesMed;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Feature 6 — Historique complet d'un patient.
 * Recupere toutes les consultations, ordonnances et examens
 * d'un patient a partir de son id_patient.
 */
public class HistoriquePatientService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    public record ConsultationHistorique(
        int           idConsultation,
        String        dateDebut,
        String        dateFin,
        String        diagnostic,
        String        icdCode,
        String        orientation,
        String        nomMedecin,
        String        statutDemande,
        boolean       cloturee,
        List<String>  medicaments,
        int           nbExamens
    ) {}

    public record PatientInfo(
        int    idPatient,
        String nom,
        String prenom,
        String dateNaissance,
        String groupeSanguin,
        String telephone
    ) {}

    /**
     * Recupere les informations du patient.
     */
    public PatientInfo getPatientInfo(int idPatient) {
        String sql = "SELECT * FROM patients WHERE id_patient = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new PatientInfo(
                    rs.getInt("id_patient"),
                    rs.getString("nom"),
                    rs.getString("prenom"),
                    rs.getString("date_naissance") != null ? rs.getString("date_naissance") : "-",
                    rs.getString("groupe_sanguin") != null ? rs.getString("groupe_sanguin") : "-",
                    rs.getString("telephone_urgence") != null ? rs.getString("telephone_urgence") : "-"
                );
            }
        } catch (SQLException e) {
            System.err.println("[HistoriquePatientService] getPatientInfo: " + e.getMessage());
        }
        return null;
    }

    /**
     * Recupere l'historique complet des consultations d'un patient.
     * @param idPatient identifiant du patient
     * @return liste des consultations triees par date decroissante
     */
    public List<ConsultationHistorique> getHistorique(int idPatient) {
        List<ConsultationHistorique> historique = new ArrayList<>();

        String sql = "SELECT c.id_consultation, "
                   + "DATE_FORMAT(c.date_heure_debut, '%d/%m/%Y %H:%i') AS date_debut, "
                   + "DATE_FORMAT(c.date_heure_fin,   '%d/%m/%Y %H:%i') AS date_fin, "
                   + "c.diagnostic, c.icd_code, c.orientation, c.statut_demande, "
                   + "c.date_heure_fin IS NOT NULL AS cloturee, "
                   + "CONCAT(p.prenom, ' ', p.nom) AS nom_medecin "
                   + "FROM consultations c "
                   + "LEFT JOIN admissions a ON a.id_admission = c.id_admission "
                   + "LEFT JOIN personnel p  ON p.id_personnel = c.id_personnel_medecin "
                   + "WHERE a.id_patient = ? "
                   + "ORDER BY c.date_heure_debut DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idPatient);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idConsult = rs.getInt("id_consultation");

                // Recuperer les medicaments de cette consultation
                List<String> meds = getMedicaments(idConsult);

                // Compter les examens
                int nbExamens = countExamens(idConsult);

                historique.add(new ConsultationHistorique(
                    idConsult,
                    rs.getString("date_debut"),
                    rs.getString("date_fin"),
                    rs.getString("diagnostic"),
                    rs.getString("icd_code"),
                    rs.getString("orientation"),
                    rs.getString("nom_medecin"),
                    rs.getString("statut_demande"),
                    rs.getBoolean("cloturee"),
                    meds,
                    nbExamens
                ));
            }
        } catch (SQLException e) {
            System.err.println("[HistoriquePatientService] getHistorique: " + e.getMessage());
        }
        return historique;
    }

    private List<String> getMedicaments(int idConsultation) {
        List<String> meds = new ArrayList<>();
        String sql = "SELECT medicament FROM ordonnances WHERE id_consultation = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idConsultation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) meds.add(rs.getString("medicament"));
        } catch (SQLException e) { /* ignore */ }
        return meds;
    }

    private int countExamens(int idConsultation) {
        String sql = "SELECT COUNT(*) FROM examens_demandes WHERE id_consultation = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idConsultation);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { /* ignore */ }
        return 0;
    }

    /**
     * Recupere l'id_patient a partir d'un id_admission.
     */
    public int getIdPatientFromAdmission(int idAdmission) {
        String sql = "SELECT id_patient FROM admissions WHERE id_admission = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("id_patient");
        } catch (SQLException e) {
            System.err.println("[HistoriquePatientService] getIdPatient: " + e.getMessage());
        }
        return -1;
    }
}

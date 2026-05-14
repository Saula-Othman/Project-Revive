package pro.revive.services.ServicesAdmission;

import pro.revive.entities.EntitiesAdmission.HistoriquePatient;
import pro.revive.utils.UtilesAdmission.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service MDIA - Dossier Medical Inter-etablissements
 *
 * Lit les tables partagees alimentees par les autres modules :
 *   - consultations    (Module 3 - Medecin)
 *   - ordonnances      (Module 3 - Medecin)
 *   - examens_demandes + resultats  (Module 4 - Biologiste)
 */
public class MdiaService {

    public List<HistoriquePatient> importerDossierMdia(String numSecu, String numCin, int patientId) throws Exception {
        List<HistoriquePatient> dossier = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection()) {
            dossier.addAll(readAdmissionsPrecedentes(conn, patientId));
            dossier.addAll(readConsultations(conn, patientId));
            dossier.addAll(readOrdonnances(conn, patientId));
            dossier.addAll(readResultatsExamens(conn, patientId));
        } catch (SQLException e) {
            throw new Exception("Erreur acces base de donnees MDIA: " + e.getMessage(), e);
        }

        // Ne pas simuler de données démo — retourner la liste réelle même si vide
        return dossier;
    }

    private List<HistoriquePatient> readAdmissionsPrecedentes(Connection conn, int patientId) {
        List<HistoriquePatient> list = new ArrayList<>();
        String sql = "SELECT id_admission, date_admission, mode_arrivee, motif_admission, " +
                     "statut, priorite_initiale, notes " +
                     "FROM admissions " +
                     "WHERE id_patient = ? " +
                     "ORDER BY date_admission DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                HistoriquePatient h = new HistoriquePatient();
                h.setPatientId(patientId);
                h.setAdmissionId(rs.getInt("id_admission"));
                h.setTypeDocument("Compte-rendu");
                h.setTitre("Admission aux urgences");

                StringBuilder contenu = new StringBuilder();
                appendLine(contenu, "Mode d'arrivee", rs.getString("mode_arrivee"));
                appendLine(contenu, "Motif", rs.getString("motif_admission"));
                appendLine(contenu, "Priorite", rs.getString("priorite_initiale"));
                appendLine(contenu, "Statut", rs.getString("statut"));
                appendLine(contenu, "Notes", rs.getString("notes"));
                h.setContenu(contenu.length() > 0 ? contenu.toString().trim() : "Admission precedente");
                h.setEtablissement("Service des Urgences");
                h.setSource("LOCAL");

                Timestamp ts = rs.getTimestamp("date_admission");
                h.setDateConsultation(ts != null ? ts.toLocalDateTime().toLocalDate() : LocalDate.now());
                list.add(h);
            }
        } catch (SQLException e) {
            // Ancienne structure de base ou table indisponible : historique vide.
        }
        return list;
    }

    private void appendLine(StringBuilder sb, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            sb.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private List<HistoriquePatient> readConsultations(Connection conn, int patientId) {
        List<HistoriquePatient> list = new ArrayList<>();
        String sql = "SELECT c.id_consultation, c.date_heure_debut, c.diagnostic, " +
                     "c.orientation, p.nom, p.prenom " +
                     "FROM consultations c " +
                     "JOIN admissions a ON c.id_admission = a.id_admission " +
                     "JOIN patients p ON a.id_patient = p.id_patient " +
                     "WHERE a.id_patient = ? " +
                     "ORDER BY c.date_heure_debut DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                HistoriquePatient h = new HistoriquePatient();
                h.setPatientId(patientId);
                h.setTypeDocument("Consultation");
                h.setTitre("Consultation aux urgences");
                String contenu = "";
                if (rs.getString("diagnostic") != null)
                    contenu += "Diagnostic: " + rs.getString("diagnostic") + "\n";
                if (rs.getString("orientation") != null)
                    contenu += "Orientation: " + rs.getString("orientation");
                h.setContenu(contenu.trim());
                h.setEtablissement("Service des Urgences - Module Medecin");
                h.setSource("MDIA_API");
                Date d = rs.getDate("date_heure_debut");
                h.setDateConsultation(d != null ? d.toLocalDate() : LocalDate.now());
                list.add(h);
            }
        } catch (SQLException e) {
            // Table consultations n'existe pas encore (Module 3 non installe) - ignorer
        }
        return list;
    }

    private List<HistoriquePatient> readOrdonnances(Connection conn, int patientId) {
        List<HistoriquePatient> list = new ArrayList<>();
        String sql = "SELECT o.id_ordonnance, o.date_ordonnance, o.medicament, o.posologie, o.duree_jours " +
                     "FROM ordonnances o " +
                     "JOIN consultations c ON o.id_consultation = c.id_consultation " +
                     "JOIN admissions a ON c.id_admission = a.id_admission " +
                     "WHERE a.id_patient = ? " +
                     "ORDER BY o.date_ordonnance DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                HistoriquePatient h = new HistoriquePatient();
                h.setPatientId(patientId);
                h.setTypeDocument("Ordonnance");
                h.setTitre("Ordonnance medicale");
                String contenu = "";
                if (rs.getString("medicament") != null)
                    contenu += "Medicament: " + rs.getString("medicament") + "\n";
                if (rs.getString("posologie") != null)
                    contenu += "Posologie: " + rs.getString("posologie") + "\n";
                if (rs.getInt("duree_jours") > 0)
                    contenu += "Duree: " + rs.getInt("duree_jours") + " jour(s)";
                h.setContenu(contenu.trim());
                h.setEtablissement("Service des Urgences - Module Medecin");
                h.setSource("MDIA_API");
                Date d = rs.getDate("date_ordonnance");
                h.setDateConsultation(d != null ? d.toLocalDate() : LocalDate.now());
                list.add(h);
            }
        } catch (SQLException e) {
            // Table ordonnances n'existe pas encore - ignorer
        }
        return list;
    }

    private List<HistoriquePatient> readResultatsExamens(Connection conn, int patientId) {
        List<HistoriquePatient> list = new ArrayList<>();
        String sql = "SELECT r.id_resultat, r.date_resultat, r.compte_rendu_texte, r.recommandation, " +
                     "ed.type_examen " +
                     "FROM resultats r " +
                     "JOIN examens_demandes ed ON r.id_demande = ed.id_demande " +
                     "JOIN consultations c ON ed.id_consultation = c.id_consultation " +
                     "JOIN admissions a ON c.id_admission = a.id_admission " +
                     "WHERE a.id_patient = ? " +
                     "ORDER BY r.date_resultat DESC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, patientId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                HistoriquePatient h = new HistoriquePatient();
                h.setPatientId(patientId);
                h.setTypeDocument("Resultat Examen");
                String type = rs.getString("type_examen");
                h.setTitre("Resultat: " + (type != null ? type : "Examen"));
                String contenu = "";
                if (rs.getString("compte_rendu_texte") != null)
                    contenu += rs.getString("compte_rendu_texte") + "\n";
                if (rs.getString("recommandation") != null)
                    contenu += "Recommandation: " + rs.getString("recommandation");
                h.setContenu(contenu.trim());
                h.setEtablissement("Service Biologie/Radiologie - Module 4");
                h.setSource("MDIA_API");
                Date d = rs.getDate("date_resultat");
                h.setDateConsultation(d != null ? d.toLocalDate() : LocalDate.now());
                list.add(h);
            }
        } catch (SQLException e) {
            // Tables examens non disponibles - ignorer
        }
        return list;
    }

    private List<HistoriquePatient> simulerDossierDemo(int patientId) {
        List<HistoriquePatient> list = new ArrayList<>();

        HistoriquePatient h1 = new HistoriquePatient();
        h1.setPatientId(patientId);
        h1.setTypeDocument("Consultation");
        h1.setTitre("Consultation - Douleurs thoraciques");
        h1.setContenu("Diagnostic: Probable angine de poitrine stable.\nObservations: ECG normal. TA 130/80. Prescrit aspirine 100mg/j.\nOrientation: Sortie avec suivi cardiologue.");
        h1.setMedecinNom("Dr. Haddad Sami");
        h1.setEtablissement("Clinique Hannibal, Tunis (DEMO)");
        h1.setSource("MDIA_API");
        h1.setDateConsultation(LocalDate.now().minusMonths(6));
        list.add(h1);

        HistoriquePatient h2 = new HistoriquePatient();
        h2.setPatientId(patientId);
        h2.setTypeDocument("Resultat Examen");
        h2.setTitre("Bilan sanguin complet");
        h2.setContenu("NFS: Hb 12.5 g/dL (anemie legere).\nGlycemie: 1.15 g/L.\nCreatinine: 8.2 mg/L.\nCholesterol: 2.1 g/L. Bilan hepatique normal.");
        h2.setMedecinNom("Laboratoire Central");
        h2.setEtablissement("Hopital Charles Nicolle, Tunis (DEMO)");
        h2.setSource("MDIA_API");
        h2.setDateConsultation(LocalDate.now().minusMonths(3));
        list.add(h2);

        HistoriquePatient h3 = new HistoriquePatient();
        h3.setPatientId(patientId);
        h3.setTypeDocument("Ordonnance");
        h3.setTitre("Ordonnance traitement chronique");
        h3.setContenu("Medicaments:\n- Metformine 500mg: 1cp matin et soir\n- Amlodipine 5mg: 1cp le matin\n- Aspirine 100mg: 1cp le soir\nRenouvellement trimestriel.");
        h3.setMedecinNom("Dr. Mansouri Leila");
        h3.setEtablissement("Centre de Sante El Menzah (DEMO)");
        h3.setSource("MDIA_API");
        h3.setDateConsultation(LocalDate.now().minusMonths(1));
        list.add(h3);

        return list;
    }

    public boolean patientExisteDansMdia(String numSecu, String numCin) {
        return (numSecu != null && !numSecu.trim().isEmpty())
            || (numCin != null && !numCin.trim().isEmpty());
    }
}

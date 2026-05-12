package pro.revive.services.ServicesLabo;

import pro.revive.entities.EntitiesLabo.ConsultationNotif;
import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.interfaces.IExamenCrud;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class Examens_demandesService implements IExamenCrud {

    // Always get a fresh connection — never cache it as a field
    private Connection getConn() {
        return MyConnection.getInstance().getCnx();
    }

    // ── Mapping simple (sans JOIN patient)
    private Examens_demandes map(ResultSet rs) throws SQLException {
        Examens_demandes e = new Examens_demandes();
        e.setIdDemande(rs.getInt("id_demande"));
        e.setIdConsultation(rs.getInt("id_consultation"));
        e.setTypeExamen(rs.getString("type_examen"));
        Timestamp ts = rs.getTimestamp("date_demande");
        if (ts != null) e.setDateDemande(new Date(ts.getTime()));
        e.setStatut(rs.getString("statut"));
        e.setUrgent(rs.getBoolean("urgent"));
        return e;
    }

    // ── Mapping avec nom patient (colonnes nom/prenom disponibles dans le RS)
    private Examens_demandes mapAvecPatient(ResultSet rs) throws SQLException {
        Examens_demandes e = map(rs);
        try {
            String nom    = rs.getString("nom");
            String prenom = rs.getString("prenom");
            if (nom != null && !nom.isBlank() && prenom != null && !prenom.isBlank())
                e.setNomPatient(nom + " " + prenom);
            else if (nom != null && !nom.isBlank())
                e.setNomPatient(nom);
            else {
                // Fallback : recherche directe par id_consultation
                String nomFallback = getNomPatientByConsultation(e.getIdConsultation());
                if (nomFallback != null) e.setNomPatient(nomFallback);
            }
        } catch (SQLException ignored) {}
        return e;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void ajouterExamen(Examens_demandes ex) {
        String sql = "INSERT INTO examens_demandes (id_consultation, type_examen, date_demande, statut, urgent) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, ex.getIdConsultation());
            ps.setString(2, ex.getTypeExamen());
            ps.setTimestamp(3, ex.getDateDemande() != null
                    ? new Timestamp(ex.getDateDemande().getTime())
                    : new Timestamp(System.currentTimeMillis()));
            ps.setString(4, ex.getStatut() != null ? ex.getStatut() : "En attente");
            ps.setBoolean(5, ex.isUrgent());
            ps.executeUpdate();
            System.out.println("✅ Examen ajouté avec succès.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur ajout examen : " + e.getMessage());
        }
    }

    @Override
    public void modifierExamen(Examens_demandes ex) {
        String sql = "UPDATE examens_demandes SET id_consultation=?, type_examen=?, " +
                "date_demande=?, statut=?, urgent=? WHERE id_demande=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, ex.getIdConsultation());
            ps.setString(2, ex.getTypeExamen());
            ps.setTimestamp(3, new Timestamp(ex.getDateDemande().getTime()));
            ps.setString(4, ex.getStatut());
            ps.setBoolean(5, ex.isUrgent());
            ps.setInt(6, ex.getIdDemande());
            ps.executeUpdate();
            System.out.println("✅ Examen modifié.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur modification examen : " + e.getMessage());
        }
    }

    @Override
    public void supprimerExamen(int idDemande) {
        String sql = "DELETE FROM examens_demandes WHERE id_demande=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ps.executeUpdate();
            System.out.println("✅ Examen supprimé.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression : " + e.getMessage());
        }
    }

    /** Supprime plusieurs examens en une seule transaction */
    public void supprimerExamens(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        String sql = "DELETE FROM examens_demandes WHERE id_demande=?";
        try {
            Connection txConn = getConn(); txConn.setAutoCommit(false);
            try (PreparedStatement ps = txConn.prepareStatement(sql)) {
                for (int id : ids) {
                    ps.setInt(1, id);
                    ps.addBatch();
                }
                ps.executeBatch();
                txConn.commit();
                System.out.println("✅ " + ids.size() + " examen(s) supprimé(s).");
            } catch (SQLException e) {
                txConn.rollback();
                throw e;
            } finally {
                txConn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression multiple : " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Examens_demandes getExamenById(int idDemande) {
        // Essai avec JOIN patient
        String sql = "SELECT ed.*, p.nom, p.prenom " +
                "FROM examens_demandes ed " +
                "LEFT JOIN consultations c  ON ed.id_consultation = c.id_consultation " +
                "LEFT JOIN admissions    a  ON c.id_admission     = a.id_admission " +
                "LEFT JOIN patients      p  ON a.id_patient       = p.id_patient " +
                "WHERE ed.id_demande=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapAvecPatient(rs);
        } catch (SQLException e) {
            System.err.println("❌ Erreur getById (avec JOIN) : " + e.getMessage());
            // Fallback sans JOIN
            String sql2 = "SELECT * FROM examens_demandes WHERE id_demande=?";
            try (PreparedStatement ps2 = getConn().prepareStatement(sql2)) {
                ps2.setInt(1, idDemande);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) return map(rs2);
            } catch (SQLException e2) {
                System.err.println("❌ Erreur getById (fallback) : " + e2.getMessage());
            }
        }
        return null;
    }

    @Override
    public List<Examens_demandes> getAllExamens() {
        List<Examens_demandes> list = new ArrayList<>();
        // Essai avec JOIN pour obtenir le nom du patient
        String sql = "SELECT ed.*, p.nom, p.prenom " +
                "FROM examens_demandes ed " +
                "LEFT JOIN consultations c  ON ed.id_consultation = c.id_consultation " +
                "LEFT JOIN admissions    a  ON c.id_admission     = a.id_admission " +
                "LEFT JOIN patients      p  ON a.id_patient       = p.id_patient " +
                "ORDER BY ed.date_demande DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapAvecPatient(rs));
            return list;
        } catch (SQLException e) {
            System.err.println("❌ getAllExamens (avec JOIN) : " + e.getMessage());
        }
        // Fallback sans JOIN
        String sql2 = "SELECT * FROM examens_demandes ORDER BY date_demande DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql2)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("❌ getAllExamens (fallback) : " + e.getMessage());
        }
        return list;
    }

    @Override
    public List<Examens_demandes> getExamensByConsultation(int idConsultation) {
        List<Examens_demandes> list = new ArrayList<>();
        String sql = "SELECT * FROM examens_demandes WHERE id_consultation=? ORDER BY urgent DESC, date_demande DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idConsultation);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("❌ Erreur getByConsultation : " + e.getMessage());
        }
        return list;
    }

    @Override
    public void marquerRealise(int idDemande) {
        String sql = "UPDATE examens_demandes SET statut='Realise' WHERE id_demande=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ps.executeUpdate();
            System.out.println("✅ Examen marqué comme réalisé.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur marquerRealise : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Méthode pour les ComboBox : retourne une map id_consultation → "Nom Prénom"
    // (avec fallback si les tables admissions/patients sont inaccessibles)
    // ─────────────────────────────────────────────────────────────────────────
    public LinkedHashMap<Integer, String> getConsultationsAvecPatients() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        // Récupère TOUTES les consultations avec le nom du patient lié
        // Chaque consultation est une entrée distincte (un patient peut avoir plusieurs consultations)
        String sql = "SELECT c.id_consultation, p.nom, p.prenom " +
                "FROM consultations c " +
                "JOIN admissions a ON c.id_admission = a.id_admission " +
                "JOIN patients   p ON a.id_patient   = p.id_patient " +
                "ORDER BY p.nom, p.prenom, c.id_consultation";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_consultation");
                // Affiche "Nom Prénom (Consultation #X)" pour distinguer les doublons
                String label = rs.getString("nom") + " " + rs.getString("prenom")
                        + "  (Consultation #" + id + ")";
                map.put(id, label);
            }
            if (!map.isEmpty()) return map;
        } catch (SQLException e) {
            System.err.println("❌ getConsultationsAvecPatients (JOIN) : " + e.getMessage());
        }
        // Fallback : consultation sans nom patient
        String[] tables = {"consultations", "consultation"};
        String[] cols   = {"id_consultation", "id"};
        for (int i = 0; i < tables.length; i++) {
            try (Statement st = getConn().createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT " + cols[i] + " FROM " + tables[i] + " ORDER BY " + cols[i])) {
                while (rs.next()) {
                    int id = rs.getInt(1);
                    map.put(id, "Consultation #" + id);
                }
                if (!map.isEmpty()) return map;
            } catch (SQLException ignored) {}
        }
        // Dernier fallback : ids déjà utilisés
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT DISTINCT id_consultation FROM examens_demandes ORDER BY id_consultation")) {
            while (rs.next()) {
                int id = rs.getInt(1);
                map.put(id, "Consultation #" + id);
            }
        } catch (SQLException e) {
            System.err.println("❌ getConsultationsAvecPatients (fallback) : " + e.getMessage());
        }
        return map;
    }

    // Récupère le nom du patient pour une consultation donnée (pour Modifier/Supprimer)
    public String getNomPatientByConsultation(int idConsultation) {
        String sql = "SELECT p.nom, p.prenom " +
                "FROM consultations c " +
                "JOIN admissions a ON c.id_admission = a.id_admission " +
                "JOIN patients   p ON a.id_patient   = p.id_patient " +
                "WHERE c.id_consultation = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idConsultation);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nom") + " " + rs.getString("prenom");
        } catch (SQLException e) {
            System.err.println("❌ getNomPatientByConsultation : " + e.getMessage());
        }
        return null;
    }

    /**
     * Pour la ComboBox de AjouterExamen :
     * retourne uniquement les consultations avec statut_demande = 'Envoyee'
     * sous forme id_consultation → "Nom Prénom  (Consultation #X)"
     */
    public LinkedHashMap<Integer, String> getConsultationsEnvoyeesMap() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();

        // Avec JOIN patient
        String sql =
            "SELECT c.id_consultation, p.nom, p.prenom " +
            "FROM consultations c " +
            "JOIN admissions a ON c.id_admission = a.id_admission " +
            "JOIN patients   p ON a.id_patient   = p.id_patient " +
            "WHERE c.statut_demande = 'Envoyee' " +
            "ORDER BY p.nom, p.prenom, c.id_consultation";

        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_consultation");
                String label = rs.getString("nom") + " " + rs.getString("prenom")
                        + "  (Consultation #" + id + ")";
                map.put(id, label);
            }
            if (!map.isEmpty()) return map;
        } catch (SQLException e) {
            System.err.println("❌ getConsultationsEnvoyeesMap (JOIN) : " + e.getMessage());
        }

        // Fallback sans JOIN
        String sql2 =
            "SELECT id_consultation FROM consultations " +
            "WHERE statut_demande = 'Envoyee' " +
            "ORDER BY id_consultation";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql2)) {
            while (rs.next()) {
                int id = rs.getInt("id_consultation");
                map.put(id, "Consultation #" + id);
            }
        } catch (SQLException e) {
            System.err.println("❌ getConsultationsEnvoyeesMap (fallback) : " + e.getMessage());
        }

        return map;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NOTIFICATIONS : consultations avec statut_demande = 'Envoyee'
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne toutes les consultations dont statut_demande = 'Envoyee',
     * avec le nom/prénom du patient, les analyses, imageries et la date.
     */
    public List<ConsultationNotif> getConsultationsEnvoyees() {
        List<ConsultationNotif> liste = new ArrayList<>();

        // Essai avec JOIN patient complet
        String sql =
            "SELECT c.id_consultation, c.analyses, c.imageries, c.date_heure_debut, " +
            "       p.nom, p.prenom " +
            "FROM consultations c " +
            "JOIN admissions a ON c.id_admission = a.id_admission " +
            "JOIN patients   p ON a.id_patient   = p.id_patient " +
            "WHERE c.statut_demande = 'Envoyee' " +
            "ORDER BY c.date_heure_debut DESC";

        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ConsultationNotif n = new ConsultationNotif();
                n.setIdConsultation(rs.getInt("id_consultation"));
                n.setNomPatient(rs.getString("nom") + " " + rs.getString("prenom"));
                n.setAnalyses(rs.getString("analyses"));
                n.setImageries(rs.getString("imageries"));
                Timestamp ts = rs.getTimestamp("date_heure_debut");
                if (ts != null) n.setDateDemande(new Date(ts.getTime()));
                liste.add(n);
            }
            return liste;
        } catch (SQLException e) {
            System.err.println("❌ getConsultationsEnvoyees (JOIN) : " + e.getMessage());
        }

        // Fallback sans JOIN patient
        String sql2 =
            "SELECT id_consultation, analyses, imageries, date_heure_debut " +
            "FROM consultations " +
            "WHERE statut_demande = 'Envoyee' " +
            "ORDER BY date_heure_debut DESC";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql2)) {
            while (rs.next()) {
                ConsultationNotif n = new ConsultationNotif();
                n.setIdConsultation(rs.getInt("id_consultation"));
                n.setNomPatient("Consultation #" + rs.getInt("id_consultation"));
                n.setAnalyses(rs.getString("analyses"));
                n.setImageries(rs.getString("imageries"));
                Timestamp ts = rs.getTimestamp("date_heure_debut");
                if (ts != null) n.setDateDemande(new Date(ts.getTime()));
                liste.add(n);
            }
        } catch (SQLException e) {
            System.err.println("❌ getConsultationsEnvoyees (fallback) : " + e.getMessage());
        }

        return liste;
    }

    /**
     * Marque une consultation comme reçue par le labo :
     * met statut_demande = 'Reçue' pour qu'elle disparaisse des notifications.
     */
    public void marquerConsultationRecue(int idConsultation) {
        String sql = "UPDATE consultations SET statut_demande = 'Re\u00e7ue' WHERE id_consultation = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setInt(1, idConsultation);
            ps.executeUpdate();
            System.out.println("✅ Consultation #" + idConsultation + " marquée comme reçue.");
        } catch (SQLException e) {
            System.err.println("❌ marquerConsultationRecue : " + e.getMessage());
        }
    }
}
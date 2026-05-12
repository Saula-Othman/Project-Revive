package pro.revive.services.ServicesLabo;

import pro.revive.entities.EntitiesLabo.Resultats;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.*;

public class ResultatService {

    // Always get a fresh connection — never cache it as a field
    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    // ── Detect the actual compte_rendu column name in the resultats table ──
    private String compteRenduCol = null;

    private String getCompteRenduCol() {
        if (compteRenduCol != null) return compteRenduCol;
        try {
            ResultSet cols = getCnx().getMetaData().getColumns(null, null, "resultats", null);
            while (cols.next()) {
                String col = cols.getString("COLUMN_NAME");
                if ("compte_rendu_texte".equalsIgnoreCase(col)) { compteRenduCol = "compte_rendu_texte"; return compteRenduCol; }
                if ("compte_rendu".equalsIgnoreCase(col))        { compteRenduCol = "compte_rendu";       return compteRenduCol; }
            }
        } catch (SQLException ignored) {}
        compteRenduCol = "compte_rendu_texte"; // default
        return compteRenduCol;
    }

    private String readCompteRendu(ResultSet rs) {
        try { return rs.getString(getCompteRenduCol()); } catch (SQLException ignored) {}
        try { return rs.getString("compte_rendu"); }      catch (SQLException ignored) {}
        try { return rs.getString("compte_rendu_texte"); } catch (SQLException ignored) {}
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INSERT
    // ─────────────────────────────────────────────────────────────────────────
    public void ajouter(Resultats r) {
        String col = getCompteRenduCol();
        String sql = "INSERT INTO resultats (id_demande, " + col + ", fichier_joint, etat, score_gravite, niveau_gravite, recommandation) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, r.getIdDemande());
            ps.setString(2, r.getCompteRendu());
            ps.setString(3, r.getFichierJoint() != null ? r.getFichierJoint() : "");
            ps.setString(4, r.getEtat() != null ? r.getEtat() : "Propre");
            ps.setInt(5, r.getScoreGravite());
            ps.setString(6, r.getNiveauGravite() != null ? r.getNiveauGravite() : "Faible");
            ps.setString(7, r.getRecommandation() != null ? r.getRecommandation() : "");
            ps.executeUpdate();
            System.out.println("Résultat ajouté avec succès !");
        } catch (SQLException e) {
            System.out.println("Erreur ajout : " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ComboBox helpers
    // ─────────────────────────────────────────────────────────────────────────
    public LinkedHashMap<Integer, String> getDemandesAvecPatients() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        String sql = "SELECT ed.id_demande, ed.type_examen, p.nom, p.prenom " +
                "FROM examens_demandes ed " +
                "JOIN consultations c ON ed.id_consultation = c.id_consultation " +
                "JOIN admissions    a ON c.id_admission     = a.id_admission " +
                "JOIN patients      p ON a.id_patient       = p.id_patient " +
                "ORDER BY ed.id_demande";
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_demande");
                map.put(id, rs.getString("nom") + " " + rs.getString("prenom") + " — " + rs.getString("type_examen"));
            }
            if (!map.isEmpty()) return map;
        } catch (SQLException e) { System.err.println("❌ getDemandesAvecPatients (JOIN): " + e.getMessage()); }
        // Fallback
        try (Statement st = getCnx().createStatement();
             ResultSet rs = st.executeQuery("SELECT id_demande, type_examen FROM examens_demandes ORDER BY id_demande")) {
            while (rs.next()) map.put(rs.getInt("id_demande"), "Demande #" + rs.getInt("id_demande") + " — " + rs.getString("type_examen"));
        } catch (SQLException e) { System.err.println("❌ getDemandesAvecPatients (fallback): " + e.getMessage()); }
        return map;
    }

    public LinkedHashMap<Integer, String> getDemandesRealisees() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        String sql = "SELECT ed.id_demande, ed.type_examen, p.nom, p.prenom " +
                "FROM examens_demandes ed " +
                "JOIN consultations c ON ed.id_consultation = c.id_consultation " +
                "JOIN admissions    a ON c.id_admission     = a.id_admission " +
                "JOIN patients      p ON a.id_patient       = p.id_patient " +
                "WHERE ed.statut = 'Realise' " +
                "  AND NOT EXISTS (SELECT 1 FROM resultats r WHERE r.id_demande = ed.id_demande) " +
                "ORDER BY ed.id_demande";
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_demande");
                map.put(id, rs.getString("nom") + " " + rs.getString("prenom") + " — " + rs.getString("type_examen"));
            }
            if (!map.isEmpty()) return map;
        } catch (SQLException e) { System.err.println("❌ getDemandesRealisees (JOIN): " + e.getMessage()); }
        String sql2 = "SELECT id_demande, type_examen FROM examens_demandes WHERE statut='Realise' " +
                "AND NOT EXISTS (SELECT 1 FROM resultats r WHERE r.id_demande=examens_demandes.id_demande) ORDER BY id_demande";
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery(sql2)) {
            while (rs.next()) map.put(rs.getInt("id_demande"), "Demande #" + rs.getInt("id_demande") + " — " + rs.getString("type_examen"));
        } catch (SQLException e) { System.err.println("❌ getDemandesRealisees (fallback): " + e.getMessage()); }
        return map;
    }

    public String getNomPatientByDemande(int idDemande) {
        String sql = "SELECT p.nom, p.prenom FROM examens_demandes ed " +
                "JOIN consultations c ON ed.id_consultation=c.id_consultation " +
                "JOIN admissions    a ON c.id_admission=a.id_admission " +
                "JOIN patients      p ON a.id_patient=p.id_patient WHERE ed.id_demande=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, idDemande);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom"), prenom = rs.getString("prenom");
                if (nom != null && prenom != null) return nom + " " + prenom;
                if (nom != null) return nom;
            }
        } catch (SQLException e) { System.err.println("❌ getNomPatientByDemande: " + e.getMessage()); }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────
    public void modifier(Resultats r) {
        String col = getCompteRenduCol();
        String sql = "UPDATE resultats SET id_demande=?, " + col + "=?, fichier_joint=?, score_gravite=?, niveau_gravite=?, recommandation=? WHERE id_resultat=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, r.getIdDemande());
            ps.setString(2, r.getCompteRendu());
            ps.setString(3, r.getFichierJoint());
            ps.setInt(4, r.getScoreGravite());
            ps.setString(5, r.getNiveauGravite() != null ? r.getNiveauGravite() : "Faible");
            ps.setString(6, r.getRecommandation() != null ? r.getRecommandation() : "");
            ps.setInt(7, r.getIdResultat());
            ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur modification: " + e.getMessage()); }
    }

    public void mettreAJourScore(int idResultat, int score, String niveau, String recommandation) {
        String sql = "UPDATE resultats SET score_gravite=?, niveau_gravite=?, recommandation=? WHERE id_resultat=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, score); ps.setString(2, niveau); ps.setString(3, recommandation); ps.setInt(4, idResultat);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Erreur mettreAJourScore: " + e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────
    public void supprimer(int idResultat) {
        try (PreparedStatement ps = getCnx().prepareStatement("DELETE FROM resultats WHERE id_resultat=?")) {
            ps.setInt(1, idResultat); ps.executeUpdate();
        } catch (SQLException e) { System.out.println("Erreur suppression: " + e.getMessage()); }
    }

    public void supprimerResultats(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;
        Connection c = getCnx();
        try {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM resultats WHERE id_resultat=?")) {
                for (int id : ids) { ps.setInt(1, id); ps.addBatch(); }
                ps.executeBatch();
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) {
            System.err.println("❌ Erreur suppression multiple: " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SELECT ALL
    // ─────────────────────────────────────────────────────────────────────────
    public List<Resultats> afficher() {
        List<Resultats> list = new ArrayList<>();
        String sql = "SELECT r.*, p.nom, p.prenom " +
                "FROM resultats r " +
                "LEFT JOIN examens_demandes ed ON r.id_demande       = ed.id_demande " +
                "LEFT JOIN consultations    c  ON ed.id_consultation = c.id_consultation " +
                "LEFT JOIN admissions       a  ON c.id_admission     = a.id_admission " +
                "LEFT JOIN patients         p  ON a.id_patient       = p.id_patient";
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultat(rs));
            return list;
        } catch (SQLException e) { System.out.println("❌ afficher (JOIN): " + e.getMessage()); }
        // Fallback without JOIN
        try (Statement st = getCnx().createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM resultats")) {
            while (rs.next()) list.add(mapResultat(rs));
        } catch (SQLException e) { System.out.println("❌ afficher (fallback): " + e.getMessage()); }
        return list;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SELECT BY ID
    // ─────────────────────────────────────────────────────────────────────────
    public Resultats getById(int idResultat) {
        String sql = "SELECT r.*, p.nom, p.prenom FROM resultats r " +
                "LEFT JOIN examens_demandes ed ON r.id_demande=ed.id_demande " +
                "LEFT JOIN consultations    c  ON ed.id_consultation=c.id_consultation " +
                "LEFT JOIN admissions       a  ON c.id_admission=a.id_admission " +
                "LEFT JOIN patients         p  ON a.id_patient=p.id_patient " +
                "WHERE r.id_resultat=?";
        try (PreparedStatement ps = getCnx().prepareStatement(sql)) {
            ps.setInt(1, idResultat);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultat(rs);
        } catch (SQLException e) { System.out.println("Erreur getById: " + e.getMessage()); }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING — handles both compte_rendu and compte_rendu_texte column names
    // ─────────────────────────────────────────────────────────────────────────
    private Resultats mapResultat(ResultSet rs) throws SQLException {
        Resultats r = new Resultats();
        r.setIdResultat(rs.getInt("id_resultat"));
        r.setIdDemande(rs.getInt("id_demande"));
        r.setCompteRendu(readCompteRendu(rs));
        try { r.setFichierJoint(rs.getString("fichier_joint")); }   catch (SQLException ignored) {}
        try { r.setDateResultat(rs.getTimestamp("date_resultat")); } catch (SQLException ignored) {}
        try { r.setEtat(rs.getString("etat")); }                    catch (SQLException ignored) {}
        try { r.setScoreGravite(rs.getInt("score_gravite")); }      catch (SQLException ignored) {}
        try { r.setNiveauGravite(rs.getString("niveau_gravite")); }  catch (SQLException ignored) {}
        try { r.setRecommandation(rs.getString("recommandation")); } catch (SQLException ignored) {}
        // Patient name
        try {
            String nom = rs.getString("nom"), prenom = rs.getString("prenom");
            if (nom != null && !nom.isBlank() && prenom != null && !prenom.isBlank())
                r.setNomPatient(nom + " " + prenom);
            else if (nom != null && !nom.isBlank())
                r.setNomPatient(nom);
            else {
                String fallback = getNomPatientByDemande(r.getIdDemande());
                if (fallback != null) r.setNomPatient(fallback);
            }
        } catch (SQLException ignored) {}
        return r;
    }

    /** @deprecated Use getDemandesAvecPatients() instead */
    public List<Integer> getIdsDemandeDisponibles() {
        return new ArrayList<>(getDemandesAvecPatients().keySet());
    }
}

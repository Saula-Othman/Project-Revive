package pro.revive.services.ServicesMateriel;

import pro.revive.entities.EntitiesMateriel.MaterielUrgence;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service JDBC — Matériel d'urgence du Module 5.
 * Table matériel : materiel_urgence
 * Table salles   : salles (colonne nom = nom_salle)
 */
public class MaterielService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // JOIN sur nom_salle (colonne réelle dans la table salles)
    private static final String SELECT_BASE =
        "SELECT m.id_materiel, m.id_salle, m.nom, " +
        "       m.date_derniere_maintenance, m.etat, m.quantite, " +
        "       s.nom_salle AS nom_salle " +
        "FROM materiel_urgence m " +
        "LEFT JOIN salles s ON m.id_salle = s.id_salle ";

    // ── Lecture ──────────────────────────────────────────────────────

    public List<MaterielUrgence> findAll() throws SQLException {
        List<MaterielUrgence> list = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(SELECT_BASE + "ORDER BY m.id_materiel")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public MaterielUrgence findById(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_BASE + "WHERE m.id_materiel = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<MaterielUrgence> findBySalle(int idSalle) throws SQLException {
        List<MaterielUrgence> list = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(
                SELECT_BASE + "WHERE m.id_salle = ? ORDER BY m.nom")) {
            ps.setInt(1, idSalle);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Retourne "id - nom_salle" pour alimenter les ComboBox.
     */
    public List<String> getAllSallesForCombo() throws SQLException {
        List<String> list = new ArrayList<>();
        list.add("Réserve");
        String sql = "SELECT id_salle, nom_salle FROM salles ORDER BY nom_salle";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getInt("id_salle") + " - " + rs.getString("nom_salle"));
            }
        }
        return list;
    }

    // ── Écriture ─────────────────────────────────────────────────────

    public void create(MaterielUrgence m) throws SQLException {
        String sql = "INSERT INTO materiel_urgence " +
                     "(id_salle, nom, date_derniere_maintenance, etat, quantite) " +
                     "VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            setIdSalle(ps, 1, m.getIdSalle());
            ps.setString(2, m.getNom());
            ps.setDate(3, m.getDateDerniereMaintenance() != null
                    ? Date.valueOf(m.getDateDerniereMaintenance()) : null);
            ps.setString(4, m.getEtat());
            ps.setInt(5, m.getQuantite());
            ps.executeUpdate();
        }
    }

    public void update(MaterielUrgence m) throws SQLException {
        String sql = "UPDATE materiel_urgence " +
                     "SET id_salle=?, nom=?, date_derniere_maintenance=?, etat=?, quantite=? " +
                     "WHERE id_materiel=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            setIdSalle(ps, 1, m.getIdSalle());
            ps.setString(2, m.getNom());
            ps.setDate(3, m.getDateDerniereMaintenance() != null
                    ? Date.valueOf(m.getDateDerniereMaintenance()) : null);
            ps.setString(4, m.getEtat());
            ps.setInt(5, m.getQuantite());
            ps.setInt(6, m.getIdMateriel());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM materiel_urgence WHERE id_materiel = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ── Utilitaires ──────────────────────────────────────────────────

    private void setIdSalle(PreparedStatement ps, int index, Integer idSalle) throws SQLException {
        if (idSalle == null) ps.setNull(index, Types.INTEGER);
        else                 ps.setInt(index, idSalle);
    }

    private MaterielUrgence mapRow(ResultSet rs) throws SQLException {
        MaterielUrgence m = new MaterielUrgence();
        m.setIdMateriel(rs.getInt("id_materiel"));
        int idSalle = rs.getInt("id_salle");
        m.setIdSalle(rs.wasNull() ? null : idSalle);
        m.setNom(rs.getString("nom"));
        Date d = rs.getDate("date_derniere_maintenance");
        m.setDateDerniereMaintenance(d != null ? d.toLocalDate() : null);
        m.setEtat(rs.getString("etat"));
        m.setQuantite(rs.getInt("quantite"));
        String nomSalle = rs.getString("nom_salle");
        m.setNomSalle(nomSalle != null ? nomSalle : "Réserve");
        return m;
    }
}

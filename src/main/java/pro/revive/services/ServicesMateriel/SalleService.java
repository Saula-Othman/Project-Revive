package pro.revive.services.ServicesMateriel;

import pro.revive.entities.EntitiesMateriel.SallePhysique;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service JDBC — Salles physiques du Module 5.
 *
 * La table "salles" dans cette base a les colonnes du Module 2 :
 *   id_salle | nom_salle | type_salle | capacite_max | nombre_actuel
 *   | statut | niveau_gravite_cible | priorite | patients_en_attente
 *
 * Ce service lit/écrit uniquement les colonnes utiles au Module 5 :
 *   id_salle → id_salle
 *   nom      → nom_salle
 *   type     → type_salle
 *   statut   → statut
 *
 * Pour la création, les colonnes obligatoires sans défaut sont remplies
 * avec des valeurs neutres (capacite_max=0, priorite=0, etc.)
 */
public class SalleService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── Lecture ──────────────────────────────────────────────────────

    public List<SallePhysique> findAll() throws SQLException {
        List<SallePhysique> list = new ArrayList<>();
        String sql = "SELECT id_salle, nom_salle, type_salle, statut, capacite_max, nombre_actuel, localisation " +
                     "FROM salles ORDER BY id_salle";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public SallePhysique findById(int id) throws SQLException {
        String sql = "SELECT id_salle, nom_salle, type_salle, statut, capacite_max, nombre_actuel, localisation " +
                     "FROM salles WHERE id_salle = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ── Écriture ─────────────────────────────────────────────────────

    /**
     * Insère une nouvelle salle.
     * Les colonnes obligatoires du Module 2 sont remplies avec des valeurs neutres.
     */
    public void create(SallePhysique s) throws SQLException {
        String sql = "INSERT INTO salles " +
                     "(nom_salle, type_salle, statut, capacite_max, nombre_actuel, " +
                     " localisation, niveau_gravite_cible, priorite, patients_en_attente) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 0, 0, 0)";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getType());
            ps.setString(3, s.getStatut());
            ps.setInt(4, s.getCapaciteMax());
            ps.setInt(5, s.getNombreActuel());
            ps.setString(6, s.getLocalisation());
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour uniquement les colonnes du Module 5.
     */
    public void update(SallePhysique s) throws SQLException {
        String sql = "UPDATE salles SET nom_salle=?, type_salle=?, statut=?, capacite_max=?, nombre_actuel=?, localisation=? " +
                     "WHERE id_salle=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, s.getNom());
            ps.setString(2, s.getType());
            ps.setString(3, s.getStatut());
            ps.setInt(4, s.getCapaciteMax());
            ps.setInt(5, s.getNombreActuel());
            ps.setString(6, s.getLocalisation());
            ps.setInt(7, s.getIdSalle());
            ps.executeUpdate();
        }
    }

    /**
     * Supprime une salle (détache d'abord le matériel lié).
     */
    public void delete(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE materiel_urgence SET id_salle = NULL WHERE id_salle = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ignored) {}

        try (PreparedStatement ps = cnx.prepareStatement(
                "DELETE FROM salles WHERE id_salle = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Modifie uniquement le statut d'une salle.
     */
    public void changerStatut(int id, String nouveauStatut) throws SQLException {
        String sql = "UPDATE salles SET statut=? WHERE id_salle=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    // ── Utilitaire ───────────────────────────────────────────────────

    private SallePhysique mapRow(ResultSet rs) throws SQLException {
        return new SallePhysique(
                rs.getInt("id_salle"),
                rs.getString("nom_salle"),   // → nom dans l'entité
                rs.getString("type_salle"),  // → type dans l'entité
                rs.getString("statut"),
                rs.getInt("capacite_max"),
                rs.getInt("nombre_actuel"),
                rs.getString("localisation")
        );
    }
}

package pro.revive.services;

import pro.revive.entities.Salle;
import pro.revive.interfaces.IService;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SalleService implements IService<Salle> {

    // Each call borrows a fresh connection from the pool — always use in try-with-resources.
    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════════
    // addEntity — INSERT using Statement (required by teacher)
    // ══════════════════════════════════════════
    @Override
    public void addEntity(Salle salle) {
        String requete = "INSERT INTO salles (nom_salle, type_salle, capacite_max, statut, niveau_gravite_cible, priorite) VALUES ('" +
                esc(salle.getNomSalle()) + "', '" +
                esc(salle.getTypeSalle()) + "', " +
                salle.getCapaciteMax() + ", 'Disponible', " +
                salle.getNiveauGraviteCible() + ", " +
                salle.getPriorite() + ")";
        try (Connection c = getCnx(); Statement st = c.createStatement()) {
            st.executeUpdate(requete);
            System.out.println("Salle ajoutee!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec ajout salle: " + e.getMessage(), e);
        }
    }

    // addEntity2 — INSERT using PreparedStatement (required by teacher)
    public void addEntity2(Salle salle) {
        String requete = "INSERT INTO salles (nom_salle, type_salle, capacite_max, statut, niveau_gravite_cible, priorite) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setString(1, salle.getNomSalle());
            pst.setString(2, salle.getTypeSalle());
            pst.setInt(3, salle.getCapaciteMax());
            pst.setString(4, "Disponible");
            pst.setInt(5, salle.getNiveauGraviteCible());
            pst.setInt(6, salle.getPriorite());
            pst.executeUpdate();
            System.out.println("Salle ajoutee!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec ajout salle: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // deleteEntity — DELETE using PreparedStatement
    // ══════════════════════════════════════════
    @Override
    public void deleteEntity(Salle salle) {
        String requete = "DELETE FROM salles WHERE id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, salle.getIdSalle());
            pst.executeUpdate();
            System.out.println("Salle supprimee!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec suppression salle: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // updateEntity — UPDATE using PreparedStatement
    // ══════════════════════════════════════════
    @Override
    public void updateEntity(int id, Salle salle) {
        String requete = "UPDATE salles SET nom_salle=?, type_salle=?, capacite_max=?, statut=?, niveau_gravite_cible=?, priorite=? WHERE id_salle=?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setString(1, salle.getNomSalle());
            pst.setString(2, salle.getTypeSalle());
            pst.setInt(3, salle.getCapaciteMax());
            pst.setString(4, salle.getStatut());
            pst.setInt(5, salle.getNiveauGraviteCible());
            pst.setInt(6, salle.getPriorite());
            pst.setInt(7, id);
            pst.executeUpdate();
            System.out.println("Salle mise a jour!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec mise a jour salle: " + e.getMessage(), e);
        }
    }

    // updateEntity2 — UPDATE occupancy delta using PreparedStatement
    public void updateEntity2(int idSalle, int delta) {
        String requete = "UPDATE salles SET nombre_actuel = nombre_actuel + ? WHERE id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, delta);
            pst.setInt(2, idSalle);
            pst.executeUpdate();
            updateStatut(idSalle);
            System.out.println("Capacite mise a jour!");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Echec mise a jour capacite: " + e.getMessage(), e);
        }
    }

    // ══════════════════════════════════════════
    // getData — SELECT all using Statement (required by teacher)
    // ══════════════════════════════════════════
    @Override
    public List<Salle> getData() {
        List<Salle> list = new ArrayList<>();
        String requete =
            "SELECT s.id_salle, s.nom_salle, s.type_salle, s.capacite_max, " +
            "s.statut, s.niveau_gravite_cible, s.priorite, s.patients_en_attente, " +
            "(SELECT COUNT(*) FROM triage t WHERE t.id_salle = s.id_salle " +
            " AND t.patient_state NOT IN ('Discharged','Cancelled','LeftWithoutSeen')) AS nombre_actuel " +
            "FROM salles s ORDER BY s.priorite ASC";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(requete)) {
            while (rs.next()) {
                list.add(mapSalle(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // getData2 — SELECT available salles by gravity using PreparedStatement
    public List<Salle> getData2(int gravityLevel) {
        List<Salle> list = new ArrayList<>();
        String requete = "SELECT * FROM salles WHERE niveau_gravite_cible=? AND statut='Disponible' AND nombre_actuel < capacite_max ORDER BY priorite ASC";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, gravityLevel);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSalle(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // getData3 — SELECT by id, nombre_actuel computed from real triage records
    public Salle getData3(int id) {
        String requete =
            "SELECT s.id_salle, s.nom_salle, s.type_salle, s.capacite_max, " +
            "s.statut, s.niveau_gravite_cible, s.priorite, s.patients_en_attente, " +
            "(SELECT COUNT(*) FROM triage t WHERE t.id_salle = s.id_salle " +
            " AND t.patient_state NOT IN ('Discharged','Cancelled','LeftWithoutSeen')) AS nombre_actuel " +
            "FROM salles s WHERE s.id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return mapSalle(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Resync the stored nombre_actuel counter from real triage data and fix statut */
    public void resyncNombreActuel(int idSalle) {
        String syncCount = "UPDATE salles SET nombre_actuel = (" +
            "SELECT COUNT(*) FROM triage WHERE id_salle = ? " +
            "AND patient_state NOT IN ('Discharged','Cancelled','LeftWithoutSeen')" +
            ") WHERE id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(syncCount)) {
            pst.setInt(1, idSalle);
            pst.setInt(2, idSalle);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateStatut(idSalle);
    }

    /** Alias for getData3 */
    public Salle getById(int id) {
        return getData3(id);
    }

    // getPatientsByRoom — SELECT triages in a specific room
    public List<pro.revive.entities.Triage> getPatientsByRoom(int idSalle) {
        List<pro.revive.entities.Triage> list = new ArrayList<>();
        String requete = "SELECT t.*, p.nom, p.prenom, s.nom_salle " +
                "FROM triage t " +
                "JOIN admissions a ON t.id_admission = a.id_admission " +
                "JOIN patients p ON a.id_patient = p.id_patient " +
                "LEFT JOIN salles s ON t.id_salle = s.id_salle " +
                "WHERE t.id_salle = ? AND t.patient_state NOT IN ('Discharged','Cancelled','LeftWithoutSeen')";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, idSalle);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    pro.revive.entities.Triage t = new pro.revive.entities.Triage();
                    t.setIdTriage(rs.getInt("id_triage"));
                    t.setIdAdmission(rs.getInt("id_admission"));
                    t.setIdSalle(rs.getInt("id_salle"));
                    t.setConstancesPouls(rs.getInt("constantes_pouls"));
                    t.setConstancesTemperature(rs.getFloat("constantes_temperature"));
                    t.setSpo2(rs.getInt("spo2"));
                    t.setNiveauFinal(rs.getInt("niveau_final"));
                    t.setPatientState(rs.getString("patient_state"));
                    t.setNomPatient(rs.getString("nom"));
                    t.setPrenomPatient(rs.getString("prenom"));
                    t.setNomSalle(rs.getString("nom_salle"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            System.out.println("getPatientsByRoom: " + e.getMessage());
        }
        return list;
    }

    public void decrementWaitlist(int idSalle) {
        String requete = "UPDATE salles SET patients_en_attente = GREATEST(patients_en_attente - 1, 0) WHERE id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, idSalle);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // BUG-10 fix: increment waitlist counter when patient enters waiting room
    public void incrementWaitlist(int idSalle) {
        String requete = "UPDATE salles SET patients_en_attente = patients_en_attente + 1 WHERE id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, idSalle);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateStatut(int idSalle) {
        String requete = "UPDATE salles SET statut = CASE WHEN nombre_actuel >= capacite_max THEN 'Pleine' ELSE 'Disponible' END WHERE id_salle = ?";
        try (Connection c = getCnx(); PreparedStatement pst = c.prepareStatement(requete)) {
            pst.setInt(1, idSalle);
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // BUG-7 fix: also escape backslashes
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "''");
    }

    private Salle mapSalle(ResultSet rs) throws SQLException {
        Salle s = new Salle();
        s.setIdSalle(rs.getInt("id_salle"));
        s.setNomSalle(rs.getString("nom_salle"));
        s.setTypeSalle(rs.getString("type_salle"));
        s.setCapaciteMax(rs.getInt("capacite_max"));
        s.setNombreActuel(rs.getInt("nombre_actuel"));
        s.setStatut(rs.getString("statut"));
        s.setNiveauGraviteCible(rs.getInt("niveau_gravite_cible"));
        s.setPriorite(rs.getInt("priorite"));
        s.setPatientsEnAttente(rs.getInt("patients_en_attente"));
        return s;
    }
}

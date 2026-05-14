package pro.revive.services.ServicesMateriel;

import pro.revive.entities.EntitiesMateriel.Ambulance;
import pro.revive.entities.EntitiesMateriel.Trajet;
import pro.revive.entities.EntitiesMateriel.AlerteMaintenance;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AmbulanceService {

    private final Connection conn = MyConnection.getInstance().getCnx();

    // ═══════════════════════════════════════════════════════════════
    // CRUD Ambulances
    // ═══════════════════════════════════════════════════════════════

    public void create(Ambulance ambulance) throws SQLException {
        String sql = "INSERT INTO ambulances (numero_serie, marque, modele, annee_fabrication, etat, km_total, " +
                     "date_derniere_vidange, km_derniere_vidange, date_derniers_pneus, km_derniers_pneus) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ambulance.getNumeroSerie());
            ps.setString(2, ambulance.getMarque());
            ps.setString(3, ambulance.getModele());
            ps.setObject(4, ambulance.getAnneeFabrication());
            ps.setString(5, ambulance.getEtat());
            ps.setDouble(6, ambulance.getKmTotal() != null ? ambulance.getKmTotal() : 0.0);
            ps.setObject(7, ambulance.getDateDerniereVidange());
            ps.setObject(8, ambulance.getKmDerniereVidange());
            ps.setObject(9, ambulance.getDateDerniersPneus());
            ps.setObject(10, ambulance.getKmDerniersPneus());
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) ambulance.setIdAmbulance(rs.getInt(1));
        }
    }

    public List<Ambulance> findAll() throws SQLException {
        List<Ambulance> list = new ArrayList<>();
        String sql = "SELECT * FROM ambulances ORDER BY numero_serie";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public Ambulance findById(int id) throws SQLException {
        String sql = "SELECT * FROM ambulances WHERE id_ambulance = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapResultSet(rs);
        }
        return null;
    }

    public void update(Ambulance ambulance) throws SQLException {
        String sql = "UPDATE ambulances SET numero_serie=?, marque=?, modele=?, annee_fabrication=?, etat=?, " +
                     "km_total=?, date_derniere_vidange=?, km_derniere_vidange=?, date_derniers_pneus=?, km_derniers_pneus=? " +
                     "WHERE id_ambulance=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ambulance.getNumeroSerie());
            ps.setString(2, ambulance.getMarque());
            ps.setString(3, ambulance.getModele());
            ps.setObject(4, ambulance.getAnneeFabrication());
            ps.setString(5, ambulance.getEtat());
            ps.setDouble(6, ambulance.getKmTotal());
            ps.setObject(7, ambulance.getDateDerniereVidange());
            ps.setObject(8, ambulance.getKmDerniereVidange());
            ps.setObject(9, ambulance.getDateDerniersPneus());
            ps.setObject(10, ambulance.getKmDerniersPneus());
            ps.setInt(11, ambulance.getIdAmbulance());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ambulances WHERE id_ambulance = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Gestion des trajets
    // ═══════════════════════════════════════════════════════════════

    public void enregistrerTrajet(Trajet trajet) throws SQLException {
        String sql = "INSERT INTO trajets (id_ambulance, localisation_depart, localisation_urgence, " +
                     "distance_km, duree_minutes, statut) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, trajet.getIdAmbulance());
            ps.setString(2, trajet.getLocalisationDepart());
            ps.setString(3, trajet.getLocalisationUrgence());
            ps.setDouble(4, trajet.getDistanceKm());
            ps.setInt(5, trajet.getDureeMinutes());
            ps.setString(6, trajet.getStatut());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) trajet.setIdTrajet(keys.getInt(1));
        }

        // Mettre à jour le kilométrage total seulement si la mission est terminée
        if ("Terminé".equals(trajet.getStatut())) {
            String updateKm = "UPDATE ambulances SET km_total = km_total + ? WHERE id_ambulance = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateKm)) {
                ps.setDouble(1, trajet.getDistanceKm());
                ps.setInt(2, trajet.getIdAmbulance());
                ps.executeUpdate();
            }
            analyserMaintenanceIA(trajet.getIdAmbulance());
        }
    }

    /**
     * Finalise un trajet "En cours" : le passe à "Terminé" et met à jour le kilométrage.
     */
    public void finaliserTrajet(int idTrajet, int idAmbulance, double distanceKm) throws SQLException {
        String sql = "UPDATE trajets SET statut = 'Terminé' WHERE id_trajet = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTrajet);
            ps.executeUpdate();
        }
        String updateKm = "UPDATE ambulances SET km_total = km_total + ? WHERE id_ambulance = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateKm)) {
            ps.setDouble(1, distanceKm);
            ps.setInt(2, idAmbulance);
            ps.executeUpdate();
        }
        analyserMaintenanceIA(idAmbulance);
    }

    public List<Trajet> getTrajetsAmbulance(int idAmbulance) throws SQLException {
        List<Trajet> list = new ArrayList<>();
        String sql = "SELECT * FROM trajets WHERE id_ambulance = ? ORDER BY date_trajet DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAmbulance);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Trajet t = new Trajet();
                t.setIdTrajet(rs.getInt("id_trajet"));
                t.setIdAmbulance(rs.getInt("id_ambulance"));
                t.setLocalisationDepart(rs.getString("localisation_depart"));
                t.setLocalisationUrgence(rs.getString("localisation_urgence"));
                t.setDistanceKm(rs.getDouble("distance_km"));
                t.setDureeMinutes(rs.getInt("duree_minutes"));
                java.sql.Timestamp tsTrajet = rs.getTimestamp("date_trajet");
                if (tsTrajet != null) t.setDateTrajet(tsTrajet.toLocalDateTime());
                t.setStatut(rs.getString("statut"));
                list.add(t);
            }
        }
        return list;
    }

    // ═══════════════════════════════════════════════════════════════
    // IA de Maintenance
    // ═══════════════════════════════════════════════════════════════

    /**
     * Analyse les besoins de maintenance d'une ambulance selon ses km parcourus.
     * Génère automatiquement des alertes si nécessaire.
     */
    public void analyserMaintenanceIA(int idAmbulance) throws SQLException {
        Ambulance amb = findById(idAmbulance);
        if (amb == null) return;

        double kmTotal = amb.getKmTotal();
        double kmDepuisVidange = kmTotal - (amb.getKmDerniereVidange() != null ? amb.getKmDerniereVidange() : 0);
        double kmDepuisPneus = kmTotal - (amb.getKmDerniersPneus() != null ? amb.getKmDerniersPneus() : 0);

        System.out.println("[IA Maintenance] Analyse ambulance " + amb.getNumeroSerie());
        System.out.println("  KM total : " + kmTotal);
        System.out.println("  KM depuis vidange : " + kmDepuisVidange);
        System.out.println("  KM depuis pneus : " + kmDepuisPneus);

        // ── Règle 1 : Vidange tous les 5000 km ──
        if (kmDepuisVidange >= 5000) {
            String priorite = kmDepuisVidange >= 6000 ? "Critique" : "Élevée";
            creerAlerte(idAmbulance, "Vidange", priorite,
                    "Vidange nécessaire (" + Math.round(kmDepuisVidange) + " km depuis dernière vidange)",
                    kmTotal, kmTotal + 100);
        } else if (kmDepuisVidange >= 4500) {
            creerAlerte(idAmbulance, "Vidange", "Moyenne",
                    "Vidange recommandée dans " + Math.round(5000 - kmDepuisVidange) + " km",
                    kmTotal, kmTotal + 500);
        }

        // ── Règle 2 : Pneus tous les 40000 km ──
        if (kmDepuisPneus >= 40000) {
            creerAlerte(idAmbulance, "Pneus", "Critique",
                    "Remplacement des pneus urgent (" + Math.round(kmDepuisPneus) + " km)",
                    kmTotal, kmTotal + 100);
        } else if (kmDepuisPneus >= 35000) {
            creerAlerte(idAmbulance, "Pneus", "Élevée",
                    "Remplacement des pneus recommandé dans " + Math.round(40000 - kmDepuisPneus) + " km",
                    kmTotal, kmTotal + 5000);
        }

        // ── Règle 3 : Révision complète tous les 30000 km ──
        if (kmTotal % 30000 < 1000 && kmTotal > 10000) {
            creerAlerte(idAmbulance, "Révision complète", "Moyenne",
                    "Révision complète recommandée (seuil : " + Math.round(kmTotal / 30000) * 30000 + " km)",
                    kmTotal, Math.round(kmTotal / 30000) * 30000);
        }
    }

    private void creerAlerte(int idAmbulance, String type, String priorite, String description,
                             double kmActuel, double kmRecommande) throws SQLException {
        // Vérifier si une alerte similaire existe déjà (éviter les doublons)
        String check = "SELECT COUNT(*) FROM alertes_maintenance WHERE id_ambulance=? AND type_maintenance=? AND statut='En attente'";
        try (PreparedStatement ps = conn.prepareStatement(check)) {
            ps.setInt(1, idAmbulance);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("[IA] Alerte " + type + " déjà existante, ignorée.");
                return;
            }
        }

        String sql = "INSERT INTO alertes_maintenance (id_ambulance, type_maintenance, priorite, description, " +
                     "km_actuel, km_recommande) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAmbulance);
            ps.setString(2, type);
            ps.setString(3, priorite);
            ps.setString(4, description);
            ps.setDouble(5, kmActuel);
            ps.setDouble(6, kmRecommande);
            ps.executeUpdate();
            System.out.println("[IA] ✅ Alerte créée : " + type + " (" + priorite + ")");
        }
    }

    public List<AlerteMaintenance> getAlertesAmbulance(int idAmbulance) throws SQLException {
        List<AlerteMaintenance> list = new ArrayList<>();
        String sql = "SELECT * FROM alertes_maintenance WHERE id_ambulance = ? ORDER BY priorite DESC, date_generation DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAmbulance);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AlerteMaintenance a = new AlerteMaintenance();
                a.setIdAlerte(rs.getInt("id_alerte"));
                a.setIdAmbulance(rs.getInt("id_ambulance"));
                a.setTypeMaintenance(rs.getString("type_maintenance"));
                a.setPriorite(rs.getString("priorite"));
                a.setDescription(rs.getString("description"));
                a.setKmActuel(rs.getDouble("km_actuel"));
                a.setKmRecommande(rs.getDouble("km_recommande"));
                java.sql.Timestamp tsGen = rs.getTimestamp("date_generation");
                if (tsGen != null) a.setDateGeneration(tsGen.toLocalDateTime());
                a.setStatut(rs.getString("statut"));
                Timestamp resol = rs.getTimestamp("date_resolution");
                if (resol != null) a.setDateResolution(resol.toLocalDateTime());
                list.add(a);
            }
        }
        return list;
    }

    public void marquerAlerteEffectuee(int idAlerte) throws SQLException {
        String sql = "UPDATE alertes_maintenance SET statut='Effectuée', date_resolution=NOW() WHERE id_alerte=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idAlerte);
            ps.executeUpdate();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private Ambulance mapResultSet(ResultSet rs) throws SQLException {
        Ambulance a = new Ambulance();
        a.setIdAmbulance(rs.getInt("id_ambulance"));
        a.setNumeroSerie(rs.getString("numero_serie"));
        a.setMarque(rs.getString("marque"));
        a.setModele(rs.getString("modele"));
        a.setAnneeFabrication(rs.getInt("annee_fabrication"));
        a.setEtat(rs.getString("etat"));
        a.setKmTotal(rs.getDouble("km_total"));
        Date vidange = rs.getDate("date_derniere_vidange");
        if (vidange != null) a.setDateDerniereVidange(vidange.toLocalDate());
        a.setKmDerniereVidange(rs.getDouble("km_derniere_vidange"));
        Date pneus = rs.getDate("date_derniers_pneus");
        if (pneus != null) a.setDateDerniersPneus(pneus.toLocalDate());
        a.setKmDerniersPneus(rs.getDouble("km_derniers_pneus"));
        return a;
    }

    public List<Ambulance> getAmbulancesDisponibles() throws SQLException {
        List<Ambulance> list = new ArrayList<>();
        String sql = "SELECT * FROM ambulances WHERE etat = 'Disponible' ORDER BY numero_serie";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public List<Ambulance> getAmbulancesEnRoute() throws SQLException {
        List<Ambulance> list = new ArrayList<>();
        String sql = "SELECT * FROM ambulances WHERE etat = 'En route' ORDER BY numero_serie";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSet(rs));
        }
        return list;
    }

    public void changerEtat(int idAmbulance, String nouvelEtat) throws SQLException {
        String sql = "UPDATE ambulances SET etat = ? WHERE id_ambulance = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelEtat);
            ps.setInt(2, idAmbulance);
            ps.executeUpdate();
        }
    }
    
    public void changerStatutTrajet(int idTrajet, String nouveauStatut) throws SQLException {
        String sql = "UPDATE trajets SET statut = ? WHERE id_trajet = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, idTrajet);
            ps.executeUpdate();
        }
    }
}

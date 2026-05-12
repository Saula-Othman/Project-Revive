package pro.revive.services.ServicesMed;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.*;

/**
 * Service auxiliaire pour le personnel médical.
 * Utilise la connexion singleton — PAS de try-with-resources sur getCnx()
 * pour ne pas fermer la connexion partagée.
 */
public class PersonnelService {

    /**
     * Retourne une map id_personnel -> "Prénom Nom" pour tous les médecins urgentistes.
     */
    public static Map<Integer, String> getMedecins() {
        Map<Integer, String> result = new LinkedHashMap<>();
        String sql = "SELECT id_personnel, prenom, nom "
                   + "FROM personnel "
                   + "WHERE role = 'Medecin Urgentiste' "
                   + "ORDER BY nom, prenom";
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id_personnel");
                String fullName = rs.getString("prenom") + " " + rs.getString("nom");
                result.put(id, fullName);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("[PersonnelService] getMedecins : " + e.getMessage());
        }
        return result;
    }

    /**
     * Retourne le nom complet d'un médecin à partir de son id.
     */
    public static String getNomMedecinById(int idPersonnel) {
        String sql = "SELECT prenom, nom FROM personnel WHERE id_personnel = ?";
        try {
            Connection cnx = MyConnection.getInstance().getCnx();
            PreparedStatement ps = cnx.prepareStatement(sql);
            ps.setInt(1, idPersonnel);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("prenom") + " " + rs.getString("nom");
                rs.close();
                ps.close();
                return nom;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("[PersonnelService] getNomMedecinById : " + e.getMessage());
        }
        
        return "Inconnu";
    }
}

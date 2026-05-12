package pro.revive.services.ServicesUser;

import pro.revive.entities.EntitiesUser.Personne;
import pro.revive.interfaces.IService;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PersonneService implements IService<Personne> {

    private Connection getConn() {
        return MyConnection.getInstance().getCnx();
    }

    // ══════════════════════════════════════════
    // addSignupRequest — INSERT with EN_ATTENTE status (no identifiant yet)
    // ══════════════════════════════════════════
    public void addSignupRequest(Personne personne) {
        String sql = "INSERT INTO personnel (nom, prenom, role, identifiant, mot_de_passe, " +
                     "date_naissance, telephone, email, statut, premier_connexion) " +
                     "VALUES (?, ?, ?, '', '', ?, ?, ?, 'EN_ATTENTE', FALSE)";
        try {
            Connection conn = getConn();
            if (conn == null) { System.err.println("addSignupRequest: pas de connexion DB"); return; }
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, personne.getNom());
            pst.setString(2, personne.getPrenom());
            pst.setString(3, personne.getRole());
            pst.setDate(4, personne.getDateNaissance() != null
                    ? Date.valueOf(personne.getDateNaissance()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            pst.setString(5, personne.getTelephone() != null ? personne.getTelephone() : "");
            pst.setString(6, personne.getEmail() != null ? personne.getEmail() : "");
            pst.executeUpdate();
            System.out.println("Demande d'inscription enregistree pour: " + personne.getNom());
        } catch (SQLException e) {
            System.err.println("addSignupRequest error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // getPendingRequests — SELECT all EN_ATTENTE
    // ══════════════════════════════════════════
    public List<Personne> getPendingRequests() {
        List<Personne> list = new ArrayList<>();
        String sql = "SELECT * FROM personnel WHERE statut = 'EN_ATTENTE' ORDER BY id_personnel DESC";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("getPendingRequests error: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════
    // approveRequest — generate identifiant + temp password, set ACTIF
    // ══════════════════════════════════════════
    public Personne approveRequest(int idPersonnel) {
        Personne p = getData5(idPersonnel);
        if (p == null) return null;

        // Generate identifiant
        String identifiant = generateIdentifiant(p.getNom(), p.getRole());
        // Generate random temp password
        String tempPassword = generateTempPassword();

        String sql = "UPDATE personnel SET identifiant=?, mot_de_passe=?, statut='ACTIF', premier_connexion=TRUE WHERE id_personnel=?";
        try {
            Connection conn = getConn();
            if (conn == null) return null;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, identifiant);
            pst.setString(2, tempPassword);
            pst.setInt(3, idPersonnel);
            pst.executeUpdate();
            p.setIdentifiant(identifiant);
            p.setMotDePasse(tempPassword);
            System.out.println("Demande approuvee: " + identifiant);
            return p;
        } catch (SQLException e) {
            System.err.println("approveRequest error: " + e.getMessage());
        }
        return null;
    }

    // ══════════════════════════════════════════
    // rejectRequest — DELETE the pending request
    // ══════════════════════════════════════════
    public void rejectRequest(int idPersonnel) {
        String sql = "DELETE FROM personnel WHERE id_personnel = ? AND statut = 'EN_ATTENTE'";
        try {
            Connection conn = getConn();
            if (conn == null) return;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idPersonnel);
            pst.executeUpdate();
            System.out.println("Demande refusee pour id=" + idPersonnel);
        } catch (SQLException e) {
            System.err.println("rejectRequest error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // setPremierConnexionFalse — after first login password change
    // ══════════════════════════════════════════
    public void setPremierConnexionFalse(int idPersonnel) {
        String sql = "UPDATE personnel SET premier_connexion=FALSE WHERE id_personnel=?";
        try {
            Connection conn = getConn();
            if (conn == null) return;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idPersonnel);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("setPremierConnexionFalse error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // getPendingCount — count EN_ATTENTE requests
    // ══════════════════════════════════════════
    public int getPendingCount() {
        String sql = "SELECT COUNT(*) FROM personnel WHERE statut = 'EN_ATTENTE'";
        try {
            Connection conn = getConn();
            if (conn == null) return 0;
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("getPendingCount error: " + e.getMessage());
        }
        return 0;
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$!";
        java.util.Random rnd = new java.util.Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    // ══════════════════════════════════════════
    // addEntity — INSERT (admin adds directly)
    // ══════════════════════════════════════════
    @Override
    public void addEntity(Personne personne) {
        String identifiant = generateIdentifiant(personne.getNom(), personne.getRole());
        personne.setIdentifiant(identifiant);
        System.out.println("Identifiant genere: " + identifiant);

        String sql = "INSERT INTO personnel (nom, prenom, role, identifiant, mot_de_passe, " +
                     "date_naissance, telephone, email, statut, premier_connexion) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIF', TRUE)";
        try {
            Connection conn = getConn();
            if (conn == null) { System.err.println("addEntity: pas de connexion DB"); return; }
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, personne.getNom());
            pst.setString(2, personne.getPrenom());
            pst.setString(3, personne.getRole());
            pst.setString(4, personne.getIdentifiant());
            pst.setString(5, personne.getMotDePasse());
            pst.setDate(6, personne.getDateNaissance() != null
                    ? Date.valueOf(personne.getDateNaissance()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            pst.setString(7, personne.getTelephone() != null ? personne.getTelephone() : "");
            pst.setString(8, personne.getEmail() != null ? personne.getEmail() : "");
            pst.executeUpdate();
            System.out.println("Personnel ajoute! Identifiant: " + identifiant);

            // Envoi email de confirmation
            EmailService.sendConfirmationEmail(personne);

        } catch (SQLException e) {
            System.err.println("addEntity error: " + e.getMessage());
        }
    }

    /**
     * Check if an agent with same nom+prenom already exists.
     * Returns list of similar agents found.
     */
    public List<Personne> checkDuplicate(String nom, String prenom) {
        List<Personne> list = new ArrayList<>();
        String sql = "SELECT * FROM personnel WHERE LOWER(nom)=LOWER(?) AND LOWER(prenom)=LOWER(?)";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, nom.trim());
            pst.setString(2, prenom.trim());
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("checkDuplicate error: " + e.getMessage());
        }
        return list;
    }

    // ══════════════════════════════════════════
    // deleteEntity — DELETE
    // ══════════════════════════════════════════
    @Override
    public void deleteEntity(Personne personne) {
        System.out.println("deleteEntity appele pour id=" + personne.getIdPersonnel());
        String sql = "DELETE FROM personnel WHERE id_personnel = ?";
        try {
            Connection conn = getConn();
            if (conn == null) { System.err.println("deleteEntity: pas de connexion DB"); return; }
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, personne.getIdPersonnel());
            int rows = pst.executeUpdate();
            System.out.println("deleteEntity: " + rows + " ligne(s) supprimee(s) pour id=" + personne.getIdPersonnel());
        } catch (SQLException e) {
            System.err.println("deleteEntity error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // updateEntity — UPDATE
    // ══════════════════════════════════════════
    @Override
    public void updateEntity(int id, Personne personne) {
        String sql = "UPDATE personnel SET nom=?, prenom=?, role=?, identifiant=?, date_naissance=?, telephone=?, email=? WHERE id_personnel=?";
        try {
            Connection conn = getConn();
            if (conn == null) { System.err.println("updateEntity: pas de connexion DB"); return; }
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, personne.getNom());
            pst.setString(2, personne.getPrenom());
            pst.setString(3, personne.getRole());
            pst.setString(4, personne.getIdentifiant());
            pst.setDate(5, personne.getDateNaissance() != null
                    ? Date.valueOf(personne.getDateNaissance()) : Date.valueOf(LocalDate.of(2000, 1, 1)));
            pst.setString(6, personne.getTelephone() != null ? personne.getTelephone() : "");
            pst.setString(7, personne.getEmail() != null ? personne.getEmail() : "");
            pst.setInt(8, id);
            pst.executeUpdate();
            System.out.println("Personnel mis a jour! id=" + id);
        } catch (SQLException e) {
            System.err.println("updateEntity error: " + e.getMessage());
        }
    }

    // updateEntity2 — UPDATE password
    public void updateEntity2(int id, String newPassword) {
        String sql = "UPDATE personnel SET mot_de_passe=? WHERE id_personnel=?";
        try {
            Connection conn = getConn();
            if (conn == null) { System.err.println("updateEntity2: pas de connexion DB"); return; }
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, newPassword);
            pst.setInt(2, id);
            pst.executeUpdate();
            System.out.println("Mot de passe mis a jour! id=" + id);
        } catch (SQLException e) {
            System.err.println("updateEntity2 error: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════
    // getData — SELECT all
    // ══════════════════════════════════════════
    @Override
    public List<Personne> getData() {
        List<Personne> list = new ArrayList<>();
        String sql = "SELECT * FROM personnel ORDER BY role ASC, nom ASC";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("getData error: " + e.getMessage());
        }
        return list;
    }

    // getData2 — search by nom or prenom
    public List<Personne> getData2(String keyword) {
        List<Personne> list = new ArrayList<>();
        String sql = "SELECT * FROM personnel WHERE nom LIKE ? OR prenom LIKE ?";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, "%" + keyword + "%");
            pst.setString(2, "%" + keyword + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("getData2 error: " + e.getMessage());
        }
        return list;
    }

    // getData3 — filter by role
    public List<Personne> getData3(String role) {
        List<Personne> list = new ArrayList<>();
        String sql = "SELECT * FROM personnel WHERE role = ? ORDER BY nom ASC";
        try {
            Connection conn = getConn();
            if (conn == null) return list;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, role);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("getData3 error: " + e.getMessage());
        }
        return list;
    }

    // getData4 — authentication (only ACTIF accounts)
    public Personne getData4(String identifiant, String motDePasse) {
        String sql = "SELECT * FROM personnel WHERE identifiant=? AND mot_de_passe=? AND (statut='ACTIF' OR statut IS NULL OR statut='')";
        try {
            Connection conn = getConn();
            if (conn == null) return null;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, identifiant);
            pst.setString(2, motDePasse);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Personne p = map(rs);
                System.out.println("Connexion reussie: " + p.getNom() + " (" + p.getRole() + ")");
                return p;
            } else {
                // Check if account exists but is EN_ATTENTE
                String checkSql = "SELECT statut FROM personnel WHERE identifiant=?";
                PreparedStatement checkPst = conn.prepareStatement(checkSql);
                checkPst.setString(1, identifiant);
                ResultSet checkRs = checkPst.executeQuery();
                if (checkRs.next() && "EN_ATTENTE".equals(checkRs.getString("statut"))) {
                    System.out.println("Compte en attente de validation: " + identifiant);
                    return null; // will be handled by LoginController
                }
                System.out.println("Identifiant ou mot de passe incorrect!");
            }
        } catch (SQLException e) {
            System.err.println("getData4 error: " + e.getMessage());
        }
        return null;
    }

    // getData4WithStatus — returns account even if EN_ATTENTE (for status check)
    public String getAccountStatus(String identifiant) {
        String sql = "SELECT statut FROM personnel WHERE identifiant=?";
        try {
            Connection conn = getConn();
            if (conn == null) return null;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, identifiant);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("statut");
        } catch (SQLException e) {
            System.err.println("getAccountStatus error: " + e.getMessage());
        }
        return null;
    }

    // getData5 — select by id
    public Personne getData5(int id) {
        String sql = "SELECT * FROM personnel WHERE id_personnel = ?";
        try {
            Connection conn = getConn();
            if (conn == null) return null;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("getData5 error: " + e.getMessage());
        }
        return null;
    }

    // getByEmail — find agent by email
    public Personne getByEmail(String email) {
        String sql = "SELECT * FROM personnel WHERE email = ?";
        try {
            Connection conn = getConn();
            if (conn == null) return null;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("getByEmail error: " + e.getMessage());
        }
        return null;
    }
    // ── helpers ────────────────────────────────────────────

    private Personne map(ResultSet rs) throws SQLException {
        Personne p = new Personne();
        p.setIdPersonnel(rs.getInt("id_personnel"));
        p.setNom(rs.getString("nom"));
        p.setPrenom(rs.getString("prenom"));
        p.setRole(rs.getString("role"));
        p.setIdentifiant(rs.getString("identifiant"));
        p.setMotDePasse(rs.getString("mot_de_passe"));
        // date_naissance
        Date dn = rs.getDate("date_naissance");
        if (dn != null) p.setDateNaissance(dn.toLocalDate());
        // telephone & email
        p.setTelephone(rs.getString("telephone"));
        p.setEmail(rs.getString("email"));
        // statut & premier_connexion
        try { p.setStatut(rs.getString("statut")); } catch (Exception ignored) {}
        try { p.setPremierConnexion(rs.getBoolean("premier_connexion")); } catch (Exception ignored) {}
        return p;
    }

    private String generateIdentifiant(String nom, String role) {
        String prefix;
        switch (role) {
            case "Medecin Urgentiste":     prefix = "DR"; break;
            case "Infirmier Triage":       prefix = "TR"; break;
            case "Agent Accueil":          prefix = "AA"; break;
            case "Biologiste Radiologue":  prefix = "BR"; break;
            case "Responsable Logistique": prefix = "RL"; break;
            case "Administrateur":         prefix = "AD"; break;
            default:                       prefix = "XX"; break;
        }
        // First 3 letters of nom
        String namePart = nom.substring(0, Math.min(3, nom.length())).toUpperCase();

        // Get the global max number for this ROLE prefix (not name-specific)
        int nextNumber = getNextNumberForRole(prefix);

        String identifiant = prefix + namePart + String.format("%03d", nextNumber);
        // Safety check — if somehow it exists, increment
        while (identifiantExists(identifiant)) {
            nextNumber++;
            identifiant = prefix + namePart + String.format("%03d", nextNumber);
        }
        System.out.println("generateIdentifiant: " + identifiant);
        return identifiant;
    }

    /**
     * Returns the next available number for a given role prefix.
     * Counts ALL agents with this role prefix regardless of name.
     */
    private int getNextNumberForRole(String rolePrefix) {
        // Get max number from all identifiants starting with this role prefix
        String sql = "SELECT MAX(CAST(SUBSTRING(identifiant, ?) AS UNSIGNED)) FROM personnel " +
                     "WHERE identifiant LIKE ? AND identifiant != '' AND LENGTH(identifiant) >= ?";
        try {
            Connection conn = getConn();
            if (conn == null) return 1;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, rolePrefix.length() + 4); // skip prefix (2) + name (3) = position of number
            pst.setString(2, rolePrefix + "%");
            pst.setInt(3, rolePrefix.length() + 3 + 1); // at least prefix+name+1digit
            ResultSet rs = pst.executeQuery();
            if (rs.next() && rs.getObject(1) != null) {
                return rs.getInt(1) + 1;
            }
        } catch (SQLException e) {
            System.err.println("getNextNumberForRole error: " + e.getMessage());
        }
        return 1;
    }

    private boolean identifiantExists(String identifiant) {
        // Don't check empty identifiants (EN_ATTENTE records)
        if (identifiant == null || identifiant.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM personnel WHERE identifiant = ? AND identifiant != ''";
        try {
            Connection conn = getConn();
            if (conn == null) return false;
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, identifiant);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("identifiantExists check: " + identifiant + " -> count=" + count);
                return count > 0;
            }
        } catch (SQLException e) {
            System.err.println("identifiantExists error: " + e.getMessage());
        }
        return false;
    }
}

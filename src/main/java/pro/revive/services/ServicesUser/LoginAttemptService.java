package pro.revive.services.ServicesUser;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class LoginAttemptService {

    private static final int MAX_ATTEMPTS   = 3;
    private static final int BLOCK_1_MINUTES = 1;
    private static final int BLOCK_2_MINUTES = 5;

    private Connection getConn() {
        return MyConnection.getInstance().getCnx();
    }

    /**
     * Returns null if login is allowed.
     * Returns a message if the account is blocked.
     */
    public String checkBlocked(String identifiant) {
        try {
            PreparedStatement pst = getConn().prepareStatement(
                "SELECT nb_tentatives, derniere_tentative, bloque, nb_blocages FROM login_attempts WHERE identifiant = ?");
            pst.setString(1, identifiant);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                boolean bloque = rs.getBoolean("bloque");
                if (bloque) {
                    LocalDateTime derniere = rs.getTimestamp("derniere_tentative").toLocalDateTime();
                    int nbBlocages = rs.getInt("nb_blocages");
                    int blockMinutes = nbBlocages <= 1 ? BLOCK_1_MINUTES : BLOCK_2_MINUTES;
                    LocalDateTime deblocage = derniere.plusMinutes(blockMinutes);
                    if (LocalDateTime.now().isBefore(deblocage)) {
                        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), deblocage).getSeconds();
                        return "Compte bloque. Reessayez dans " + secondsLeft + " secondes.";
                    } else {
                        // Auto-unblock
                        unblock(identifiant);
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("checkBlocked error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Call this on failed login attempt.
     * Returns a message if account just got blocked, null otherwise.
     */
    public String recordFailedAttempt(String identifiant) {
        try {
            // Check if record exists
            PreparedStatement check = getConn().prepareStatement(
                "SELECT nb_tentatives, nb_blocages FROM login_attempts WHERE identifiant = ?");
            check.setString(1, identifiant);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                int attempts = rs.getInt("nb_tentatives") + 1;
                int nbBlocages = rs.getInt("nb_blocages");

                if (attempts >= MAX_ATTEMPTS) {
                    // Block the account
                    int newBlocages = nbBlocages + 1;
                    int blockMinutes = newBlocages <= 1 ? BLOCK_1_MINUTES : BLOCK_2_MINUTES;
                    PreparedStatement block = getConn().prepareStatement(
                        "UPDATE login_attempts SET nb_tentatives=?, derniere_tentative=NOW(), bloque=TRUE, nb_blocages=? WHERE identifiant=?");
                    block.setInt(1, attempts);
                    block.setInt(2, newBlocages);
                    block.setString(3, identifiant);
                    block.executeUpdate();
                    return "Compte bloque pendant " + blockMinutes + " minute(s) apres " + MAX_ATTEMPTS + " tentatives echouees.";
                } else {
                    PreparedStatement update = getConn().prepareStatement(
                        "UPDATE login_attempts SET nb_tentatives=?, derniere_tentative=NOW() WHERE identifiant=?");
                    update.setInt(1, attempts);
                    update.setString(2, identifiant);
                    update.executeUpdate();
                    return "Identifiant ou mot de passe incorrect. Tentative " + attempts + "/" + MAX_ATTEMPTS + ".";
                }
            } else {
                // First failed attempt — insert record
                PreparedStatement insert = getConn().prepareStatement(
                    "INSERT INTO login_attempts (identifiant, nb_tentatives, derniere_tentative, bloque, nb_blocages) VALUES (?, 1, NOW(), FALSE, 0)");
                insert.setString(1, identifiant);
                insert.executeUpdate();
                return "Identifiant ou mot de passe incorrect. Tentative 1/" + MAX_ATTEMPTS + ".";
            }
        } catch (Exception e) {
            System.err.println("recordFailedAttempt error: " + e.getMessage());
        }
        return "Identifiant ou mot de passe incorrect.";
    }

    /**
     * Call this on successful login — reset attempts.
     */
    public void resetAttempts(String identifiant) {
        try {
            PreparedStatement pst = getConn().prepareStatement(
                "UPDATE login_attempts SET nb_tentatives=0, bloque=FALSE WHERE identifiant=?");
            pst.setString(1, identifiant);
            pst.executeUpdate();
        } catch (Exception e) {
            System.err.println("resetAttempts error: " + e.getMessage());
        }
    }

    private void unblock(String identifiant) {
        try {
            PreparedStatement pst = getConn().prepareStatement(
                "UPDATE login_attempts SET nb_tentatives=0, bloque=FALSE WHERE identifiant=?");
            pst.setString(1, identifiant);
            pst.executeUpdate();
        } catch (Exception e) {
            System.err.println("unblock error: " + e.getMessage());
        }
    }
}

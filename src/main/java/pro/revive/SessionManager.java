package pro.revive;

import pro.revive.entities.EntitiesUser.Personne;

/**
 * Global session holder — set once after successful login,
 * read by any module that needs the current user's name, role, or id.
 *
 * Usage:
 *   // After login:
 *   SessionManager.login(user);
 *
 *   // Anywhere in the app:
 *   Personne me = SessionManager.getUser();
 *   String role = SessionManager.getRole();
 *
 *   // On logout:
 *   SessionManager.logout();
 */
public class SessionManager {

    private static Personne currentUser;

    private SessionManager() {}

    /** Store the authenticated user. Call this right after a successful login. */
    public static void login(Personne user) {
        currentUser = user;
        // Also sync Navigator so Triage module can read the name
        if (user != null) {
            Navigator.currentUserName    = user.getPrenom() + " " + user.getNom();
            Navigator.currentPersonnelId = user.getIdPersonnel();
        }
    }

    /** Clear the session. Call this on logout. */
    public static void logout() {
        currentUser = null;
        Navigator.currentUserName    = "";
        Navigator.currentPersonnelId = -1;
    }

    /** Returns the logged-in user, or null if not authenticated. */
    public static Personne getUser() {
        return currentUser;
    }

    /** Convenience — returns the role string, or empty string if not logged in. */
    public static String getRole() {
        return currentUser != null && currentUser.getRole() != null
                ? currentUser.getRole() : "";
    }

    /** Convenience — returns "Prénom Nom". */
    public static String getFullName() {
        if (currentUser == null) return "";
        return (currentUser.getPrenom() != null ? currentUser.getPrenom() : "")
             + " "
             + (currentUser.getNom() != null ? currentUser.getNom() : "");
    }

    /** Returns true if a user is currently logged in. */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}

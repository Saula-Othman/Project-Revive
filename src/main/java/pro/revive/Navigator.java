package pro.revive;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;

import java.io.IOException;

/**
 * Central navigation utility for Module 2.
 * Screens: Dashboard, Triage_List, Triage_Add, Triage_Edit,
 *          Triage_Delete, Triage_View, Salle_List, Salle_View, Surveillance, VisualAssistance
 *
 * Prefer the typed helpers (goToTriage, goToSalle) over the two-step pattern
 * of setting currentTriageId / currentSalleId and then calling goTo() — the
 * helpers are atomic and impossible to forget.
 */
public class Navigator {

    // Shared state — set before navigating
    public static int currentTriageId = -1;
    public static int currentSalleId  = -1;

    // ── Logged-in user ───────────────────────────────────────────
    // Defaults are used only while the login module is not yet integrated.
    // Once the login screen is ready, call Navigator.login(name, id) after
    // a successful authentication and the name will appear everywhere in the UI.
    public static String currentUserName    = "Infirmier Triage";
    public static int    currentPersonnelId = 1;

    /**
     * Call this from the login controller after a successful authentication.
     * Sets the logged-in user and navigates to the Dashboard.
     *
     * Example (from your login module):
     *   Navigator.login("Dr. Ben Ali", 42);
     *
     * @param fullName      Full name of the logged-in user (shown in the top-right of every screen)
     * @param personnelId   id_personnel from the DB (used for override tracking, audit trail, etc.)
     */
    public static void login(String fullName, int personnelId) {
        currentUserName    = fullName;
        currentPersonnelId = personnelId;
        goTo("DashboardTriage");
    }

    /**
     * Call this when the user logs out.
     * Clears the session and navigates back to the Login screen.
     */
    public static void logout() {
        currentUserName    = "";
        currentPersonnelId = -1;
        currentTriageId    = -1;
        currentSalleId     = -1;
        SessionManager.logout();
        // Navigate back to Login
        try {
            java.net.URL fxmlUrl = Navigator.class.getResource("/ResourcesUser/images/fxml/Login.fxml");
            if (fxmlUrl == null) return;
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();
            java.net.URL cssUrl = Navigator.class.getResource("/ResourcesUser/images/css/user.css");
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
            App.primaryStage.setScene(scene);
            App.primaryStage.setTitle("REVIVE — Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Typed navigation helpers ─────────────────────────────────

    /** Navigate to the Triage detail view for the given id. */
    public static void goToTriage(int id) {
        currentTriageId = id;
        goTo("Triage_View");
    }

    /** Navigate to the Triage edit screen for the given id. */
    public static void goToTriageEdit(int id) {
        currentTriageId = id;
        goTo("Triage_Edit");
    }

    /** Navigate to the Triage delete confirmation for the given id. */
    public static void goToTriageDelete(int id) {
        currentTriageId = id;
        goTo("Triage_Delete");
    }

    /** Navigate to the Visual Assistance page. */
    public static void goToVisualAssistance() {
        goTo("VisualAssistance");
    }

    /** Navigate to the Salle detail view for the given id. */
    public static void goToSalle(int id) {
        currentSalleId = id;
        goTo("Salle_View");
    }

    // ── Generic navigation ───────────────────────────────────────

    public static void goTo(String fxmlName) {
        goTo(fxmlName, null);
    }

    public static void goTo(String fxmlName, Object controller) {
        try {
            String path = "/TriageResources/fxml/" + fxmlName + ".fxml";
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(path));
            if (controller != null) {
                loader.setController(controller);
            }
            Parent root = loader.load();
            // Fix: reuse the existing Scene instead of creating a new one every navigation
            if (App.primaryStage.getScene() == null) {
                App.primaryStage.setScene(new Scene(root, 1200, 800));
            } else {
                App.primaryStage.getScene().setRoot(root);
            }
        } catch (IOException | RuntimeException e) {
            // BUG-13 fix: javafx.fxml.LoadException extends RuntimeException (NOT IOException),
            // so the old catch(IOException) silently dropped any controller init failure,
            // leaving the scene unchanged — buttons appeared to "do nothing".
            e.printStackTrace();
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Impossible de charger l'ecran '" + fxmlName + "'.\n" + cause.getMessage());
            alert.setTitle("Erreur de navigation");
            alert.showAndWait();
        }
    }
}

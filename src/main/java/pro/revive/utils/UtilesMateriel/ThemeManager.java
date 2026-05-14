package pro.revive.utils.UtilesMateriel;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Gestionnaire de thème pour le module Matériel.
 *
 * Stratégie :
 *  - Le CSS de base (styleMateriel.css) est TOUJOURS présent dans la scène.
 *  - En mode sombre, on AJOUTE revive-dark-content.css par-dessus (override).
 *  - En mode clair,  on RETIRE revive-dark-content.css.
 *
 * Ainsi on ne touche jamais au CSS de base chargé par le FXML.
 */
public class ThemeManager {

    private static final String PREF_KEY = "revive.theme.dark";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    private static boolean darkMode = prefs.getBoolean(PREF_KEY, false);

    // Chemin correct dans les ressources du projet
    private static final String CSS_DARK_OVERLAY =
        "/ResourcesMateriel/module5/css/revive-dark-content.css";

    private static final List<Scene> registeredScenes = new ArrayList<>();

    /** Enregistre une scène et applique immédiatement le thème courant. */
    public static void register(Scene scene) {
        if (scene == null) return;
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        applyTheme(scene);
    }

    /** Bascule le thème et l'applique à toutes les scènes enregistrées. */
    public static void toggle() {
        darkMode = !darkMode;
        prefs.putBoolean(PREF_KEY, darkMode);
        // Nettoyer les scènes dont le root a été remplacé (navigation)
        registeredScenes.removeIf(s -> s == null || s.getRoot() == null);
        registeredScenes.forEach(ThemeManager::applyTheme);
    }

    /**
     * Applique le thème à une scène sans toucher aux CSS déjà présents.
     * On ajoute ou retire uniquement le CSS sombre.
     */
    public static void applyTheme(Scene scene) {
        if (scene == null) return;

        String darkUrl = null;
        try {
            var res = ThemeManager.class.getResource(CSS_DARK_OVERLAY);
            if (res != null) darkUrl = res.toExternalForm();
        } catch (Exception ignored) {}

        if (darkUrl == null) {
            System.err.println("[ThemeManager] CSS sombre introuvable : " + CSS_DARK_OVERLAY);
            return;
        }

        if (darkMode) {
            // Ajouter le CSS sombre s'il n'est pas déjà là
            if (!scene.getStylesheets().contains(darkUrl)) {
                scene.getStylesheets().add(darkUrl);
            }
        } else {
            // Retirer le CSS sombre
            scene.getStylesheets().remove(darkUrl);
        }
    }

    public static boolean isDarkMode() { return darkMode; }

    public static String getToggleLabel() {
        return darkMode ? "☀️  Mode Clair" : "🌙  Mode Sombre";
    }
}

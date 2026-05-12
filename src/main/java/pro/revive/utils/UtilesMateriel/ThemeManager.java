package pro.revive.utils.UtilesMateriel;

import javafx.scene.Scene;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Gestionnaire de thème pour REVIVE.
 * - Mode Clair  : fond blanc/gris, sidebar inchangée
 * - Mode Sombre : fond #0f172a, cartes #1e293b, sidebar inchangée
 */
public class ThemeManager {

    private static final String PREF_KEY = "revive.theme.dark";
    private static final Preferences prefs = Preferences.userNodeForPackage(ThemeManager.class);

    // true = sombre (défaut), false = clair
    private static boolean darkMode = prefs.getBoolean(PREF_KEY, false);

    private static final List<Scene> registeredScenes = new ArrayList<>();

    // CSS clair  = revive-dark.css  (fond #f0f4f8, sidebar bleue inchangée)
    // CSS sombre = revive-dark-content.css (fond #0f172a, sidebar bleue inchangée)
    private static final String CSS_LIGHT = "/com/revive/module5/css/revive-dark.css";
    private static final String CSS_DARK  = "/com/revive/module5/css/revive-dark-content.css";

    public static void register(Scene scene) {
        if (!registeredScenes.contains(scene)) {
            registeredScenes.add(scene);
        }
        applyTheme(scene);
    }

    public static void toggle() {
        darkMode = !darkMode;
        prefs.putBoolean(PREF_KEY, darkMode);
        registeredScenes.removeIf(s -> s.getRoot() == null);
        registeredScenes.forEach(ThemeManager::applyTheme);
    }

    public static void applyTheme(Scene scene) {
        scene.getStylesheets().clear();
        String css = darkMode ? CSS_DARK : CSS_LIGHT;
        try {
            String url = ThemeManager.class.getResource(css).toExternalForm();
            scene.getStylesheets().add(url);
        } catch (Exception e) {
            // Fallback
            try {
                scene.getStylesheets().add(
                    ThemeManager.class.getResource(CSS_LIGHT).toExternalForm()
                );
            } catch (Exception ignored) {}
        }
    }

    public static boolean isDarkMode() { return darkMode; }

    public static String getToggleLabel() {
        return darkMode ? "☀️  Mode Clair" : "🌙  Mode Sombre";
    }
}

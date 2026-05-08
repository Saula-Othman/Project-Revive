package pro.revive.utils;

/**
 * Shared UI utility methods used across multiple controllers.
 */
public class UIUtils {

    /**
     * Returns the level color hex for a given severity level (1-5).
     */
    public static String niveauColor(int niveau) {
        switch (niveau) {
            case 1: return "#EF4444";
            case 2: return "#F97316";
            case 3: return "#F59E0B";
            case 4: return "#3B82F6";
            case 5: return "#22C55E";
            default: return "#64748B";
        }
    }

    private UIUtils() {}
}

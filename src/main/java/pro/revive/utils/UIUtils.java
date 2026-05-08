package pro.revive.utils;

/**
 * Shared UI utility methods used across multiple controllers.
 */
public class UIUtils {

    /**
     * Converts a DB patient state string to French display text.
     */
    public static String etatFrancais(String state) {
        if (state == null) return "Inconnu";
        switch (state) {
            case "Triaged":           return "Triage";
            case "WaitingRoom":       return "Salle d'attente";
            case "InRoom":            return "En salle";
            case "InConsultation":    return "En consultation";
            case "Discharged":        return "Sorti";
            case "Cancelled":         return "Annule";
            case "LeftWithoutSeen":   return "Parti sans consultation";
            case "Quarantine":        return "Quarantaine";
            default:                  return state;
        }
    }

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

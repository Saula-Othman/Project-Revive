package pro.revive.services.TriageServices;

import pro.revive.entities.TriageEntities.Triage;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.List;

/**
 * IA d'analyse de contagion et de prévision saisonnière.
 * La détection de cas similaires est automatique — elle interroge la base de données
 * au lieu de demander à l'infirmier.
 */
public class AIAnalysisService {

    // ── Inner classes ────────────────────────────────────────────

    public static class ContagionResult {
        public String flag;             // "aucun" | "possible" | "confirme"
        public String maladieSupspecte; // nullable

        public ContagionResult(String flag, String maladieSupspecte) {
            this.flag             = flag;
            this.maladieSupspecte = maladieSupspecte;
        }
    }

    public static class SeasonalForecast {
        public String       analyse;
        public List<String> maladiesPrevues;
        public String       recommandations;

        public SeasonalForecast(String analyse, List<String> maladiesPrevues, String recommandations) {
            this.analyse         = analyse;
            this.maladiesPrevues = maladiesPrevues;
            this.recommandations = recommandations;
        }
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Évalue le risque de contagion d'un triage.
     *
     * Signaux analysés automatiquement :
     *   +2  Fièvre ≥ 38.5°C
     *   +2  Voyage récent déclaré
     *   +1  Syndrome respiratoire
     *   +1  Symptômes suspects (toux, fièvre, éruption, diarrhée)
     *   +3  Cas similaires détectés en DB dans les 48h (même syndrome, ≥ 2 autres patients)
     *
     * Résultat :
     *   score ≥ 6  → "confirme"
     *   score ≥ 3  → "possible"
     *   sinon      → "aucun"
     */
    public ContagionResult evaluerContagion(Triage t) {
        if (t == null) return new ContagionResult("aucun", null);

        String symptomes = t.getSymptomes() != null ? t.getSymptomes().toLowerCase() : "";
        String syndrome  = t.getSyndromeCategory();
        boolean voyage   = t.isVoyageRecent();

        boolean fievre      = t.getConstancesTemperature() >= 38.5f;
        boolean respSynd    = "Respiratoire".equalsIgnoreCase(syndrome);
        boolean suspSymptom = symptomes.contains("toux")     || symptomes.contains("fievre")
                           || symptomes.contains("eruption") || symptomes.contains("diarrhee");

        // ── Détection automatique de cas similaires en DB ────────
        boolean casSimilairesDetectes = detecterCasSimilaires(syndrome);

        int score = 0;
        if (fievre)                 score += 2;
        if (voyage)                 score += 2;
        if (respSynd)               score += 1;
        if (suspSymptom)            score += 1;
        if (casSimilairesDetectes)  score += 3;  // signal fort — remplace la question manuelle

        if (score >= 6) {
            String maladie = inferMaladie(syndrome, symptomes, voyage, casSimilairesDetectes);
            return new ContagionResult("confirme", maladie);
        }
        if (score >= 3) {
            return new ContagionResult("possible", null);
        }
        return new ContagionResult("aucun", null);
    }

    /**
     * Génère une prévision saisonnière basée sur la saison actuelle et les alertes OMS.
     */
    public SeasonalForecast genererPrevisionSaisonniere(String saisonNom, int mois,
                                                         List<WHOFeedService.WHOAlert> whoAlerts) {
        int nbAlertes = whoAlerts != null ? whoAlerts.size() : 0;

        String analyse = "Analyse saisonniere — " + saisonNom + " (mois " + mois + ").\n" +
                "Alertes OMS actives : " + nbAlertes + ".\n" +
                "Aucune anomalie epidemiologique majeure detectee localement.";

        List<String> maladies = new EpidemiologicalDetector().getSaisonActuelle().maladiesAttendues;

        String recommandations =
                "• Renforcer les precautions standard en salle d'attente.\n" +
                "• Isoler tout patient presentant fievre + symptomes respiratoires.\n" +
                "• Verifier la vaccination du personnel soignant.\n" +
                "• Consulter les alertes OMS regulierement.";

        return new SeasonalForecast(analyse, maladies, recommandations);
    }

    // ── Détection automatique de cas similaires ──────────────────

    /**
     * Interroge la DB : y a-t-il ≥ 2 autres patients actifs avec le même syndrome
     * arrivés dans les 48 dernières heures ?
     * Si oui, c'est un signal de cluster — pas besoin de demander à l'infirmier.
     */
    private boolean detecterCasSimilaires(String syndrome) {
        if (syndrome == null || syndrome.isEmpty()) return false;
        String sql = "SELECT COUNT(*) FROM triage " +
                     "WHERE syndrome_category = ? " +
                     "AND date_heure_triage >= NOW() - INTERVAL 48 HOUR " +
                     "AND patient_state NOT IN ('Discharged','Cancelled','LeftWithoutSeen')";
        try (Connection c = MyConnection.getInstance().getCnx();
             PreparedStatement pst = c.prepareStatement(sql)) {
            pst.setString(1, syndrome);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) >= 2; // au moins 2 autres cas du même syndrome
                }
            }
        } catch (SQLException e) {
            System.out.println("AIAnalysisService.detecterCasSimilaires: " + e.getMessage());
        }
        return false;
    }

    // ── Inférence de la maladie suspectée ────────────────────────

    private String inferMaladie(String syndrome, String symptomes,
                                 boolean voyage, boolean cluster) {
        if ("Respiratoire".equalsIgnoreCase(syndrome)) {
            if (symptomes.contains("covid") || symptomes.contains("perte gout"))
                return "COVID-19 suspecte";
            if (cluster) return "Infection respiratoire — cluster detecte";
            return "Infection respiratoire aigue";
        }
        if ("Digestif".equalsIgnoreCase(syndrome)) {
            if (cluster) return "Gastro-enterite — cluster detecte";
            return "Gastro-enterite infectieuse";
        }
        if ("Cutane".equalsIgnoreCase(syndrome) && voyage)
            return "Maladie tropicale cutanee";
        if ("Neurologique".equalsIgnoreCase(syndrome))
            return "Infection neurologique";
        return "Maladie infectieuse non determinee";
    }
}

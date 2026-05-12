package pro.revive.services.ServicesMed;

import pro.revive.utils.MyConnection;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Feature 5 — Calcul automatique du score de gravite
 * base sur les constantes vitales du triage (table triage).
 *
 * Score de gravite (1 = critique, 5 = non urgent) :
 * - SpO2 < 90%          → +3 points critique
 * - Pouls > 120 ou < 50 → +2 points urgent
 * - Temperature > 39.5  → +1 point
 * - Score douleur > 7   → +1 point
 * - GCS < 9             → +3 points critique
 */
public class GraviteService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    public record ConstantesVitales(
        Integer pouls,
        Double  temperature,
        Integer spo2,
        Double  glycemie,
        Integer scoreDouleur,
        Integer gcsScore,
        Integer frequenceRespiratoire,
        String  tensionArterielle
    ) {}

    public record ScoreGravite(
        int     niveau,       // 1 (critique) a 5 (non urgent)
        String  label,        // "Critique", "Urgent", etc.
        String  couleur,      // couleur hex pour l'UI
        String  explication,  // texte explicatif
        boolean alerte        // true si niveau <= 2
    ) {}

    /**
     * Recupere les constantes vitales du triage pour une admission.
     */
    public ConstantesVitales getConstantesVitales(int idAdmission) {
        String sql = "SELECT constantes_pouls, constantes_temperature, spo2, "
                   + "glycemie, score_douleur, gcs_score, frequence_respiratoire, constantes_ta "
                   + "FROM triage WHERE id_admission = ? "
                   + "ORDER BY date_heure_triage DESC LIMIT 1";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAdmission);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new ConstantesVitales(
                    getInt(rs, "constantes_pouls"),
                    getDouble(rs, "constantes_temperature"),
                    getInt(rs, "spo2"),
                    getDouble(rs, "glycemie"),
                    getInt(rs, "score_douleur"),
                    getInt(rs, "gcs_score"),
                    getInt(rs, "frequence_respiratoire"),
                    rs.getString("constantes_ta")
                );
            }
        } catch (SQLException e) {
            System.err.println("[GraviteService] " + e.getMessage());
        }
        return null;
    }

    /**
     * Calcule le score de gravite a partir des constantes vitales.
     */
    public ScoreGravite calculerScore(ConstantesVitales cv) {
        if (cv == null) return new ScoreGravite(5, "Non evalue", "#6b7280", "Aucune donnee de triage.", false);

        int score = 0;
        StringBuilder expl = new StringBuilder();

        // SpO2
        if (cv.spo2() != null) {
            if (cv.spo2() < 90) {
                score += 3;
                expl.append("SpO2 critique (").append(cv.spo2()).append("%). ");
            } else if (cv.spo2() < 94) {
                score += 1;
                expl.append("SpO2 basse (").append(cv.spo2()).append("%). ");
            }
        }

        // GCS
        if (cv.gcsScore() != null) {
            if (cv.gcsScore() < 9) {
                score += 3;
                expl.append("GCS critique (").append(cv.gcsScore()).append("/15). ");
            } else if (cv.gcsScore() < 13) {
                score += 1;
                expl.append("GCS altere (").append(cv.gcsScore()).append("/15). ");
            }
        }

        // Pouls
        if (cv.pouls() != null) {
            if (cv.pouls() > 130 || cv.pouls() < 40) {
                score += 2;
                expl.append("Pouls anormal (").append(cv.pouls()).append(" bpm). ");
            } else if (cv.pouls() > 110 || cv.pouls() < 50) {
                score += 1;
                expl.append("Pouls limite (").append(cv.pouls()).append(" bpm). ");
            }
        }

        // Temperature
        if (cv.temperature() != null) {
            if (cv.temperature() > 40.0 || cv.temperature() < 35.0) {
                score += 2;
                expl.append("Temperature critique (").append(cv.temperature()).append("C). ");
            } else if (cv.temperature() > 38.5) {
                score += 1;
                expl.append("Fievre (").append(cv.temperature()).append("C). ");
            }
        }

        // Score douleur
        if (cv.scoreDouleur() != null && cv.scoreDouleur() >= 8) {
            score += 1;
            expl.append("Douleur severe (").append(cv.scoreDouleur()).append("/10). ");
        }

        // Frequence respiratoire
        if (cv.frequenceRespiratoire() != null) {
            if (cv.frequenceRespiratoire() > 30 || cv.frequenceRespiratoire() < 8) {
                score += 2;
                expl.append("FR anormale (").append(cv.frequenceRespiratoire()).append("/min). ");
            }
        }

        // Convertir score en niveau 1-5
        int niveau;
        String label, couleur;
        if (score >= 6) {
            niveau = 1; label = "CRITIQUE"; couleur = "#dc2626";
        } else if (score >= 4) {
            niveau = 2; label = "URGENT"; couleur = "#ea580c";
        } else if (score >= 2) {
            niveau = 3; label = "Semi-urgent"; couleur = "#d97706";
        } else if (score >= 1) {
            niveau = 4; label = "Peu urgent"; couleur = "#16a34a";
        } else {
            niveau = 5; label = "Non urgent"; couleur = "#6b7280";
        }

        String explStr = expl.length() > 0 ? expl.toString().trim() : "Constantes dans les normes.";
        return new ScoreGravite(niveau, label, couleur, explStr, niveau <= 2);
    }

    /**
     * Retourne un resume des constantes vitales pour affichage.
     */
    public Map<String, String> getResume(ConstantesVitales cv) {
        Map<String, String> resume = new LinkedHashMap<>();
        if (cv == null) return resume;
        if (cv.pouls()                != null) resume.put("Pouls",       cv.pouls() + " bpm");
        if (cv.temperature()          != null) resume.put("Temperature", cv.temperature() + " C");
        if (cv.spo2()                 != null) resume.put("SpO2",        cv.spo2() + " %");
        if (cv.glycemie()             != null) resume.put("Glycemie",    cv.glycemie() + " g/L");
        if (cv.scoreDouleur()         != null) resume.put("Douleur",     cv.scoreDouleur() + "/10");
        if (cv.gcsScore()             != null) resume.put("GCS",         cv.gcsScore() + "/15");
        if (cv.frequenceRespiratoire()!= null) resume.put("FR",          cv.frequenceRespiratoire() + "/min");
        if (cv.tensionArterielle()    != null) resume.put("TA",          cv.tensionArterielle());
        return resume;
    }

    private Integer getInt(ResultSet rs, String col) {
        try { int v = rs.getInt(col); return rs.wasNull() ? null : v; }
        catch (SQLException e) { return null; }
    }

    private Double getDouble(ResultSet rs, String col) {
        try { double v = rs.getDouble(col); return rs.wasNull() ? null : v; }
        catch (SQLException e) { return null; }
    }
}

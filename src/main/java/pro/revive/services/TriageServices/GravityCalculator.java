package pro.revive.services;

import pro.revive.entities.Triage;
import java.util.ArrayList;
import java.util.List;

public class GravityCalculator {

    public static int calculateScore(Triage t) {
        int score = 0;
        List<String> reasons = null; // lazy — only allocated when a reason is added

        // Pulse
        int pouls = t.getConstancesPouls();
        if (pouls < 40 || pouls > 150) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Pouls critique (" + pouls + " bpm)"); }
        else if (pouls < 60 || pouls > 130) { score += 3; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Pouls anormal (" + pouls + " bpm)"); }

        // Blood Pressure
        float taSys = t.getConstancesTaSys();
        if (taSys < 80 || taSys > 180) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Tension critique (" + taSys + " mmHg)"); }
        else if (taSys < 90 || taSys > 160) { score += 3; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Tension anormale (" + taSys + " mmHg)"); }

        // Temperature
        float temp = t.getConstancesTemperature();
        if (temp < 35 || temp > 40) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Temperature critique (" + temp + "C)"); }
        else if (temp < 36 || temp > 38) { score += 2; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Temperature anormale (" + temp + "C)"); }

        // SpO2
        int spo2 = t.getSpo2();
        if (spo2 < 85) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("SpO2 critique (" + spo2 + "%)"); }
        else if (spo2 < 90) { score += 3; if (reasons == null) reasons = new ArrayList<>(); reasons.add("SpO2 bas (" + spo2 + "%)"); }
        else if (spo2 < 95) { score += 1; }

        // Glycemia
        float glycemie = t.getGlycemie();
        if (glycemie < 0.5f || glycemie > 3.0f) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Glycemie critique (" + glycemie + " g/L)"); }
        else if (glycemie < 0.7f || glycemie > 2.0f) { score += 3; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Glycemie anormale (" + glycemie + " g/L)"); }

        // Pain Score
        int douleur = t.getScoreDouleur();
        if (douleur >= 9) { score += 4; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Douleur severe (" + douleur + "/10)"); }
        else if (douleur >= 7) { score += 3; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Douleur importante (" + douleur + "/10)"); }
        else if (douleur >= 5) { score += 2; }

        // GCS
        int gcs = t.getGcsScore();
        if (gcs < 9) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Conscience alteree GCS (" + gcs + "/15)"); }
        else if (gcs < 13) { score += 3; if (reasons == null) reasons = new ArrayList<>(); reasons.add("GCS reduit (" + gcs + "/15)"); }
        else if (gcs < 15) { score += 1; }

        // Respiratory Rate
        int freqResp = t.getFrequenceRespiratoire();
        if (freqResp < 10 || freqResp > 30) { score += 5; if (reasons == null) reasons = new ArrayList<>(); reasons.add("Frequence resp. critique (" + freqResp + "/min)"); }
        else if (freqResp < 12 || freqResp > 25) { score += 2; }

        t.setScoreCalcule(score);
        t.setNiveauAuto(scoreToLevel(score));
        t.setNiveauFinal(scoreToLevel(score));
        t.setAnalyseAuto(reasons == null
                ? "Toutes les constantes normales. Niveau 5 - Cas mineur."
                : levelLabel(scoreToLevel(score)) + ": " + String.join(", ", reasons) + ".");

        return score;
    }

    public static int scoreToLevel(int score) {
        if (score >= 20) return 1;
        if (score >= 14) return 2;
        if (score >= 8)  return 3;
        if (score >= 3)  return 4;
        return 5;
    }

    public static String levelLabel(int level) {
        switch (level) {
            case 1: return "CRITIQUE";
            case 2: return "TRES URGENT";
            case 3: return "URGENT";
            case 4: return "STANDARD";
            default: return "MINEUR";
        }
    }

    // ══════════════════════════════════════════
    // Vital status helpers — single source of truth for thresholds.
    // Used by TriageViewController to color vital cards.
    // Returns "crit", "warn", or "" (normal).
    // ══════════════════════════════════════════

    public static String getPulseStatus(int pouls) {
        if (pouls < 40 || pouls > 150) return "crit";
        if (pouls < 60 || pouls > 130) return "warn";
        return "";
    }

    public static String getTensionStatus(float taSys) {
        if (taSys < 80 || taSys > 180) return "crit";
        if (taSys < 90 || taSys > 160) return "warn";
        return "";
    }

    public static String getTemperatureStatus(float temp) {
        if (temp < 35 || temp > 40) return "crit";
        if (temp < 36 || temp > 38) return "warn";
        return "";
    }

    public static String getSpo2Status(int spo2) {
        if (spo2 < 85) return "crit";
        if (spo2 < 90) return "warn";
        return "";
    }

    public static String getGlycemieStatus(float glycemie) {
        if (glycemie < 0.5f || glycemie > 3.0f) return "crit";
        if (glycemie < 0.7f || glycemie > 2.0f) return "warn";
        return "";
    }

    public static String getGcsStatus(int gcs) {
        if (gcs < 9)  return "crit";
        if (gcs < 13) return "warn";
        return "";
    }

    public static String getFreqRespStatus(int freqResp) {
        if (freqResp < 10 || freqResp > 30) return "crit";
        if (freqResp < 12 || freqResp > 25) return "warn";
        return "";
    }

    public static String getDouleurStatus(int douleur) {
        if (douleur >= 9) return "crit";
        if (douleur >= 7) return "warn";
        return "";
    }
}

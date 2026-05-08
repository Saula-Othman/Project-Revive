package pro.revive.services;

import pro.revive.entities.Triage;
import pro.revive.utils.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Epidemiological detection service.
 * Detects clusters, internal propagation risks, and seasonal context.
 */
public class EpidemiologicalDetector {

    // ── Inner classes ────────────────────────────────────────────

    public static class ThreatLevel {
        public int    niveau;          // 0-4
        public String label;
        public String description;
        public String couleur;
        public boolean clusterDetecte;
        public int    nbPatientsCluster;
        public String syndromeCluster;
        /** Total contagious patients in the waiting room — cached here to avoid a second DB query. */
        public int    nbContagieux;

        public ThreatLevel(int niveau, String label, String description, String couleur) {
            this.niveau      = niveau;
            this.label       = label;
            this.description = description;
            this.couleur     = couleur;
        }
    }

    public static class ExposureAlert {
        public String nomPatient;
        public String syndrome;
        public String contagionFlag;
        public long   minutesEnAttente;
        public int    urgenceScore;

        public ExposureAlert(String nomPatient, String syndrome,
                             String contagionFlag, long minutesEnAttente, int urgenceScore) {
            this.nomPatient      = nomPatient;
            this.syndrome        = syndrome;
            this.contagionFlag   = contagionFlag;
            this.minutesEnAttente = minutesEnAttente;
            this.urgenceScore    = urgenceScore;
        }
    }

    public static class SeasonInfo {
        public String       nom;
        public int          mois;
        public List<String> maladiesAttendues;

        public SeasonInfo(String nom, int mois, List<String> maladiesAttendues) {
            this.nom              = nom;
            this.mois             = mois;
            this.maladiesAttendues = maladiesAttendues;
        }
    }

    // ── Connection helper ────────────────────────────────────────

    private Connection getCnx() {
        return MyConnection.getInstance().getCnx();
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Calculates the overall threat level based on WHO alerts and local data.
     */
    public ThreatLevel calculerNiveauMenace(List<WHOFeedService.WHOAlert> whoAlerts) {
        // Threat level is based on LOCAL data only — WHO alerts are informational,
        // not a trigger for hospital-level threat escalation.
        int nbContagieux = getNbContagieuxEnAttente();

        if (nbContagieux >= 3) {
            ThreatLevel t = new ThreatLevel(4, "CRITIQUE",
                    "Plusieurs patients contagieux en attente. Isolement immediat requis.", "#B91C1C");
            t.clusterDetecte = true;
            t.nbPatientsCluster = nbContagieux;
            t.nbContagieux      = nbContagieux;
            t.syndromeCluster = "Multiple";
            return t;
        }
        if (nbContagieux >= 1) {
            ThreatLevel t = new ThreatLevel(2, "MODERE",
                    "Patient(s) potentiellement contagieux detecte(s). Surveillance renforcee.", "#B45309");
            t.clusterDetecte = true;
            t.nbPatientsCluster = nbContagieux;
            t.nbContagieux      = nbContagieux;
            t.syndromeCluster = "A determiner";
            return t;
        }
        ThreatLevel t = new ThreatLevel(0, "FAIBLE",
                "Aucun signal epidemiologique local detecte. Situation normale.", "#15803D");
        t.clusterDetecte = false;
        t.nbContagieux   = 0;
        return t;
    }

    /**
     * Groups active triages by syndrome category to detect clusters (>= 2 same syndrome).
     */
    public Map<String, List<Triage>> detecterClusters(List<Triage> triages) {
        Map<String, List<Triage>> groups = new LinkedHashMap<>();
        if (triages == null) return groups;
        for (Triage t : triages) {
            String cat = t.getSyndromeCategory();
            if (cat == null || cat.isEmpty()) continue;
            groups.computeIfAbsent(cat, k -> new ArrayList<>()).add(t);
        }
        // Keep only groups with >= 2 patients (cluster threshold)
        groups.entrySet().removeIf(e -> e.getValue().size() < 2);
        return groups;
    }

    /**
     * Detects contagious patients currently in the waiting room (internal propagation risk).
     */
    public List<ExposureAlert> detecterPropagationInterne() {
        List<ExposureAlert> alerts = new ArrayList<>();
        String sql = "SELECT t.id_triage, p.nom, p.prenom, t.syndrome_category, " +
                     "t.contagion_flag, t.date_heure_triage " +
                     "FROM triage t " +
                     "JOIN admissions a ON t.id_admission = a.id_admission " +
                     "JOIN patients p ON a.id_patient = p.id_patient " +
                     "WHERE t.patient_state = 'WaitingRoom' " +
                     "AND t.contagion_flag IN ('possible','confirme')";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String nom   = rs.getString("nom") + " " + rs.getString("prenom");
                String synd  = rs.getString("syndrome_category");
                String flag  = rs.getString("contagion_flag");
                Timestamp ts = rs.getTimestamp("date_heure_triage");
                long minutes = 0;
                if (ts != null) {
                    minutes = java.time.Duration.between(
                            ts.toLocalDateTime(), LocalDateTime.now()).toMinutes();
                }
                int urgence = flag.equals("confirme") ? Math.min(100, (int)(minutes * 2)) : (int)(minutes);
                alerts.add(new ExposureAlert(nom, synd != null ? synd : "Inconnu",
                        flag, minutes, urgence));
            }
        } catch (SQLException e) {
            System.out.println("detecterPropagationInterne: " + e.getMessage());
        }
        return alerts;
    }

    /**
     * Returns syndrome case counts for the last 24 hours.
     */
    public Map<String, Integer> getSyndromeStats24h() {
        Map<String, Integer> stats = new LinkedHashMap<>();
        // Pre-populate with all categories so the chart always shows them
        for (String cat : new String[]{"Respiratoire","Digestif","Neurologique",
                                        "Cutane","Cardiovasculaire","Trauma","Autre"}) {
            stats.put(cat, 0);
        }
        String sql = "SELECT syndrome_category, COUNT(*) as nb FROM triage " +
                     "WHERE date_heure_triage >= NOW() - INTERVAL 24 HOUR " +
                     "AND syndrome_category IS NOT NULL " +
                     "GROUP BY syndrome_category";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String cat = rs.getString("syndrome_category");
                int nb     = rs.getInt("nb");
                stats.put(cat, nb);
            }
        } catch (SQLException e) {
            System.out.println("getSyndromeStats24h: " + e.getMessage());
        }
        return stats;
    }

    /**
     * Returns the number of contagious patients currently in the waiting room.
     */
    public int getNbContagieuxEnAttente() {
        String sql = "SELECT COUNT(*) FROM triage " +
                     "WHERE patient_state = 'WaitingRoom' " +
                     "AND contagion_flag IN ('possible','confirme')";
        try (Connection c = getCnx();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("getNbContagieuxEnAttente: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Returns seasonal context based on the current month.
     */
    public SeasonInfo getSaisonActuelle() {
        int mois = LocalDateTime.now().getMonthValue();
        String nom;
        List<String> maladies;

        if (mois >= 12 || mois <= 2) {
            nom = "Hiver";
            maladies = Arrays.asList("Grippe saisonniere", "Bronchiolite", "Gastro-enterite",
                    "Pneumonie", "COVID-19");
        } else if (mois <= 5) {
            nom = "Printemps";
            maladies = Arrays.asList("Allergies respiratoires", "Rhinite allergique",
                    "Conjonctivite", "Asthme");
        } else if (mois <= 8) {
            nom = "Ete";
            maladies = Arrays.asList("Insolation", "Deshydratation", "Gastro-enterite",
                    "Infections cutanees", "Noyade");
        } else {
            nom = "Automne";
            maladies = Arrays.asList("Grippe (debut saison)", "Bronchite",
                    "Gastro-enterite", "Infections ORL");
        }
        return new SeasonInfo(nom, mois, maladies);
    }

    /**
     * Auto-suggests a syndrome category from symptom text.
     */
    public String autoClassifierSyndrome(String symptomes) {
        if (symptomes == null || symptomes.isEmpty()) return "Autre";
        String s = symptomes.toLowerCase();
        if (s.contains("toux") || s.contains("essoufflement") || s.contains("respir"))
            return "Respiratoire";
        if (s.contains("nausee") || s.contains("vomiss") || s.contains("diarrhee") || s.contains("abdomin"))
            return "Digestif";
        if (s.contains("cephalee") || s.contains("convuls") || s.contains("conscience") || s.contains("neurolog"))
            return "Neurologique";
        if (s.contains("eruption") || s.contains("rash") || s.contains("peau") || s.contains("cutane"))
            return "Cutane";
        if (s.contains("thoracique") || s.contains("cardiaque") || s.contains("palpitation"))
            return "Cardiovasculaire";
        if (s.contains("trauma") || s.contains("fracture") || s.contains("chute") || s.contains("accident"))
            return "Trauma";
        return "Autre";
    }
}

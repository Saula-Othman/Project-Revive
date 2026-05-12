package pro.revive.services.ServicesLabo;

import pro.revive.entities.EntitiesLabo.Examens_demandes;
import pro.revive.entities.EntitiesLabo.Resultats;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * ══════════════════════════════════════════════════════════════════
 *  REVIVE — Score de Gravité Intelligent du Patient
 *  Calcule un score 0-100 basé sur :
 *    1. Type d'examen (poids selon criticité médicale)
 *    2. Urgence déclarée (booléen)
 *    3. Analyse sémantique du compte rendu (mots-clés médicaux)
 *    4. État IA (Propre / Grave)
 * ══════════════════════════════════════════════════════════════════
 */
public class GraviteService {

    // ─────────────────────────────────────────────────────────────────────────
    // RÉSULTAT DU CALCUL
    // ─────────────────────────────────────────────────────────────────────────

    public static class ScoreResult {
        public final int    score;          // 0-100
        public final String niveau;         // Faible / Moyen / Élevé / Critique
        public final String recommandation; // texte automatique
        public final String detail;         // explication du calcul

        public ScoreResult(int score, String niveau, String recommandation, String detail) {
            this.score          = score;
            this.niveau         = niveau;
            this.recommandation = recommandation;
            this.detail         = detail;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DICTIONNAIRE MOTS-CLÉS MÉDICAUX → SCORE
    // ─────────────────────────────────────────────────────────────────────────

    /** Mots-clés CRITIQUES → +35 pts */
    private static final String[] MOTS_CRITIQUES = {
        "infarctus", "arrêt cardiaque", "arrêt respiratoire", "hémorragie massive",
        "choc septique", "choc hémorragique", "embolie pulmonaire massive",
        "avc hémorragique", "rupture", "perforation", "tamponnade",
        "dissection aortique", "méningite", "encéphalite", "coma",
        "détresse respiratoire", "insuffisance respiratoire aiguë",
        "ischémie", "nécrose", "gangrène", "sepsis sévère"
    };

    /** Mots-clés GRAVES → +25 pts */
    private static final String[] MOTS_GRAVES = {
        "pneumonie", "pneumothorax", "fracture ouverte", "fracture déplacée",
        "hémorragie", "hématome", "tumeur", "masse suspecte", "lésion maligne",
        "cancer", "métastase", "thrombose", "embolie", "avc", "accident vasculaire",
        "insuffisance cardiaque", "insuffisance rénale", "insuffisance hépatique",
        "abcès", "péritonite", "occlusion", "sténose sévère", "grave",
        "sévère", "critique", "urgent", "anormal", "pathologique",
        "positif", "suspect", "douteux", "inquiétant", "alarme"
    };

    /** Mots-clés MODÉRÉS → +15 pts */
    private static final String[] MOTS_MODERES = {
        "fracture", "contusion", "infection", "inflammation", "oedème",
        "épanchement", "calcification", "nodule", "kyste", "lithiase",
        "calcul", "sténose", "dilatation", "hypertrophie", "atrophie",
        "anomalie", "irrégularité", "asymétrie", "opacité", "hyperdensité",
        "hypodensité", "consolidation", "atélectasie", "pleurésie",
        "péricardite", "myocardite", "arythmie", "tachycardie", "bradycardie",
        "hypertension", "hypotension", "anémie", "leucocytose", "thrombopénie"
    };

    /** Mots-clés NORMAUX → -10 pts (résultat rassurant) */
    private static final String[] MOTS_NORMAUX = {
        "normal", "sans anomalie", "sans particularité", "rien à signaler",
        "négatif", "propre", "satisfaisant", "dans les normes", "dans les limites",
        "absence de", "pas de lésion", "pas d'anomalie", "pas de fracture",
        "pas de masse", "pas de tumeur", "pas d'épanchement", "intact",
        "régulier", "homogène", "symétrique", "bien visible", "bon aspect"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // POIDS DES TYPES D'EXAMENS
    // ─────────────────────────────────────────────────────────────────────────

    /** Types d'examens avec leur score de base (criticité intrinsèque) */
    private static final LinkedHashMap<String, Integer> POIDS_EXAMENS = new LinkedHashMap<>() {{
        // Imageries critiques
        put("scanner",          20); put("tdm",             20); put("tomodensitométrie", 20);
        put("irm",              18); put("imagerie par résonance", 18);
        put("angiographie",     22); put("artériographie",  22);
        put("scintigraphie",    16); put("pet scan",        20); put("pet-scan", 20);
        // Imageries standard
        put("radiographie",     10); put("radio",           10); put("rx",              10);
        put("échographie",      12); put("echo",            12); put("doppler",         14);
        put("mammographie",     14); put("ostéodensitométrie", 8);
        // Analyses biologiques critiques
        put("troponine",        25); put("d-dimères",       22); put("d dimères",       22);
        put("bnp",              20); put("nt-probnp",       20); put("lactates",        18);
        put("gaz du sang",      20); put("gazométrie",      20); put("bilan coagulation", 16);
        put("tp",               14); put("tca",             14); put("fibrinogène",     14);
        // Analyses biologiques standard
        put("nfs",              10); put("numération",      10); put("hémogramme",      10);
        put("bilan hépatique",  12); put("bilan rénal",     12); put("ionogramme",      12);
        put("glycémie",          8); put("hba1c",            8); put("lipase",          14);
        put("amylase",          12); put("crp",             10); put("vs",               8);
        put("procalcitonine",   16); put("hémoculture",     18); put("ecbu",            10);
        put("sérologie",        10); put("pcr",             12);
    }};

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTHODE PRINCIPALE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcule le score de gravité d'un résultat médical.
     * Version standard — sans prédiction IA radio.
     */
    public static ScoreResult calculerScore(Resultats r, Examens_demandes e) {
        return calculerScoreAvecIA(r, e, null, 0.0);
    }

    /**
     * Calcule le score de gravité en intégrant la prédiction IA radio.
     *
     * @param r              L'entité Resultats
     * @param e              L'entité Examens_demandes
     * @param predictionIA   "NORMAL", "PNEUMONIA" ou null si pas d'IA radio
     * @param confianceIA    Confiance de l'IA (0-100)
     */
    public static ScoreResult calculerScoreAvecIA(Resultats r, Examens_demandes e,
                                                   String predictionIA, double confianceIA) {
        int score = 0;
        StringBuilder detail = new StringBuilder();

        // ── 1. Score de base selon le type d'examen
        int scoreExamen = 0;
        if (e != null && e.getTypeExamen() != null) {
            String type = e.getTypeExamen().toLowerCase()
                    .replace("[analyse] ", "")
                    .replace("[imagerie] ", "");
            for (Map.Entry<String, Integer> entry : POIDS_EXAMENS.entrySet()) {
                if (type.contains(entry.getKey())) {
                    scoreExamen = Math.max(scoreExamen, entry.getValue());
                }
            }
            if (scoreExamen > 0) {
                detail.append("Type examen (+").append(scoreExamen).append(") ");
            }
        }
        score += scoreExamen;

        // ── 2. Bonus urgence déclarée
        if (e != null && e.isUrgent()) {
            score += 20;
            detail.append("Urgent (+20) ");
        }

        // ── 3. Analyse sémantique du compte rendu
        int scoreCR = 0;
        if (r != null && r.getCompteRendu() != null) {
            String cr = r.getCompteRendu().toLowerCase();

            for (String mot : MOTS_CRITIQUES) {
                if (cr.contains(mot)) {
                    scoreCR = Math.max(scoreCR, 35);
                    detail.append("Mot critique '").append(mot).append("' (+35) ");
                    break;
                }
            }
            if (scoreCR < 35) {
                for (String mot : MOTS_GRAVES) {
                    if (cr.contains(mot)) {
                        scoreCR = Math.max(scoreCR, 25);
                        detail.append("Mot grave '").append(mot).append("' (+25) ");
                        break;
                    }
                }
            }
            if (scoreCR < 25) {
                for (String mot : MOTS_MODERES) {
                    if (cr.contains(mot)) {
                        scoreCR = Math.max(scoreCR, 15);
                        detail.append("Mot modéré '").append(mot).append("' (+15) ");
                        break;
                    }
                }
            }
            for (String mot : MOTS_NORMAUX) {
                if (cr.contains(mot)) {
                    scoreCR = Math.min(scoreCR, 5);
                    detail.append("Mot normal '").append(mot).append("' (réduction) ");
                    break;
                }
            }
        }
        score += scoreCR;

        // ── 4. Bonus état IA (Propre / Grave)
        if (r != null && r.getEtat() != null) {
            if ("Grave".equalsIgnoreCase(r.getEtat())) {
                score += 15;
                detail.append("État IA=Grave (+15) ");
            } else if ("Propre".equalsIgnoreCase(r.getEtat())) {
                score = Math.max(0, score - 10);
                detail.append("État IA=Propre (-10) ");
            }
        }

        // ── 5. INTÉGRATION PRÉDICTION IA RADIO (DenseNet121)
        // Règle de cohérence : si l'IA détecte une anomalie (PNEUMONIA),
        // le score ne peut PAS être Faible — minimum forcé à Élevé (65)
        if (predictionIA != null && !predictionIA.isBlank()) {
            if ("PNEUMONIA".equalsIgnoreCase(predictionIA)) {
                // Anomalie détectée par l'IA → score minimum selon la confiance
                int scoreMinIA;
                if (confianceIA >= 90)      scoreMinIA = 82; // Critique
                else if (confianceIA >= 70) scoreMinIA = 65; // Élevé
                else                        scoreMinIA = 45; // Modéré (incertain)

                if (score < scoreMinIA) {
                    detail.append("IA radio=PNEUMONIA (score forcé à ").append(scoreMinIA).append(") ");
                    score = scoreMinIA;
                } else {
                    detail.append("IA radio=PNEUMONIA (+confirmation) ");
                }
            } else if ("NORMAL".equalsIgnoreCase(predictionIA)) {
                // IA dit normal → légère réduction si score était élevé sans raison
                if (score > 30 && scoreCR == 0) {
                    score = Math.max(score - 10, 10);
                    detail.append("IA radio=NORMAL (-10) ");
                }
            }
        }

        // ── 6. INTÉGRATION ANALYSE BIOLOGIQUE
        // Si l'analyse bio détecte des anomalies critiques → score minimum Modéré
        if (r != null && r.getCompteRendu() != null && !r.getCompteRendu().isBlank()) {
            boolean estAnalyse = r.getFichierJoint() == null || r.getFichierJoint().isBlank();
            if (estAnalyse) {
                // Pass the etat so Grave results are always flagged correctly
                AnalyseBiologiqueService.ResultatAnalyse bio =
                        AnalyseBiologiqueService.analyserAvecEtat(r.getCompteRendu(), r.getEtat());
                if ("Critique".equals(bio.niveauAttention) && score < 65) {
                    score = 65;
                    detail.append("Bio=Critique (score forcé à 65) ");
                } else if ("Modéré".equals(bio.niveauAttention) && score < 35) {
                    score = 35;
                    detail.append("Bio=Modéré (score forcé à 35) ");
                }
            }
        }

        // ── Clamp 0-100
        score = Math.max(0, Math.min(100, score));

        String niveau         = determinerNiveau(score);
        String recommandation = determinerRecommandation(score, niveau, e);

        return new ScoreResult(score, niveau, recommandation, detail.toString().trim());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NIVEAU DE GRAVITÉ
    // ─────────────────────────────────────────────────────────────────────────

    public static String determinerNiveau(int score) {
        if (score <= 30)  return "Faible";
        if (score <= 60)  return "Moyen";
        if (score <= 80)  return "Élevé";
        return "Critique";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECOMMANDATION AUTOMATIQUE
    // ─────────────────────────────────────────────────────────────────────────

    private static String determinerRecommandation(int score, String niveau,
                                                    Examens_demandes e) {
        String typeExamen = (e != null && e.getTypeExamen() != null)
                ? e.getTypeExamen().replace("[ANALYSE] ", "").replace("[IMAGERIE] ", "")
                : "examen";

        return switch (niveau) {
            case "Critique" -> "🚨 URGENCE IMMÉDIATE — Alerter le médecin traitant sans délai. " +
                    "Mise en observation immédiate du patient. " +
                    "Résultat de " + typeExamen + " nécessite une prise en charge urgente.";
            case "Élevé"    -> "⚠️ PRIORITÉ MÉDICALE — Informer le médecin dans les 2 heures. " +
                    "Surveillance rapprochée recommandée. " +
                    "Envisager des examens complémentaires pour " + typeExamen + ".";
            case "Moyen"    -> "📋 SURVEILLANCE — Consulter le médecin traitant dans la journée. " +
                    "Suivi régulier conseillé. " +
                    "Résultat de " + typeExamen + " à interpréter en contexte clinique.";
            default         -> "✅ ROUTINE — Résultat de " + typeExamen + " dans les normes attendues. " +
                    "Suivi de routine recommandé. Prochain contrôle selon protocole habituel.";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COULEUR DU BADGE (pour l'UI JavaFX)
    // ─────────────────────────────────────────────────────────────────────────

    /** Retourne la couleur de fond du badge selon le niveau */
    public static String couleurFond(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "#7F1D1D";
            case "Élevé"    -> "#DC2626";
            case "Moyen"    -> "#EA580C";
            default         -> "#16A34A";
        };
    }

    /** Retourne la couleur de fond clair (pour les cartes) */
    public static String couleurFondClair(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "#FEF2F2";
            case "Élevé"    -> "#FEF2F2";
            case "Moyen"    -> "#FFF7ED";
            default         -> "#F0FDF4";
        };
    }

    /** Retourne la couleur du texte selon le niveau */
    public static String couleurTexte(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "#7F1D1D";
            case "Élevé"    -> "#DC2626";
            case "Moyen"    -> "#EA580C";
            default         -> "#16A34A";
        };
    }

    /** Retourne l'emoji du niveau */
    public static String emoji(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "🔴";
            case "Élevé"    -> "🟠";
            case "Moyen"    -> "🟡";
            default         -> "🟢";
        };
    }
}

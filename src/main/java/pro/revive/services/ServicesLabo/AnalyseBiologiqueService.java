package pro.revive.services.ServicesLabo;

import java.util.ArrayList;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════════
 *  REVIVE — Analyseur Intelligent de Comptes Rendus Biologiques
 *
 *  Détecte automatiquement les biomarqueurs anormaux dans le texte
 *  du compte rendu et génère :
 *    - Une liste d'anomalies détectées
 *    - Un niveau d'attention global
 *    - Une interprétation médicale simplifiée
 *    - Une aide à la décision médicale
 * ══════════════════════════════════════════════════════════════════
 */
public class AnalyseBiologiqueService {

    // ─────────────────────────────────────────────────────────────────────────
    // MODÈLE DE RÉSULTAT
    // ─────────────────────────────────────────────────────────────────────────

    public static class ResultatAnalyse {
        public final List<Anomalie>  anomalies;       // liste des anomalies détectées
        public final String          niveauAttention; // Faible / Modéré / Critique
        public final String          interpretation;  // texte d'interprétation
        public final String          aideDecision;    // recommandation médicale
        public final boolean         anomalieDetectee;

        public ResultatAnalyse(List<Anomalie> anomalies, String niveauAttention,
                               String interpretation, String aideDecision) {
            this.anomalies        = anomalies;
            this.niveauAttention  = niveauAttention;
            this.interpretation   = interpretation;
            this.aideDecision     = aideDecision;
            this.anomalieDetectee = !anomalies.isEmpty();
        }
    }

    public static class Anomalie {
        public final String biomarqueur;   // ex: "Troponine"
        public final String statut;        // "élevé" / "bas" / "anormal"
        public final String signification; // explication médicale
        public final String niveau;        // Faible / Modéré / Critique
        public final String emoji;

        public Anomalie(String biomarqueur, String statut,
                        String signification, String niveau, String emoji) {
            this.biomarqueur   = biomarqueur;
            this.statut        = statut;
            this.signification = signification;
            this.niveau        = niveau;
            this.emoji         = emoji;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DICTIONNAIRE DES BIOMARQUEURS
    // ─────────────────────────────────────────────────────────────────────────

    private record Marqueur(
            String[]  motsCles,
            String    biomarqueur,
            String    statut,
            String    signification,
            String    niveau,
            String    emoji
    ) {}

    private static final List<Marqueur> MARQUEURS = List.of(

        // ── CARDIAQUE
        new Marqueur(
            new String[]{"troponine élevée","troponine haute","troponine augmentée",
                         "troponine positive","troponine > ","troponine ↑"},
            "Troponine", "élevée",
            "Marqueur de lésion myocardique — risque d'infarctus du myocarde",
            "Critique", "❤️"
        ),
        new Marqueur(
            new String[]{"bnp élevé","bnp augmenté","nt-probnp élevé","bnp >","bnp ↑"},
            "BNP / NT-proBNP", "élevé",
            "Marqueur d'insuffisance cardiaque — surcharge volumique",
            "Critique", "💔"
        ),
        new Marqueur(
            new String[]{"ck-mb élevé","ck mb élevé","créatine kinase élevée","ck élevé"},
            "CK-MB", "élevée",
            "Enzyme cardiaque — possible nécrose myocardique",
            "Critique", "🫀"
        ),

        // ── INFLAMMATION / INFECTION
        new Marqueur(
            new String[]{"crp élevée","crp augmentée","crp haute","crp >","crp ↑",
                         "protéine c réactive élevée","protéine c-réactive élevée"},
            "CRP", "élevée",
            "Syndrome inflammatoire — infection bactérienne ou virale possible",
            "Modéré", "🔥"
        ),
        new Marqueur(
            new String[]{"procalcitonine élevée","pct élevée","procalcitonine >","pct >"},
            "Procalcitonine", "élevée",
            "Marqueur d'infection bactérienne sévère — risque de sepsis",
            "Critique", "🦠"
        ),
        new Marqueur(
            new String[]{"leucocytes élevés","globules blancs élevés","hyperleucocytose",
                         "leucocytose","gb élevés","gb >","leucocytes >","wbc élevé"},
            "Leucocytes", "élevés",
            "Hyperleucocytose — réponse inflammatoire ou infectieuse",
            "Modéré", "🔬"
        ),
        new Marqueur(
            new String[]{"leucopénie","leucocytes bas","globules blancs bas","gb bas","gb <"},
            "Leucocytes", "bas",
            "Leucopénie — immunodépression ou aplasie médullaire possible",
            "Modéré", "⚠️"
        ),
        new Marqueur(
            new String[]{"ferritine basse","ferritine diminuée","ferritine <","ferritine faible"},
            "Ferritine", "basse",
            "Carence martiale — anémie ferriprive probable",
            "Modéré", "🩸"
        ),
        new Marqueur(
            new String[]{"ferritine élevée","ferritine haute","ferritine augmentée","ferritine >"},
            "Ferritine", "élevée",
            "Hyperferritinémie — inflammation, surcharge en fer ou hépatopathie",
            "Modéré", "⚡"
        ),

        // ── HÉMATOLOGIE
        new Marqueur(
            new String[]{"hémoglobine basse","hb basse","hb <","anémie","hémoglobine diminuée",
                         "hémoglobine faible","hgb bas","hgb <"},
            "Hémoglobine", "basse",
            "Anémie — fatigue, dyspnée, risque de décompensation cardiaque",
            "Modéré", "🩸"
        ),
        new Marqueur(
            new String[]{"thrombopénie","plaquettes basses","plaquettes <","plaquettes diminuées"},
            "Plaquettes", "basses",
            "Thrombopénie — risque hémorragique augmenté",
            "Critique", "💉"
        ),
        new Marqueur(
            new String[]{"plaquettes élevées","thrombocytose","plaquettes >","plaquettes augmentées"},
            "Plaquettes", "élevées",
            "Thrombocytose — risque thrombotique possible",
            "Faible", "🔴"
        ),

        // ── RÉNALE
        new Marqueur(
            new String[]{"créatinine élevée","créatinine augmentée","créatinine >","créatinine ↑",
                         "insuffisance rénale","ira","irc","créatininémie élevée"},
            "Créatinine", "élevée",
            "Insuffisance rénale — altération de la filtration glomérulaire",
            "Critique", "🫘"
        ),
        new Marqueur(
            new String[]{"urée élevée","urée augmentée","urée >","hyperurémie"},
            "Urée", "élevée",
            "Hyperurémie — insuffisance rénale ou déshydratation",
            "Modéré", "⚗️"
        ),
        new Marqueur(
            new String[]{"potassium élevé","hyperkaliémie","k+ élevé","kaliémie élevée"},
            "Potassium", "élevé",
            "Hyperkaliémie — risque de troubles du rythme cardiaque",
            "Critique", "⚡"
        ),
        new Marqueur(
            new String[]{"potassium bas","hypokaliémie","k+ bas","kaliémie basse"},
            "Potassium", "bas",
            "Hypokaliémie — faiblesse musculaire, troubles du rythme",
            "Modéré", "⚡"
        ),

        // ── MÉTABOLIQUE / ENDOCRINE
        new Marqueur(
            new String[]{"glycémie élevée","hyperglycémie","glucose élevé","glycémie >",
                         "diabète décompensé","glycémie augmentée","sucre élevé"},
            "Glycémie", "élevée",
            "Hyperglycémie — diabète décompensé ou stress métabolique",
            "Modéré", "🍬"
        ),
        new Marqueur(
            new String[]{"glycémie basse","hypoglycémie","glucose bas","glycémie <"},
            "Glycémie", "basse",
            "Hypoglycémie — risque de malaise, confusion, coma",
            "Critique", "⚠️"
        ),
        new Marqueur(
            new String[]{"tsh élevée","hypothyroïdie","tsh >","tsh augmentée"},
            "TSH", "élevée",
            "Hypothyroïdie — ralentissement métabolique",
            "Faible", "🦋"
        ),
        new Marqueur(
            new String[]{"tsh basse","hyperthyroïdie","tsh <","tsh diminuée"},
            "TSH", "basse",
            "Hyperthyroïdie — tachycardie, amaigrissement, tremblements",
            "Modéré", "🦋"
        ),

        // ── HÉPATIQUE
        new Marqueur(
            new String[]{"transaminases élevées","alat élevée","asat élevée","got élevé",
                         "gpt élevé","cytolyse","transaminases augmentées","tgo élevé","tgp élevé"},
            "Transaminases", "élevées",
            "Cytolyse hépatique — hépatite, toxicité médicamenteuse ou alcoolique",
            "Modéré", "🫁"
        ),
        new Marqueur(
            new String[]{"bilirubine élevée","hyperbilirubinémie","ictère","jaunisse","bilirubine >"},
            "Bilirubine", "élevée",
            "Hyperbilirubinémie — ictère, hémolyse ou obstruction biliaire",
            "Modéré", "🟡"
        ),
        new Marqueur(
            new String[]{"lipase élevée","amylase élevée","pancréatite","lipase >","amylase >"},
            "Lipase / Amylase", "élevée",
            "Pancréatite aiguë probable — douleur abdominale sévère",
            "Critique", "🔴"
        ),

        // ── COAGULATION
        new Marqueur(
            new String[]{"tp bas","tp <","taux de prothrombine bas","inr élevé","inr >",
                         "trouble de coagulation","coagulopathie"},
            "TP / INR", "anormal",
            "Trouble de la coagulation — risque hémorragique",
            "Critique", "💉"
        ),
        new Marqueur(
            new String[]{"d-dimères élevés","d dimères élevés","d-dimères >","ddimères élevés"},
            "D-Dimères", "élevés",
            "Activation de la coagulation — embolie pulmonaire ou thrombose à exclure",
            "Critique", "🩺"
        )
    );

    // ─────────────────────────────────────────────────────────────────────────
    // MÉTHODE PRINCIPALE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Analyse le texte du compte rendu et détecte les anomalies biologiques.
     * IMPORTANT: If etat='Grave' is explicitly set, always returns at least Critique level.
     *
     * @param compteRendu  Texte du compte rendu médical
     * @return             ResultatAnalyse avec anomalies, niveau, interprétation
     */
    public static ResultatAnalyse analyser(String compteRendu) {
        return analyserAvecEtat(compteRendu, null);
    }

    /**
     * Analyse avec prise en compte explicite de l'état (Grave/Propre).
     * Si etat='Grave', le niveau minimum est Critique même sans mots-clés.
     */
    public static ResultatAnalyse analyserAvecEtat(String compteRendu, String etat) {
        boolean estGrave = "Grave".equalsIgnoreCase(etat);

        if (compteRendu == null || compteRendu.isBlank()) {
            if (estGrave) {
                // etat=Grave but no compte rendu text — still flag as critical
                return new ResultatAnalyse(List.of(), "Critique",
                        "🔴 RÉSULTAT CLASSIFIÉ GRAVE\n\nL'état du résultat est marqué comme GRAVE.",
                        "🚨 ACTION IMMÉDIATE REQUISE\n• Alerter le médecin traitant sans délai\n• Surveillance rapprochée du patient");
            }
            return new ResultatAnalyse(List.of(), "Faible",
                    "Aucun compte rendu à analyser.", "Saisir le compte rendu pour activer l'analyse.");
        }

        String texte = compteRendu.toLowerCase();
        List<Anomalie> anomalies = new ArrayList<>();

        // Détecter chaque marqueur dans le texte
        for (Marqueur m : MARQUEURS) {
            for (String motCle : m.motsCles()) {
                if (texte.contains(motCle)) {
                    boolean dejaPresent = anomalies.stream()
                            .anyMatch(a -> a.biomarqueur.equals(m.biomarqueur())
                                    && a.statut.equals(m.statut()));
                    if (!dejaPresent) {
                        anomalies.add(new Anomalie(
                                m.biomarqueur(), m.statut(),
                                m.signification(), m.niveau(), m.emoji()));
                    }
                    break;
                }
            }
        }

        // ── KEY FIX: If etat='Grave' but no keywords found, add a generic critical anomaly
        if (estGrave && anomalies.isEmpty()) {
            anomalies.add(new Anomalie(
                "Résultat biologique", "anormal",
                "Résultat classifié GRAVE par le biologiste — anomalie significative détectée",
                "Critique", "🔴"
            ));
        }

        // ── If etat='Grave', ensure minimum level is Critique
        String niveauGlobal;
        if (anomalies.isEmpty()) {
            // No anomalies detected and etat is not Grave
            return new ResultatAnalyse(List.of(), "Faible",
                    "✅ Aucune anomalie biologique majeure détectée dans le compte rendu.",
                    "Résultat dans les normes. Suivi de routine recommandé.");
        } else {
            niveauGlobal = determinerNiveauGlobal(anomalies);
            // If etat=Grave, force minimum Critique
            if (estGrave && !"Critique".equals(niveauGlobal)) {
                niveauGlobal = "Critique";
            }
        }

        String interpretation = genererInterpretation(anomalies, niveauGlobal);
        String aideDecision   = genererAideDecision(anomalies, niveauGlobal);

        return new ResultatAnalyse(anomalies, niveauGlobal, interpretation, aideDecision);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NIVEAU GLOBAL
    // ─────────────────────────────────────────────────────────────────────────

    private static String determinerNiveauGlobal(List<Anomalie> anomalies) {
        boolean aCritique = anomalies.stream().anyMatch(a -> "Critique".equals(a.niveau));
        boolean aModere   = anomalies.stream().anyMatch(a -> "Modéré".equals(a.niveau));
        if (aCritique) return "Critique";
        if (aModere)   return "Modéré";
        return "Faible";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERPRÉTATION MÉDICALE
    // ─────────────────────────────────────────────────────────────────────────

    private static String genererInterpretation(List<Anomalie> anomalies, String niveau) {
        StringBuilder sb = new StringBuilder();

        switch (niveau) {
            case "Critique" -> sb.append("🔴 ANOMALIES CRITIQUES DÉTECTÉES\n\n");
            case "Modéré"   -> sb.append("🟡 ANOMALIES MODÉRÉES DÉTECTÉES\n\n");
            default         -> sb.append("🟢 ANOMALIES MINEURES DÉTECTÉES\n\n");
        }

        sb.append("Biomarqueurs anormaux identifiés :\n");
        for (Anomalie a : anomalies) {
            sb.append(a.emoji).append(" ").append(a.biomarqueur)
              .append(" ").append(a.statut).append(" — ")
              .append(a.signification).append("\n");
        }

        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AIDE À LA DÉCISION
    // ─────────────────────────────────────────────────────────────────────────

    private static String genererAideDecision(List<Anomalie> anomalies, String niveau) {
        StringBuilder sb = new StringBuilder();

        switch (niveau) {
            case "Critique" -> {
                sb.append("🚨 ACTION IMMÉDIATE REQUISE\n");
                sb.append("• Alerter le médecin traitant sans délai\n");
                sb.append("• Mettre le patient sous surveillance continue\n");
                sb.append("• Envisager une prise en charge urgente\n");
            }
            case "Modéré" -> {
                sb.append("⚠️ SURVEILLANCE RECOMMANDÉE\n");
                sb.append("• Informer le médecin dans les 2 heures\n");
                sb.append("• Contrôle biologique de suivi conseillé\n");
                sb.append("• Adapter la prise en charge selon le contexte clinique\n");
            }
            default -> {
                sb.append("📋 SUIVI DE ROUTINE\n");
                sb.append("• Informer le médecin lors de la prochaine consultation\n");
                sb.append("• Contrôle biologique selon protocole habituel\n");
            }
        }

        // Recommandations spécifiques par biomarqueur
        sb.append("\nRecommandations spécifiques :\n");
        for (Anomalie a : anomalies) {
            sb.append("• ").append(a.biomarqueur).append(" : ");
            sb.append(getRecommandationSpecifique(a.biomarqueur, a.statut)).append("\n");
        }

        return sb.toString().trim();
    }

    private static String getRecommandationSpecifique(String biomarqueur, String statut) {
        return switch (biomarqueur) {
            case "Troponine"        -> "ECG urgent + avis cardiologique";
            case "BNP / NT-proBNP" -> "Échocardiographie + diurétiques si indiqués";
            case "CK-MB"           -> "Surveillance ECG + enzymes cardiaques en série";
            case "CRP"             -> "Hémocultures si fièvre + antibiothérapie si indiquée";
            case "Procalcitonine"  -> "Bilan infectieux complet + antibiothérapie urgente";
            case "Leucocytes"      -> "statut".equals(statut) && statut.equals("élevés")
                    ? "Frottis sanguin + hémocultures si fièvre"
                    : "NFS complète + bilan immunologique";
            case "Ferritine"       -> "bas".equals(statut)
                    ? "Supplémentation en fer + recherche de saignement occulte"
                    : "Bilan hépatique + recherche d'hémochromatose";
            case "Hémoglobine"     -> "Bilan martial + réticulocytes + avis hématologique";
            case "Plaquettes"      -> "bas".equals(statut)
                    ? "Frottis sanguin + avis hématologique urgent"
                    : "Bilan de thrombophilie";
            case "Créatinine"      -> "Bilan rénal complet + ajustement des médicaments néphrotoxiques";
            case "Urée"            -> "Hydratation + surveillance de la diurèse";
            case "Potassium"       -> "élevé".equals(statut)
                    ? "ECG urgent + traitement hypokaliémiant"
                    : "Supplémentation potassique + surveillance ECG";
            case "Glycémie"        -> "élevée".equals(statut)
                    ? "Insulinothérapie si indiquée + surveillance glycémique"
                    : "Resucrage immédiat + surveillance neurologique";
            case "TSH"             -> "Bilan thyroïdien complet (T3, T4) + avis endocrinologique";
            case "Transaminases"   -> "Bilan hépatique complet + échographie abdominale";
            case "Bilirubine"      -> "Bilan hépatique + échographie des voies biliaires";
            case "Lipase / Amylase"-> "Mise à jeun + hydratation IV + scanner abdominal";
            case "TP / INR"        -> "Bilan de coagulation complet + avis hématologique";
            case "D-Dimères"       -> "Angio-scanner pulmonaire ou écho-doppler veineux";
            default                -> "Contrôle biologique de suivi recommandé";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COULEURS UI
    // ─────────────────────────────────────────────────────────────────────────

    public static String couleurNiveau(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "#DC2626";
            case "Modéré"   -> "#EA580C";
            default         -> "#16A34A";
        };
    }

    public static String couleurFondNiveau(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "#FEF2F2";
            case "Modéré"   -> "#FFF7ED";
            default         -> "#F0FDF4";
        };
    }

    public static String emojiNiveau(String niveau) {
        return switch (niveau != null ? niveau : "") {
            case "Critique" -> "🔴";
            case "Modéré"   -> "🟡";
            default         -> "🟢";
        };
    }
}

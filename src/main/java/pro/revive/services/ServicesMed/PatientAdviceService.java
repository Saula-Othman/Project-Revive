package pro.revive.services.ServicesMed;

import pro.revive.entities.EntitiesMed.AdviceData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service fournissant des conseils pratiques en francais par code ICD-10.
 * Base de donnees statique de conseils pour les pathologies courantes.
 */
public class PatientAdviceService {

    private static final Map<String, AdviceData> ADVICE_MAP = new HashMap<>();

    static {
        // ── J18.9 - Pneumonie ─────────────────────────────────────────────
        ADVICE_MAP.put("J18.9", new AdviceData(
            Arrays.asList(
                "Prenez tous vos antibiotiques jusqu'au bout, meme si vous vous sentez mieux",
                "Reposez-vous et buvez beaucoup d'eau (au moins 2 litres par jour)",
                "Evitez de fumer et les environnements enfumes",
                "Surveillez votre temperature 2 fois par jour"
            ),
            Arrays.asList(
                "Fievre persistante apres 48h de traitement",
                "Difficulte respiratoire qui s'aggrave",
                "Douleur thoracique intense ou crachats sanguinolents"
            ),
            "Infection pulmonaire necessitant un traitement antibiotique et du repos"
        ));

        // ── I10 - Hypertension ────────────────────────────────────────────
        ADVICE_MAP.put("I10", new AdviceData(
            Arrays.asList(
                "Prenez vos medicaments tous les jours a heure fixe",
                "Reduisez le sel dans votre alimentation (moins de 5g par jour)",
                "Pratiquez une activite physique reguliere (30 min de marche par jour)",
                "Controlez votre tension a domicile et notez les valeurs"
            ),
            Arrays.asList(
                "Maux de tete violents avec vision trouble",
                "Douleur thoracique ou essoufflement important",
                "Tension superieure a 180/110 mmHg"
            ),
            "Pression arterielle elevee necessitant un suivi regulier et un traitement"
        ));

        // ── E11 - Diabete type 2 ──────────────────────────────────────────
        ADVICE_MAP.put("E11", new AdviceData(
            Arrays.asList(
                "Controlez votre glycemie selon les recommandations de votre medecin",
                "Adoptez une alimentation equilibree, evitez les sucres rapides",
                "Pratiquez une activite physique reguliere",
                "Surveillez vos pieds quotidiennement (plaies, rougeurs)"
            ),
            Arrays.asList(
                "Glycemie superieure a 3g/L ou inferieure a 0.6g/L",
                "Soif intense, urines frequentes, fatigue extreme",
                "Plaie au pied qui ne guerit pas"
            ),
            "Trouble metabolique necessitant un controle de la glycemie et un suivi regulier"
        ));

        // ── K29 - Gastrite ────────────────────────────────────────────────
        ADVICE_MAP.put("K29", new AdviceData(
            Arrays.asList(
                "Mangez leger et fractionne (5-6 petits repas par jour)",
                "Evitez l'alcool, le cafe, les epices et les aliments acides",
                "Prenez vos medicaments anti-acides avant les repas",
                "Evitez l'aspirine et les anti-inflammatoires"
            ),
            Arrays.asList(
                "Vomissements de sang ou selles noires",
                "Douleur abdominale intense qui ne passe pas",
                "Perte de poids rapide et inexpliquee"
            ),
            "Inflammation de l'estomac necessitant un regime alimentaire adapte"
        ));

        // ── A09 - Gastro-enterite ─────────────────────────────────────────
        ADVICE_MAP.put("A09", new AdviceData(
            Arrays.asList(
                "Buvez beaucoup pour eviter la deshydratation (eau, bouillon, SRO)",
                "Reprenez progressivement une alimentation legere (riz, banane, compote)",
                "Lavez-vous les mains frequemment pour eviter la contagion",
                "Reposez-vous jusqu'a disparition complete des symptomes"
            ),
            Arrays.asList(
                "Signes de deshydratation (bouche seche, urines rares et foncees)",
                "Fievre elevee persistante (>39°C)",
                "Sang dans les selles ou vomissements incoercibles"
            ),
            "Infection digestive virale ou bacterienne necessitant une rehydratation"
        ));

        // ── S52 - Fracture ────────────────────────────────────────────────
        ADVICE_MAP.put("S52", new AdviceData(
            Arrays.asList(
                "Gardez le platre ou l'attelle propre et sec",
                "Surelevez le membre fracture pour reduire le gonflement",
                "Bougez les doigts/orteils regulierement pour maintenir la circulation",
                "Prenez les antalgiques prescrits si douleur"
            ),
            Arrays.asList(
                "Douleur intense qui ne passe pas avec les medicaments",
                "Doigts/orteils froids, bleus ou insensibles",
                "Odeur desagreable ou humidite sous le platre"
            ),
            "Fracture osseuse necessitant une immobilisation et un suivi orthopedique"
        ));

        // ── J06.9 - Infection respiratoire ────────────────────────────────
        ADVICE_MAP.put("J06.9", new AdviceData(
            Arrays.asList(
                "Reposez-vous et buvez beaucoup de liquides chauds",
                "Utilisez du paracetamol pour la fievre et les douleurs",
                "Lavez-vous les mains frequemment",
                "Aerez regulierement votre chambre"
            ),
            Arrays.asList(
                "Fievre superieure a 39°C pendant plus de 3 jours",
                "Difficulte a respirer ou douleur thoracique",
                "Aggravation des symptomes apres amelioration initiale"
            ),
            "Infection des voies respiratoires superieures d'origine virale"
        ));

        // ── N39.0 - Infection urinaire ────────────────────────────────────
        ADVICE_MAP.put("N39.0", new AdviceData(
            Arrays.asList(
                "Buvez au moins 2 litres d'eau par jour",
                "Prenez tous vos antibiotiques jusqu'au bout",
                "Urinez des que vous en ressentez le besoin",
                "Essuyez-vous d'avant en arriere (pour les femmes)"
            ),
            Arrays.asList(
                "Fievre elevee avec frissons",
                "Douleur lombaire intense",
                "Sang dans les urines ou urines troubles malodorantes"
            ),
            "Infection bacterienne des voies urinaires necessitant un traitement antibiotique"
        ));

        // ── M54.5 - Lombalgie ─────────────────────────────────────────────
        ADVICE_MAP.put("M54.5", new AdviceData(
            Arrays.asList(
                "Appliquez du chaud ou du froid selon ce qui vous soulage",
                "Restez actif, evitez le repos au lit prolonge",
                "Adoptez de bonnes postures au quotidien",
                "Pratiquez des etirements doux regulierement"
            ),
            Arrays.asList(
                "Douleur irradiant dans la jambe avec fourmillements",
                "Perte de force dans la jambe ou difficulte a marcher",
                "Troubles urinaires ou fecaux associes"
            ),
            "Douleur du bas du dos necessitant repos relatif et antalgiques"
        ));

        // ── I21 - Infarctus du myocarde ───────────────────────────────────
        ADVICE_MAP.put("I21", new AdviceData(
            Arrays.asList(
                "Prenez TOUS vos medicaments cardiologiques sans interruption",
                "Arretez completement le tabac",
                "Suivez un regime pauvre en graisses saturees et en sel",
                "Reprenez progressivement une activite physique adaptee (reeducation cardiaque)"
            ),
            Arrays.asList(
                "Douleur thoracique qui revient",
                "Essoufflement important au moindre effort",
                "Palpitations ou malaise avec sueurs"
            ),
            "Crise cardiaque necessitant un suivi cardiologique strict et un traitement a vie"
        ));

        // ── DEFAULT - Conseils generaux ───────────────────────────────────
        ADVICE_MAP.put("DEFAULT", new AdviceData(
            Arrays.asList(
                "Suivez scrupuleusement les prescriptions medicales",
                "Reposez-vous et hydratez-vous regulierement",
                "Surveillez l'evolution de vos symptomes",
                "N'hesitez pas a recontacter votre medecin en cas de doute"
            ),
            Arrays.asList(
                "Aggravation brutale de votre etat",
                "Apparition de nouveaux symptomes inquietants",
                "Fievre elevee persistante"
            ),
            "Suivi medical recommande selon les prescriptions"
        ));
    }

    /**
     * Recupere les conseils pour un code ICD-10.
     * Retourne les conseils par defaut si le code n'est pas trouve.
     * 
     * @param icdCode Code ICD-10 (ex: "J18.9")
     * @return AdviceData contenant conseils, alertes et description
     */
    public AdviceData getAdvice(String icdCode) {
        if (icdCode == null || icdCode.trim().isEmpty()) {
            return ADVICE_MAP.get("DEFAULT");
        }

        String code = icdCode.trim().toUpperCase();
        
        // Recherche exacte
        if (ADVICE_MAP.containsKey(code)) {
            System.out.println("[PatientAdvice] Conseils trouves pour " + code);
            return ADVICE_MAP.get(code);
        }

        // Recherche par prefixe (ex: J18.91 -> J18.9)
        for (String key : ADVICE_MAP.keySet()) {
            if (!key.equals("DEFAULT") && code.startsWith(key)) {
                System.out.println("[PatientAdvice] Conseils trouves par prefixe pour " 
                        + code + " -> " + key);
                return ADVICE_MAP.get(key);
            }
        }

        System.out.println("[PatientAdvice] Aucun conseil specifique pour " + code 
                + ", utilisation des conseils par defaut");
        return ADVICE_MAP.get("DEFAULT");
    }

    /**
     * Retourne la liste de tous les codes ICD supportes.
     */
    public List<String> getSupportedIcdCodes() {
        return ADVICE_MAP.keySet().stream()
                .filter(k -> !k.equals("DEFAULT"))
                .sorted()
              
                .collect(java.util.stream.Collectors.toList());
    }
}

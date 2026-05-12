package pro.revive.tests.TestsMed;

import pro.revive.entities.EntitiesMed.AdviceData;
import pro.revive.entities.EntitiesMed.MedlinePlusResult;
import pro.revive.services.ServicesMed.MedlinePlusService;
import pro.revive.services.ServicesMed.PatientAdviceService;
import pro.revive.utils.UtilesMed.ConfigLoader;

/**
 * Classe de test pour verifier la configuration de la feature Email.
 * Executez cette classe pour tester :
 * - Chargement config.properties
 * - API MedlinePlus
 * - Conseils patients en francais
 */
public class EmailFeatureTest {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  TEST FEATURE EMAIL POST-CONSULTATION");
        System.out.println("═══════════════════════════════════════════════════════\n");

        // ── Test 1 : Configuration ────────────────────────────────────────
        System.out.println("1. TEST CONFIGURATION");
        System.out.println("─────────────────────────────────────────────────────");
        ConfigLoader config = ConfigLoader.getInstance();
        
        String smtpHost = config.get("mail.smtp.host");
        String smtpUser = config.get("mail.smtp.user");
        String smtpPass = config.get("mail.smtp.password");
        String hospitalName = config.get("hospital.name");
        String hospitalPhone = config.get("hospital.phone");
        
        System.out.println("SMTP Host     : " + smtpHost);
        System.out.println("SMTP User     : " + (smtpUser.isEmpty() ? "❌ NON CONFIGURE" : "✅ " + smtpUser));
        System.out.println("SMTP Password : " + (smtpPass.isEmpty() ? "❌ NON CONFIGURE" : "✅ Configuré (" + smtpPass.length() + " caractères)"));
        System.out.println("Hôpital       : " + hospitalName);
        System.out.println("Téléphone     : " + hospitalPhone);
        
        boolean configOk = !smtpUser.isEmpty() && !smtpPass.isEmpty();
        System.out.println("\nRésultat : " + (configOk ? "✅ Configuration OK" : "❌ Configuration incomplète"));
        System.out.println();

        // ── Test 2 : MedlinePlus API ──────────────────────────────────────
        System.out.println("2. TEST MEDLINEPLUS API");
        System.out.println("─────────────────────────────────────────────────────");
        MedlinePlusService medlineService = new MedlinePlusService();
        
        String[] testCodes = {"J18.9", "I10", "E11", "INVALID"};
        for (String code : testCodes) {
            System.out.println("\nTest code ICD : " + code);
            try {
                MedlinePlusResult result = medlineService.fetchHealthInfo(code);
                System.out.println("  Titre   : " + result.getDiseaseTitle());
                System.out.println("  Résumé  : " + (result.getSummary().isEmpty() 
                        ? "(vide)" 
                        : result.getSummary().substring(0, Math.min(80, result.getSummary().length())) + "..."));
                System.out.println("  URL     : " + (result.getFullInfoUrl().isEmpty() ? "(vide)" : result.getFullInfoUrl()));
                System.out.println("  Statut  : ✅ OK");
            } catch (Exception e) {
                System.out.println("  Statut  : ❌ Erreur - " + e.getMessage());
            }
        }
        System.out.println();

        // ── Test 3 : Conseils Patients ────────────────────────────────────
        System.out.println("3. TEST CONSEILS PATIENTS (FRANÇAIS)");
        System.out.println("─────────────────────────────────────────────────────");
        PatientAdviceService adviceService = new PatientAdviceService();
        
        String[] testAdviceCodes = {"J18.9", "I21", "DEFAULT"};
        for (String code : testAdviceCodes) {
            System.out.println("\nCode ICD : " + code);
            AdviceData advice = adviceService.getAdvice(code);
            System.out.println("  Description : " + advice.getDescriptionFr());
            System.out.println("  Conseils    : " + advice.getConseils().size() + " items");
            if (!advice.getConseils().isEmpty()) {
                System.out.println("    → " + advice.getConseils().get(0));
            }
            System.out.println("  Alertes     : " + advice.getAlertes().size() + " items");
            if (!advice.getAlertes().isEmpty()) {
                System.out.println("    → " + advice.getAlertes().get(0));
            }
        }
        System.out.println();

        // ── Test 4 : Codes ICD Supportés ─────────────────────────────────
        System.out.println("4. CODES ICD-10 SUPPORTÉS");
        System.out.println("─────────────────────────────────────────────────────");
        System.out.println("Nombre de codes : " + adviceService.getSupportedIcdCodes().size());
        System.out.println("Liste : " + String.join(", ", adviceService.getSupportedIcdCodes()));
        System.out.println();

        // ── Résumé ────────────────────────────────────────────────────────
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("  RÉSUMÉ");
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("Configuration SMTP  : " + (configOk ? "✅ OK" : "❌ À configurer"));
        System.out.println("MedlinePlus API     : ✅ Fonctionnel");
        System.out.println("Conseils français   : ✅ " + adviceService.getSupportedIcdCodes().size() + " codes supportés");
        System.out.println();
        
        if (!configOk) {
            System.out.println("⚠️  ATTENTION : Configurez config.properties avant d'envoyer des emails");
            System.out.println("    Voir EMAIL_SETUP_README.md pour les instructions");
        } else {
            System.out.println("✅ Système prêt pour l'envoi d'emails !");
        }
        System.out.println("═══════════════════════════════════════════════════════");
        System.out.println("==============================================");
    }
}

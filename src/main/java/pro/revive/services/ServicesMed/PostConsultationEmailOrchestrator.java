package pro.revive.services.ServicesMed;

import pro.revive.entities.EntitiesMed.AdviceData;
import pro.revive.entities.EntitiesMed.Consultation;
import pro.revive.entities.EntitiesMed.MedlinePlusResult;
import pro.revive.entities.EntitiesMed.Ordonnance;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Orchestrateur pour l'envoi d'email post-consultation.
 * Utilise sa propre connexion JDBC independante (evite les conflits
 * avec la connexion singleton du thread principal JavaFX).
 */
public class PostConsultationEmailOrchestrator {

    // Memes parametres que MyConnection
    private static final String DB_URL   = "jdbc:mysql://127.0.0.1:3306/revive"
            + "?useSSL=false&characterEncoding=UTF-8"
            + "&useJDBCCompliantTimezoneShift=true"
            + "&useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String DB_USER  = "root";
    private static final String DB_PASS  = "";

    private final MedlinePlusService medlinePlusService = new MedlinePlusService();
    private final PatientAdviceService adviceService      = new PatientAdviceService();
    private final PatientEmailService  emailService       = new PatientEmailService();

    /**
     * Ouvre une connexion JDBC dediee pour ce thread.
     */
    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /**
     * Envoie l'email de maniere asynchrone (ne bloque jamais le thread UI).
     */
    public void sendAsync(int consultationId) {
        CompletableFuture.runAsync(() -> {
            System.out.println("[EmailOrchestrator] Demarrage envoi email pour consultation #" + consultationId);

            try (Connection cnx = openConnection()) {
                System.out.println("[EmailOrchestrator] Connexion DB dediee ouverte.");

                // 1. Charger la consultation
                Consultation consultation = getConsultationById(cnx, consultationId);
                if (consultation == null) {
                    System.err.println("[EmailOrchestrator] Consultation #" + consultationId + " introuvable");
                    return;
                }
                System.out.println("[EmailOrchestrator] Consultation chargee : admission #" + consultation.getIdAdmission());

                // 2. Charger le patient
                Map<String, Object> patient = getPatientByAdmission(cnx, consultation.getIdAdmission());
                if (patient == null) {
                    System.err.println("[EmailOrchestrator] Patient introuvable pour admission #" + consultation.getIdAdmission());
                    return;
                }
                String emailPatient = (String) patient.get("email");
                System.out.println("[EmailOrchestrator] Patient : " + patient.get("prenom") + " " + patient.get("nom") + " | email=" + emailPatient);
                if (emailPatient == null || emailPatient.trim().isEmpty()) {
                    System.out.println("[EmailOrchestrator] Patient sans email, envoi ignore.");
                    return;
                }

                // 3. Charger l'admission (constantes vitales)
                Map<String, Object> admission = getAdmissionDetails(cnx, consultation.getIdAdmission());

                // 4. Charger les ordonnances
                List<Ordonnance> ordonnances = getOrdonnances(cnx, consultationId);
                System.out.println("[EmailOrchestrator] " + ordonnances.size() + " ordonnance(s) chargee(s).");

                // 5. MedlinePlus
                MedlinePlusResult medlineInfo;
                String icd = consultation.getIcdCode();
                if (icd != null && !icd.trim().isEmpty()) {
                    medlineInfo = medlinePlusService.fetchHealthInfo(icd);
                } else {
                    String diag = consultation.getDiagnostic();
                    medlineInfo = new MedlinePlusResult(
                            diag != null ? diag : "Consultation medicale", "", "");
                }

                // 6. Conseils francais
                AdviceData adviceData = adviceService.getAdvice(icd != null ? icd : "DEFAULT");

                // 7. Envoi email
                emailService.sendPostConsultationEmail(patient, consultation, admission,
                        ordonnances, medlineInfo, adviceData);

            } catch (Exception e) {
                System.err.println("[EmailOrchestrator] Erreur : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // ── Requetes SQL independantes ────────────────────────────────────────

    private Consultation getConsultationById(Connection cnx, int id) {
        String sql = "SELECT c.id_consultation, c.id_admission, c.id_personnel_medecin, "
                   + "c.date_heure_debut, c.date_heure_fin, c.diagnostic, c.orientation, c.icd_code "
                   + "FROM consultations c WHERE c.id_consultation = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Consultation c = new Consultation();
                    c.setIdConsultation(rs.getInt("id_consultation"));
                    c.setIdAdmission(rs.getInt("id_admission"));
                    c.setIdPersonnelMedecin(rs.getInt("id_personnel_medecin"));
                    c.setDiagnostic(rs.getString("diagnostic"));
                    c.setOrientation(rs.getString("orientation"));
                    try { c.setIcdCode(rs.getString("icd_code")); } catch (SQLException ignored) {}
                    Timestamp d = rs.getTimestamp("date_heure_debut");
                    if (d != null) c.setDateHeureDebut(d.toLocalDateTime());
                    Timestamp f = rs.getTimestamp("date_heure_fin");
                    if (f != null) c.setDateHeureFin(f.toLocalDateTime());
                    return c;
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmailOrchestrator] SQL consultation : " + e.getMessage());
        }
        return null;
    }

    private Map<String, Object> getPatientByAdmission(Connection cnx, int idAdmission) {
        // Colonnes minimales — on evite les colonnes optionnelles qui peuvent ne pas exister
        String sql = "SELECT p.id_patient, p.nom, p.prenom, p.email, p.date_naissance "
                   + "FROM patients p "
                   + "JOIN admissions a ON a.id_patient = p.id_patient "
                   + "WHERE a.id_admission = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAdmission);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> p = new HashMap<>();
                    p.put("id",            rs.getInt("id_patient"));
                    p.put("nom",           rs.getString("nom"));
                    p.put("prenom",        rs.getString("prenom"));
                    p.put("email",         rs.getString("email"));
                    // date_naissance optionnelle
                    try { p.put("dateNaissance", rs.getDate("date_naissance")); }
                    catch (SQLException ignored) {}
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmailOrchestrator] SQL patient : " + e.getMessage());
        }
        return null;
    }

    private Map<String, Object> getAdmissionDetails(Connection cnx, int idAdmission) {
        // Requete flexible — ignore les colonnes absentes
        String sql = "SELECT * FROM admissions WHERE id_admission = ?";
        Map<String, Object> admission = new HashMap<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idAdmission);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    // Colonnes connues — on essaie chacune
                    tryGet(rs, admission, "constances_pouls",       "constancesPouls");
                    tryGet(rs, admission, "constances_temperature",  "constancesTemperature");
                    tryGet(rs, admission, "spo2",                    "spo2");
                    tryGet(rs, admission, "glycemie",                "glycemie");
                    tryGet(rs, admission, "score_douleur",           "scoreDouleur");
                    tryGet(rs, admission, "frequence_respiratoire",  "frequenceRespiratoire");
                    tryGet(rs, admission, "gcs_score",               "gcsScore");
                    tryGet(rs, admission, "motif_consultation",      "motifConsultation");
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmailOrchestrator] SQL admission : " + e.getMessage());
        }
        return admission;
    }

    private void tryGet(ResultSet rs, Map<String, Object> map, String col, String key) {
        try { map.put(key, rs.getObject(col)); } catch (SQLException ignored) {}
    }

    private List<Ordonnance> getOrdonnances(Connection cnx, int idConsultation) {
        List<Ordonnance> list = new ArrayList<>();
        String sql = "SELECT * FROM ordonnances WHERE id_consultation = ? ORDER BY id_ordo";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idConsultation);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ordonnance o = new Ordonnance();
                    o.setIdOrdo(rs.getInt("id_ordo"));
                    o.setIdConsultation(rs.getInt("id_consultation"));
                    o.setMedicament(rs.getString("medicament"));
                    o.setPosologie(rs.getString("posologie"));
                    o.setDureeJours(rs.getInt("duree_jours"));
                    list.add(o);
                }
            }
        } catch (SQLException e) {
            System.err.println("[EmailOrchestrator] SQL ordonnances : " + e.getMessage());
        }
        return list;
    }
}

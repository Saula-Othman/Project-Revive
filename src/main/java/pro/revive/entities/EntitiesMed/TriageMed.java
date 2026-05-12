package pro.revive.entities.EntitiesMed;

import java.time.LocalDateTime;

/**
 * Entité représentant un triage médical dans REVIVE — Module 3.
 * Contient la priorité calculée et les métadonnées du patient à l'arrivée.
 */
public class TriageMed {

    public enum Priorite {
        P1, P2, P3, P4;

        public String getLibelle() {
            return switch (this) {
                case P1 -> "Critique";
                case P2 -> "Urgent";
                case P3 -> "Semi-urgent";
                case P4 -> "Non urgent";
            };
        }
    }

    private int           idTriage;
    private int           idConsultation;
    private String        nomPatient;
    private int           age;
    private String        symptomes;
    private String        modeArrivee;
    private Priorite      priorite;
    private LocalDateTime horodatage;
    private String        personnelTriage;

    // ── Constructeurs ────────────────────────────────────────────────

    public TriageMed() {
        this.horodatage = LocalDateTime.now();
    }

    public TriageMed(String nomPatient, int age, String symptomes,
                  String modeArrivee, Priorite priorite) {
        this.nomPatient  = nomPatient;
        this.age         = age;
        this.symptomes   = symptomes;
        this.modeArrivee = modeArrivee;
        this.priorite    = priorite;
        this.horodatage  = LocalDateTime.now();
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public int           getIdTriage()                           { return idTriage; }
    public void          setIdTriage(int idTriage)               { this.idTriage = idTriage; }

    public int           getIdConsultation()                     { return idConsultation; }
    public void          setIdConsultation(int idConsultation)   { this.idConsultation = idConsultation; }

    public String        getNomPatient()                         { return nomPatient; }
    public void          setNomPatient(String nomPatient)        { this.nomPatient = nomPatient; }

    public int           getAge()                                { return age; }
    public void          setAge(int age)                         { this.age = age; }

    public String        getSymptomes()                          { return symptomes; }
    public void          setSymptomes(String symptomes)          { this.symptomes = symptomes; }

    public String        getModeArrivee()                        { return modeArrivee; }
    public void          setModeArrivee(String modeArrivee)      { this.modeArrivee = modeArrivee; }

    public Priorite      getPriorite()                           { return priorite; }
    public void          setPriorite(Priorite priorite)          { this.priorite = priorite; }

    public LocalDateTime getHorodatage()                         { return horodatage; }
    public void          setHorodatage(LocalDateTime horodatage) { this.horodatage = horodatage; }

    public String        getPersonnelTriage()                    { return personnelTriage; }
    public void          setPersonnelTriage(String p)            { this.personnelTriage = p; }

    // ── Utilitaires ──────────────────────────────────────────────────

    /**
     * Retourne la classe CSS du badge correspondant à la priorité.
     */
    public String getBadgeCssClass() {
        if (priorite == null) return "badge-triage-unknown";
        return switch (priorite) {
            case P1 -> "badge-triage-p1";
            case P2 -> "badge-triage-p2";
            case P3 -> "badge-triage-p3";
            case P4 -> "badge-triage-p4";
        };
    }

    // ── toString ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "TriageMed{" +
               "id=" + idTriage +
               ", patient='" + nomPatient + '\'' +
               ", age=" + age +
               ", priorite=" + priorite +
               ", horodatage=" + horodatage +
               '}';
    }
}

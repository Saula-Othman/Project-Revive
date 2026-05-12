package pro.revive.entities.EntitiesMed;

public class Ordonnance {
    private int    idOrdo;
    private int    idConsultation;
    private String medicament;
    private String posologie;
    private int    dureeJours;

    public Ordonnance() {}

    public Ordonnance(int idConsultation, String medicament, String posologie, int dureeJours) {
        this.idConsultation = idConsultation;
        this.medicament     = medicament;
        this.posologie      = posologie;
        this.dureeJours     = dureeJours;
    }

    public int    getIdOrdo()                   { return idOrdo; }
    public void   setIdOrdo(int v)              { this.idOrdo = v; }
    public int    getIdConsultation()           { return idConsultation; }
    public void   setIdConsultation(int v)      { this.idConsultation = v; }
    public String getMedicament()               { return medicament; }
    public void   setMedicament(String v)       { this.medicament = v; }
    public String getPosologie()                { return posologie; }
    public void   setPosologie(String v)        { this.posologie = v; }
    public int    getDureeJours()               { return dureeJours; }
    public void   setDureeJours(int v)          { this.dureeJours = v; }

    @Override
    public String toString() {
        return medicament + " — " + posologie + " (" + dureeJours + "j)"; }
}

package pro.revive.entities.EntitiesMateriel;

public class Salle {

    private int    idSalle;
    private String nomSalle;
    private String typeSalle;       // "resuscitation" | "urgence" | "standard" | "attente"
    private int    capaciteMax;
    private int    nombreActuel;
    private String statut;          // "Disponible" | "Pleine" | "Nettoyage" | "Maintenance"
    private int    niveauGraviteCible;
    private int    priorite;
    private int    patientsEnAttente;

    // ── Constructeurs ────────────────────────────────────────────────

    public Salle() {}

    public Salle(String nomSalle, String typeSalle, int capaciteMax,
                 String statut, int niveauGraviteCible, int priorite) {
        this.nomSalle            = nomSalle;
        this.typeSalle           = typeSalle;
        this.capaciteMax         = capaciteMax;
        this.statut              = statut;
        this.niveauGraviteCible  = niveauGraviteCible;
        this.priorite            = priorite;
        this.nombreActuel        = 0;
        this.patientsEnAttente   = 0;
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public int    getIdSalle()                          { return idSalle; }
    public void   setIdSalle(int idSalle)               { this.idSalle = idSalle; }

    public String getNomSalle()                         { return nomSalle; }
    public void   setNomSalle(String nomSalle)          { this.nomSalle = nomSalle; }

    public String getTypeSalle()                        { return typeSalle; }
    public void   setTypeSalle(String typeSalle)        { this.typeSalle = typeSalle; }

    public int    getCapaciteMax()                      { return capaciteMax; }
    public void   setCapaciteMax(int capaciteMax)       { this.capaciteMax = capaciteMax; }

    public int    getNombreActuel()                     { return nombreActuel; }
    public void   setNombreActuel(int nombreActuel)     { this.nombreActuel = nombreActuel; }

    public String getStatut()                           { return statut; }
    public void   setStatut(String statut)              { this.statut = statut; }

    public int    getNiveauGraviteCible()               { return niveauGraviteCible; }
    public void   setNiveauGraviteCible(int n)          { this.niveauGraviteCible = n; }

    public int    getPriorite()                         { return priorite; }
    public void   setPriorite(int priorite)             { this.priorite = priorite; }

    public int    getPatientsEnAttente()                { return patientsEnAttente; }
    public void   setPatientsEnAttente(int n)           { this.patientsEnAttente = n; }

    // ── Utilitaire ───────────────────────────────────────────────────

    /** Retourne true si la salle peut accueillir un patient supplémentaire. */
    public boolean estDisponible() {
        return "Disponible".equals(statut) && nombreActuel < capaciteMax;
    }

    /** Retourne le taux d'occupation en pourcentage (0-100). */
    public int getTauxOccupation() {
        if (capaciteMax == 0) return 0;
        return (nombreActuel * 100) / capaciteMax;
    }

    // ── toString ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Salle{" +
               "id=" + idSalle +
               ", nom='" + nomSalle + '\'' +
               ", type='" + typeSalle + '\'' +
               ", capacite=" + nombreActuel + "/" + capaciteMax +
               ", statut='" + statut + '\'' +
               ", gravite=" + niveauGraviteCible +
               '}';
    }
}

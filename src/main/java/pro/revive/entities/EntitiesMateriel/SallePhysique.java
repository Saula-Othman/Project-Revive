package pro.revive.entities.EntitiesMateriel;

/**
 * Entité représentant une salle physique de l'hôpital.
 * Table : salles (Module 5 - Gestion des Salles et Matériel)
 */
public class SallePhysique {

    private int    idSalle;
    private String nom;
    private String type;    // Box | Bloc | Radio
    private String statut;  // Disponible | Occupée | Nettoyage
    private int    capaciteMax;
    private int    nombreActuel;
    private String localisation;

    // ── Constructeurs ────────────────────────────────────────────────

    public SallePhysique() {}

    /** Constructeur sans ID (pour création). */
    public SallePhysique(String nom, String type, String statut) {
        this.nom    = nom;
        this.type   = type;
        this.statut = statut;
        this.capaciteMax = 0;
        this.nombreActuel = 0;
        this.localisation = "Non spécifiée";
    }

    /** Constructeur complet (pour lecture depuis BDD). */
    public SallePhysique(int idSalle, String nom, String type, String statut, int capaciteMax, int nombreActuel, String localisation) {
        this.idSalle      = idSalle;
        this.nom          = nom;
        this.type         = type;
        this.statut       = statut;
        this.capaciteMax  = capaciteMax;
        this.nombreActuel = nombreActuel;
        this.localisation = localisation;
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public int    getIdSalle()              { return idSalle; }
    public void   setIdSalle(int idSalle)   { this.idSalle = idSalle; }

    public String getNom()                  { return nom; }
    public void   setNom(String nom)        { this.nom = nom; }

    public String getType()                 { return type; }
    public void   setType(String type)      { this.type = type; }

    public String getStatut()               { return statut; }
    public void   setStatut(String statut)  { this.statut = statut; }

    public int    getCapaciteMax()          { return capaciteMax; }
    public void   setCapaciteMax(int c)     { this.capaciteMax = c; }

    public int    getNombreActuel()         { return nombreActuel; }
    public void   setNombreActuel(int n)    { this.nombreActuel = n; }

    public String getLocalisation()         { return localisation; }
    public void   setLocalisation(String l) { this.localisation = l; }

    // ── toString ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return idSalle + " - " + nom;
    }
}

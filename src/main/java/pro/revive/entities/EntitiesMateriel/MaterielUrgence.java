package pro.revive.entities.EntitiesMateriel;

import java.time.LocalDate;

/**
 * Entité représentant un équipement médical d'urgence.
 * Table : materiel_urgence (Module 5 - Gestion des Salles et Matériel)
 */
public class MaterielUrgence {

    private int       idMateriel;
    private Integer   idSalle;                  // nullable → NULL = en réserve
    private String    nom;
    private LocalDate dateDerniereMaintenance;
    private String    etat;                     // Fonctionnel | À réviser
    private int       quantite;

    // Champ transient : nom de la salle (rempli par JOIN dans le service)
    private String nomSalle;

    // ── Constructeurs ────────────────────────────────────────────────

    public MaterielUrgence() {}

    /** Constructeur sans ID (pour création). */
    public MaterielUrgence(Integer idSalle, String nom,
                           LocalDate dateDerniereMaintenance,
                           String etat, int quantite) {
        this.idSalle                  = idSalle;
        this.nom                      = nom;
        this.dateDerniereMaintenance  = dateDerniereMaintenance;
        this.etat                     = etat;
        this.quantite                 = quantite;
    }

    /** Constructeur complet (pour lecture depuis BDD). */
    public MaterielUrgence(int idMateriel, Integer idSalle, String nom,
                           LocalDate dateDerniereMaintenance,
                           String etat, int quantite) {
        this.idMateriel               = idMateriel;
        this.idSalle                  = idSalle;
        this.nom                      = nom;
        this.dateDerniereMaintenance  = dateDerniereMaintenance;
        this.etat                     = etat;
        this.quantite                 = quantite;
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public int       getIdMateriel()                          { return idMateriel; }
    public void      setIdMateriel(int idMateriel)            { this.idMateriel = idMateriel; }

    public Integer   getIdSalle()                             { return idSalle; }
    public void      setIdSalle(Integer idSalle)              { this.idSalle = idSalle; }

    public String    getNom()                                 { return nom; }
    public void      setNom(String nom)                       { this.nom = nom; }

    public LocalDate getDateDerniereMaintenance()             { return dateDerniereMaintenance; }
    public void      setDateDerniereMaintenance(LocalDate d)  { this.dateDerniereMaintenance = d; }

    public String    getEtat()                                { return etat; }
    public void      setEtat(String etat)                     { this.etat = etat; }

    public int       getQuantite()                            { return quantite; }
    public void      setQuantite(int quantite)                { this.quantite = quantite; }

    public String    getNomSalle()                            { return nomSalle; }
    public void      setNomSalle(String nomSalle)             { this.nomSalle = nomSalle; }

    // ── toString ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "MaterielUrgence{id=" + idMateriel
                + ", nom='" + nom + '\''
                + ", salle=" + (idSalle == null ? "Réserve" : idSalle)
                + ", etat='" + etat + '\''
                + ", qte=" + quantite + '}';
    }
}

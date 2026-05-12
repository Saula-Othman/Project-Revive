package pro.revive.entities.EntitiesMateriel;

import java.time.LocalDate;

public class Materiel {

    private int       idMateriel;
    private int       idSalle;          // 0 = non assigné
    private String    nom;
    private LocalDate dateDerniereMaintenance;
    private String    etat;             // "Fonctionnel" | "A reviser"
    private int       quantite;

    // ── Champ transient (jointure) ───────────────────────────────────
    private String nomSalle;            // rempli lors d'un SELECT avec JOIN

    // ── Constructeurs ────────────────────────────────────────────────

    public Materiel() {}

    public Materiel(String nom, int idSalle, String etat, int quantite) {
        this.nom      = nom;
        this.idSalle  = idSalle;
        this.etat     = etat;
        this.quantite = quantite;
    }

    public Materiel(String nom, int idSalle,
                    LocalDate dateDerniereMaintenance, String etat, int quantite) {
        this.nom                      = nom;
        this.idSalle                  = idSalle;
        this.dateDerniereMaintenance  = dateDerniereMaintenance;
        this.etat                     = etat;
        this.quantite                 = quantite;
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public int       getIdMateriel()                         { return idMateriel; }
    public void      setIdMateriel(int idMateriel)           { this.idMateriel = idMateriel; }

    public int       getIdSalle()                            { return idSalle; }
    public void      setIdSalle(int idSalle)                 { this.idSalle = idSalle; }

    public String    getNom()                                { return nom; }
    public void      setNom(String nom)                      { this.nom = nom; }

    public LocalDate getDateDerniereMaintenance()            { return dateDerniereMaintenance; }
    public void      setDateDerniereMaintenance(LocalDate d) { this.dateDerniereMaintenance = d; }

    public String    getEtat()                               { return etat; }
    public void      setEtat(String etat)                    { this.etat = etat; }

    public int       getQuantite()                           { return quantite; }
    public void      setQuantite(int quantite)               { this.quantite = quantite; }

    public String    getNomSalle()                           { return nomSalle; }
    public void      setNomSalle(String nomSalle)            { this.nomSalle = nomSalle; }

    // ── Utilitaire ───────────────────────────────────────────────────

    /** Retourne true si le matériel nécessite une révision. */
    public boolean necessiteRevision() {
        return "A reviser".equals(etat);
    }

    // ── toString ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "Materiel{" +
               "id=" + idMateriel +
               ", nom='" + nom + '\'' +
               ", salle=" + idSalle +
               ", etat='" + etat + '\'' +
               ", quantite=" + quantite +
               ", maintenance=" + dateDerniereMaintenance +
               '}';
    }
}

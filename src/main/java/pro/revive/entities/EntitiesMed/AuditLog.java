package pro.revive.entities.EntitiesMed;

import java.time.LocalDateTime;

/**
 * Représente une entrée dans le journal d'audit de l'application REVIVE.
 * Enregistre chaque action effectuée sur une entité métier.
 */
public class AuditLog {

    public enum Action {
        CREATE, UPDATE, DELETE, CLOTURER, LOGIN
    }

    private String        utilisateur;
    private Action        action;
    private String        entite;
    private int           idEntite;
    private String        ancienneValeur;
    private String        nouvelleValeur;
    private LocalDateTime horodatage;

    /** Constructeur par défaut — initialise l'horodatage à maintenant. */
    public AuditLog() {
        this.horodatage = LocalDateTime.now();
    }

    /** Constructeur complet. */
    public AuditLog(String utilisateur, Action action, String entite,
                    int idEntite, String ancienneValeur, String nouvelleValeur) {
        this.utilisateur    = utilisateur;
        this.action         = action;
        this.entite         = entite;
        this.idEntite       = idEntite;
        this.ancienneValeur = ancienneValeur;
        this.nouvelleValeur = nouvelleValeur;
        this.horodatage     = LocalDateTime.now();
    }

    // ── Getters / Setters ────────────────────────────────────────────

    public String        getUtilisateur()                        { return utilisateur; }
    public void          setUtilisateur(String utilisateur)      { this.utilisateur = utilisateur; }

    public Action        getAction()                             { return action; }
    public void          setAction(Action action)                { this.action = action; }

    public String        getEntite()                             { return entite; }
    public void          setEntite(String entite)                { this.entite = entite; }

    public int           getIdEntite()                           { return idEntite; }
    public void          setIdEntite(int idEntite)               { this.idEntite = idEntite; }

    public String        getAncienneValeur()                     { return ancienneValeur; }
    public void          setAncienneValeur(String ancienneValeur){ this.ancienneValeur = ancienneValeur; }

    public String        getNouvelleValeur()                     { return nouvelleValeur; }
    public void          setNouvelleValeur(String nouvelleValeur){ this.nouvelleValeur = nouvelleValeur; }

    public LocalDateTime getHorodatage()                         { return horodatage; }
    public void          setHorodatage(LocalDateTime horodatage) { this.horodatage = horodatage; }

    // ── toString ─────────────────────────────────────────────────────

    @Override
    public String toString() {
        return "AuditLog{" +
               "utilisateur='" + utilisateur + '\'' +
               ", action=" + action +
               ", entite='" + entite + '\'' +
               ", idEntite=" + idEntite +
               ", horodatage=" + horodatage +
               '}';
    }
}

package pro.revive.entities.EntitiesLabo;

import java.util.Date;

public class Resultats {

    private int idResultat;
    private int idDemande;
    private String compteRendu;
    private String fichierJoint;
    private Date dateResultat;

    public Resultats() {
    }

    public Resultats(int idDemande, String compteRendu, String fichierJoint) {
        this.idDemande = idDemande;
        this.compteRendu = compteRendu;
        this.fichierJoint = fichierJoint;
    }

    public Resultats(int idResultat, int idDemande, String compteRendu, String fichierJoint, Date dateResultat) {
        this.idResultat = idResultat;
        this.idDemande = idDemande;
        this.compteRendu = compteRendu;
        this.fichierJoint = fichierJoint;
        this.dateResultat = dateResultat;
    }

    public int getIdResultat() {
        return idResultat;
    }

    public void setIdResultat(int idResultat) {
        this.idResultat = idResultat;
    }

    public int getIdDemande() {
        return idDemande;
    }

    public void setIdDemande(int idDemande) {
        this.idDemande = idDemande;
    }

    public String getCompteRendu() {
        return compteRendu;
    }

    public void setCompteRendu(String compteRendu) {
        this.compteRendu = compteRendu;
    }

    public String getFichierJoint() {
        return fichierJoint;
    }

    public void setFichierJoint(String fichierJoint) {
        this.fichierJoint = fichierJoint;
    }

    public Date getDateResultat() {
        return dateResultat;
    }

    public void setDateResultat(Date dateResultat) {
        this.dateResultat = dateResultat;
    }

    @Override
    public String toString() {
        return "Resultats{" +
                "idResultat=" + idResultat +
                ", idDemande=" + idDemande +
                ", compteRendu='" + compteRendu + '\'' +
                ", fichierJoint='" + fichierJoint + '\'' +
                ", dateResultat=" + dateResultat +
                '}';
    }

    private String etat;
    // Champ transient : nom+prénom du patient (via JOIN, non stocké en BD)
    private String nomPatient;

    // ── Score de gravité intelligent
    private int    scoreGravite;    // 0-100
    private String niveauGravite;   // Faible / Moyen / Élevé / Critique
    private String recommandation;  // texte de recommandation automatique

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public String getNomPatient() { return nomPatient; }
    public void setNomPatient(String nomPatient) { this.nomPatient = nomPatient; }

    public int getScoreGravite() { return scoreGravite; }
    public void setScoreGravite(int scoreGravite) { this.scoreGravite = scoreGravite; }

    public String getNiveauGravite() { return niveauGravite; }
    public void setNiveauGravite(String niveauGravite) { this.niveauGravite = niveauGravite; }

    public String getRecommandation() { return recommandation; }
    public void setRecommandation(String recommandation) { this.recommandation = recommandation; }
}
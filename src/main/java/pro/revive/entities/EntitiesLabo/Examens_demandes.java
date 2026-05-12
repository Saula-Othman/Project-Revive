package pro.revive.entities.EntitiesLabo;

import java.util.Date;

public class Examens_demandes {

    private int idDemande;
    private int idConsultation;
    private String typeExamen;
    private Date dateDemande;
    private String statut;
    private boolean urgent;
    // Champ transient : nom+prénom du patient (via JOIN, non stocké en BD)
    private String nomPatient;

    public Examens_demandes() {
    }

    public Examens_demandes(int idConsultation, String typeExamen, String statut, boolean urgent) {
        this.idConsultation = idConsultation;
        this.typeExamen = typeExamen;
        this.statut = statut;
        this.urgent = urgent;
    }

    public Examens_demandes(int idDemande, int idConsultation, String typeExamen, Date dateDemande, String statut, boolean urgent) {
        this.idDemande = idDemande;
        this.idConsultation = idConsultation;
        this.typeExamen = typeExamen;
        this.dateDemande = dateDemande;
        this.statut = statut;
        this.urgent = urgent;
    }

    public int getIdDemande() {
        return idDemande;
    }

    public void setIdDemande(int idDemande) {
        this.idDemande = idDemande;
    }

    public int getIdConsultation() {
        return idConsultation;
    }

    public void setIdConsultation(int idConsultation) {
        this.idConsultation = idConsultation;
    }

    public String getTypeExamen() {
        return typeExamen;
    }

    public void setTypeExamen(String typeExamen) {
        this.typeExamen = typeExamen;
    }

    public Date getDateDemande() {
        return dateDemande;
    }

    public void setDateDemande(Date dateDemande) {
        this.dateDemande = dateDemande;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public String getNomPatient() {
        return nomPatient;
    }

    public void setNomPatient(String nomPatient) {
        this.nomPatient = nomPatient;
    }

    @Override
    public String toString() {
        return "DemandeExamen{" +
                "idDemande=" + idDemande +
                ", idConsultation=" + idConsultation +
                ", typeExamen='" + typeExamen + '\'' +
                ", dateDemande=" + dateDemande +
                ", statut='" + statut + '\'' +
                ", urgent=" + urgent +
                '}';
    }
}
package pro.revive.entities.EntitiesLabo;

import java.util.Date;

/**
 * Modèle représentant une notification de demande d'examen
 * envoyée par un médecin urgentiste (statut_demande = 'Envoyée').
 */
public class ConsultationNotif {

    private int    idConsultation;
    private String nomPatient;      // nom + prénom depuis la table patients
    private String analyses;        // champ TEXT de la table consultations
    private String imageries;       // champ TEXT de la table consultations
    private Date   dateDemande;     // date de la consultation

    public ConsultationNotif() {}

    public ConsultationNotif(int idConsultation, String nomPatient,
                             String analyses, String imageries, Date dateDemande) {
        this.idConsultation = idConsultation;
        this.nomPatient     = nomPatient;
        this.analyses       = analyses;
        this.imageries      = imageries;
        this.dateDemande    = dateDemande;
    }

    public int    getIdConsultation()              { return idConsultation; }
    public void   setIdConsultation(int id)        { this.idConsultation = id; }

    public String getNomPatient()                  { return nomPatient; }
    public void   setNomPatient(String nomPatient) { this.nomPatient = nomPatient; }

    public String getAnalyses()                    { return analyses; }
    public void   setAnalyses(String analyses)     { this.analyses = analyses; }

    public String getImageries()                   { return imageries; }
    public void   setImageries(String imageries)   { this.imageries = imageries; }

    public Date   getDateDemande()                 { return dateDemande; }
    public void   setDateDemande(Date dateDemande) { this.dateDemande = dateDemande; }
}

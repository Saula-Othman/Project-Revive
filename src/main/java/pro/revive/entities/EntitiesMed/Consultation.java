package pro.revive.entities.EntitiesMed;

import java.time.LocalDateTime;

public class Consultation {
    private int           idConsultation;
    private int           idAdmission;
    private int           idPersonnelMedecin;
    private LocalDateTime dateHeureDebut;
    private LocalDateTime dateHeureFin;
    private String        diagnostic;
    private String        orientation;
    private String        nomMedecin;
    private String        nomPatient;
    // Nouveaux champs Module 4 — Laboratoire & Imagerie
    private String        analyses;
    private String        imageries;
    private String        statutDemande;
    private String        icdCode;   // Feature 1 — code CIM-10 selectionne

    public Consultation() {}

    public Consultation(int idAdmission, int idPersonnelMedecin, String diagnostic, String orientation) {
        this.idAdmission        = idAdmission;
        this.idPersonnelMedecin = idPersonnelMedecin;
        this.dateHeureDebut     = LocalDateTime.now();
        this.diagnostic         = diagnostic;
        this.orientation        = orientation;
        this.statutDemande      = "Non envoyee";
    }

    public int    getIdConsultation()                    { return idConsultation; }
    public void   setIdConsultation(int v)               { this.idConsultation = v; }
    public int    getIdAdmission()                       { return idAdmission; }
    public void   setIdAdmission(int v)                  { this.idAdmission = v; }
    public int    getIdPersonnelMedecin()                { return idPersonnelMedecin; }
    public void   setIdPersonnelMedecin(int v)           { this.idPersonnelMedecin = v; }
    public LocalDateTime getDateHeureDebut()             { return dateHeureDebut; }
    public void   setDateHeureDebut(LocalDateTime v)     { this.dateHeureDebut = v; }
    public LocalDateTime getDateHeureFin()               { return dateHeureFin; }
    public void   setDateHeureFin(LocalDateTime v)       { this.dateHeureFin = v; }
    public String getDiagnostic()                        { return diagnostic; }
    public void   setDiagnostic(String v)                { this.diagnostic = v; }
    public String getOrientation()                       { return orientation; }
    public void   setOrientation(String v)               { this.orientation = v; }
    public String getNomMedecin()                        { return nomMedecin; }
    public void   setNomMedecin(String v)                { this.nomMedecin = v; }
    public String getNomPatient()                        { return nomPatient; }
    public void   setNomPatient(String v)                { this.nomPatient = v; }

    // Getters/Setters — Laboratoire & Imagerie
    public String getAnalyses()                          { return analyses; }
    public void   setAnalyses(String v)                  { this.analyses = v; }
    public String getImageries()                         { return imageries; }
    public void   setImageries(String v)                 { this.imageries = v; }
    public String getStatutDemande()                     { return statutDemande; }
    public void   setStatutDemande(String v)             { this.statutDemande = v; }

    /**
     * Calcule automatiquement le statut de la demande.
     * "Envoyee" si analyses OU imageries non vide, sinon "Non envoyee".
     */
    public void calculerStatutDemande() {
        boolean aAnalyses  = analyses  != null && !analyses.trim().isEmpty();
        boolean aImageries = imageries != null && !imageries.trim().isEmpty();
        this.statutDemande = (aAnalyses || aImageries) ? "Envoyee" : "Non envoyee";
    }

    public boolean isDemandEnvoyee() {
        return "Envoyee".equals(statutDemande);
    }

    public String getIcdCode()               { return icdCode; }
    public void   setIcdCode(String v)       { this.icdCode = v; }

    @Override
    public String toString() {
        return "Consultation{id=" + idConsultation + ", admission=" + idAdmission
             + ", diagnostic='" + diagnostic + "', orientation='" + orientation + "'}";
    }
}

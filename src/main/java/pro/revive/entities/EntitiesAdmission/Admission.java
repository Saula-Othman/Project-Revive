package pro.revive.entities.EntitiesAdmission;

import java.time.LocalDateTime;

public class Admission {
    private int id;
    private int patientId;
    private Patient patient;
    private LocalDateTime dateAdmission;
    private String modeArrivee;
    private String motifAdmission;
    private String statut;
    private String prioriteInitiale;
    private int agentAccueilId;
    private String notes;
    private Integer ambulanceId;
    private boolean patientInconnu;
    private boolean actif;

    public Admission() {
        this.statut = "En attente triage";
        this.prioriteInitiale = "Non évalué";
        this.dateAdmission = LocalDateTime.now();
        this.patientInconnu = false;
        this.actif = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }

    public LocalDateTime getDateAdmission() { return dateAdmission; }
    public void setDateAdmission(LocalDateTime dateAdmission) { this.dateAdmission = dateAdmission; }

    public String getModeArrivee() { return modeArrivee; }
    public void setModeArrivee(String modeArrivee) { this.modeArrivee = modeArrivee; }

    public String getMotifAdmission() { return motifAdmission; }
    public void setMotifAdmission(String motifAdmission) { this.motifAdmission = motifAdmission; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getPrioriteInitiale() { return prioriteInitiale; }
    public void setPrioriteInitiale(String prioriteInitiale) { this.prioriteInitiale = prioriteInitiale; }

    public int getAgentAccueilId() { return agentAccueilId; }
    public void setAgentAccueilId(int agentAccueilId) { this.agentAccueilId = agentAccueilId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getAmbulanceId() { return ambulanceId; }
    public void setAmbulanceId(Integer ambulanceId) { this.ambulanceId = ambulanceId; }

    public boolean isPatientInconnu() { return patientInconnu; }
    public void setPatientInconnu(boolean patientInconnu) { this.patientInconnu = patientInconnu; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
}

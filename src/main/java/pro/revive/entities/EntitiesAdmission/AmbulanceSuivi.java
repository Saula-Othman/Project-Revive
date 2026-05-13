package pro.revive.entities.EntitiesAdmission;

import java.time.LocalDateTime;

public class AmbulanceSuivi {
    private int id;
    private String matricule;
    private double latitude;
    private double longitude;
    private int etaMinutes;
    private String niveauUrgence;
    private String patientInfoProvisoire;
    private String statut;
    private LocalDateTime dateMiseAJour;
    private LocalDateTime dateDepart;
    private LocalDateTime dateArriveePrevue;
    private Integer personnelId;
    private Integer admissionId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getEtaMinutes() { return etaMinutes; }
    public void setEtaMinutes(int etaMinutes) { this.etaMinutes = etaMinutes; }

    public String getNiveauUrgence() { return niveauUrgence; }
    public void setNiveauUrgence(String niveauUrgence) { this.niveauUrgence = niveauUrgence; }

    public String getPatientInfoProvisoire() { return patientInfoProvisoire; }
    public void setPatientInfoProvisoire(String patientInfoProvisoire) { this.patientInfoProvisoire = patientInfoProvisoire; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateMiseAJour() { return dateMiseAJour; }
    public void setDateMiseAJour(LocalDateTime dateMiseAJour) { this.dateMiseAJour = dateMiseAJour; }

    public LocalDateTime getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDateTime dateDepart) { this.dateDepart = dateDepart; }

    public LocalDateTime getDateArriveePrevue() { return dateArriveePrevue; }
    public void setDateArriveePrevue(LocalDateTime dateArriveePrevue) { this.dateArriveePrevue = dateArriveePrevue; }

    public Integer getPersonnelId() { return personnelId; }
    public void setPersonnelId(Integer personnelId) { this.personnelId = personnelId; }

    public Integer getAdmissionId() { return admissionId; }
    public void setAdmissionId(Integer admissionId) { this.admissionId = admissionId; }
}

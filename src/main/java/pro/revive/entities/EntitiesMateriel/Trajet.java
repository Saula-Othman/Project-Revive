package pro.revive.entities.EntitiesMateriel;

import java.time.LocalDateTime;

public class Trajet {

    private Integer idTrajet;
    private Integer idAmbulance;
    private String localisationDepart;
    private String localisationUrgence;
    private Double distanceKm;
    private Integer dureeMinutes;
    private LocalDateTime dateTrajet;
    private String statut; // Terminé, En cours, Annulé

    // Constructeurs
    public Trajet() {}

    public Trajet(Integer idAmbulance, String localisationDepart, String localisationUrgence,
                  Double distanceKm, Integer dureeMinutes) {
        this.idAmbulance = idAmbulance;
        this.localisationDepart = localisationDepart;
        this.localisationUrgence = localisationUrgence;
        this.distanceKm = distanceKm;
        this.dureeMinutes = dureeMinutes;
        this.statut = "Terminé";
    }

    // Getters & Setters
    public Integer getIdTrajet() { return idTrajet; }
    public void setIdTrajet(Integer idTrajet) { this.idTrajet = idTrajet; }

    public Integer getIdAmbulance() { return idAmbulance; }
    public void setIdAmbulance(Integer idAmbulance) { this.idAmbulance = idAmbulance; }

    public String getLocalisationDepart() { return localisationDepart; }
    public void setLocalisationDepart(String localisationDepart) { this.localisationDepart = localisationDepart; }

    public String getLocalisationUrgence() { return localisationUrgence; }
    public void setLocalisationUrgence(String localisationUrgence) { this.localisationUrgence = localisationUrgence; }

    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }

    public Integer getDureeMinutes() { return dureeMinutes; }
    public void setDureeMinutes(Integer dureeMinutes) { this.dureeMinutes = dureeMinutes; }

    public LocalDateTime getDateTrajet() { return dateTrajet; }
    public void setDateTrajet(LocalDateTime dateTrajet) { this.dateTrajet = dateTrajet; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    @Override
    public String toString() {
        return localisationDepart + " → " + localisationUrgence + " (" + distanceKm + " km)";
    }
}

package pro.revive.entities.EntitiesMateriel;

import java.time.LocalDateTime;

public class AlerteMaintenance {

    private Integer idAlerte;
    private Integer idAmbulance;
    private String typeMaintenance; // Vidange, Pneus, Freins, Révision complète, Autre
    private String priorite; // Faible, Moyenne, Élevée, Critique
    private String description;
    private Double kmActuel;
    private Double kmRecommande;
    private LocalDateTime dateGeneration;
    private String statut; // En attente, Planifiée, Effectuée, Ignorée
    private LocalDateTime dateResolution;

    // Constructeurs
    public AlerteMaintenance() {}

    public AlerteMaintenance(Integer idAmbulance, String typeMaintenance, String priorite,
                             String description, Double kmActuel, Double kmRecommande) {
        this.idAmbulance = idAmbulance;
        this.typeMaintenance = typeMaintenance;
        this.priorite = priorite;
        this.description = description;
        this.kmActuel = kmActuel;
        this.kmRecommande = kmRecommande;
        this.statut = "En attente";
    }

    // Getters & Setters
    public Integer getIdAlerte() { return idAlerte; }
    public void setIdAlerte(Integer idAlerte) { this.idAlerte = idAlerte; }

    public Integer getIdAmbulance() { return idAmbulance; }
    public void setIdAmbulance(Integer idAmbulance) { this.idAmbulance = idAmbulance; }

    public String getTypeMaintenance() { return typeMaintenance; }
    public void setTypeMaintenance(String typeMaintenance) { this.typeMaintenance = typeMaintenance; }

    public String getPriorite() { return priorite; }
    public void setPriorite(String priorite) { this.priorite = priorite; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getKmActuel() { return kmActuel; }
    public void setKmActuel(Double kmActuel) { this.kmActuel = kmActuel; }

    public Double getKmRecommande() { return kmRecommande; }
    public void setKmRecommande(Double kmRecommande) { this.kmRecommande = kmRecommande; }

    public LocalDateTime getDateGeneration() { return dateGeneration; }
    public void setDateGeneration(LocalDateTime dateGeneration) { this.dateGeneration = dateGeneration; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateResolution() { return dateResolution; }
    public void setDateResolution(LocalDateTime dateResolution) { this.dateResolution = dateResolution; }

    @Override
    public String toString() {
        return typeMaintenance + " - " + priorite + " (" + statut + ")";
    }
}

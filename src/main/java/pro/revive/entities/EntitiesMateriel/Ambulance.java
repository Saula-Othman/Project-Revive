package pro.revive.entities.EntitiesMateriel;

import java.time.LocalDate;

public class Ambulance {

    private Integer idAmbulance;
    private String numeroSerie;
    private String marque;
    private String modele;
    private Integer anneeFabrication;
    private String etat; // Disponible, En route, En panne
    private Double kmTotal;
    private LocalDate dateDerniereVidange;
    private Double kmDerniereVidange;
    private LocalDate dateDerniersPneus;
    private Double kmDerniersPneus;

    // Constructeurs
    public Ambulance() {}

    public Ambulance(Integer idAmbulance, String numeroSerie, String marque, String modele, 
                     Integer anneeFabrication, String etat, Double kmTotal) {
        this.idAmbulance = idAmbulance;
        this.numeroSerie = numeroSerie;
        this.marque = marque;
        this.modele = modele;
        this.anneeFabrication = anneeFabrication;
        this.etat = etat;
        this.kmTotal = kmTotal;
    }

    // Getters & Setters
    public Integer getIdAmbulance() { return idAmbulance; }
    public void setIdAmbulance(Integer idAmbulance) { this.idAmbulance = idAmbulance; }

    public String getNumeroSerie() { return numeroSerie; }
    public void setNumeroSerie(String numeroSerie) { this.numeroSerie = numeroSerie; }

    public String getMarque() { return marque; }
    public void setMarque(String marque) { this.marque = marque; }

    public String getModele() { return modele; }
    public void setModele(String modele) { this.modele = modele; }

    public Integer getAnneeFabrication() { return anneeFabrication; }
    public void setAnneeFabrication(Integer anneeFabrication) { this.anneeFabrication = anneeFabrication; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public Double getKmTotal() { return kmTotal; }
    public void setKmTotal(Double kmTotal) { this.kmTotal = kmTotal; }

    public LocalDate getDateDerniereVidange() { return dateDerniereVidange; }
    public void setDateDerniereVidange(LocalDate dateDerniereVidange) { this.dateDerniereVidange = dateDerniereVidange; }

    public Double getKmDerniereVidange() { return kmDerniereVidange; }
    public void setKmDerniereVidange(Double kmDerniereVidange) { this.kmDerniereVidange = kmDerniereVidange; }

    public LocalDate getDateDerniersPneus() { return dateDerniersPneus; }
    public void setDateDerniersPneus(LocalDate dateDerniersPneus) { this.dateDerniersPneus = dateDerniersPneus; }

    public Double getKmDerniersPneus() { return kmDerniersPneus; }
    public void setKmDerniersPneus(Double kmDerniersPneus) { this.kmDerniersPneus = kmDerniersPneus; }

    @Override
    public String toString() {
        return numeroSerie + " - " + marque + " " + modele + " (" + etat + ")";
    }
}

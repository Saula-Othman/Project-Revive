package pro.revive.entities;

public class Salle {

    private int idSalle;
    private String nomSalle;
    private String typeSalle;
    private int capaciteMax;
    private int nombreActuel;
    private String statut;
    private int niveauGraviteCible;
    private int priorite;
    private int patientsEnAttente;

    public Salle() {
    }

    public Salle(String nomSalle, String typeSalle, int capaciteMax, int niveauGraviteCible, int priorite) {
        this.nomSalle = nomSalle;
        this.typeSalle = typeSalle;
        this.capaciteMax = capaciteMax;
        this.niveauGraviteCible = niveauGraviteCible;
        this.priorite = priorite;
        this.statut = "Disponible";
        this.nombreActuel = 0;
        this.patientsEnAttente = 0;
    }

    public boolean isAvailable() {
        // BUG-4 fix: null-safe — flip operands so a null statut doesn't NPE
        return nombreActuel < capaciteMax && "Disponible".equals(statut);
    }

    public boolean isFull() {
        return nombreActuel >= capaciteMax;
    }

    public int getIdSalle() { return idSalle; }
    public void setIdSalle(int idSalle) { this.idSalle = idSalle; }
    public String getNomSalle() { return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }
    public String getTypeSalle() { return typeSalle; }
    public void setTypeSalle(String typeSalle) { this.typeSalle = typeSalle; }
    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }
    public int getNombreActuel() { return nombreActuel; }
    public void setNombreActuel(int nombreActuel) { this.nombreActuel = nombreActuel; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public int getNiveauGraviteCible() { return niveauGraviteCible; }
    public void setNiveauGraviteCible(int niveauGraviteCible) { this.niveauGraviteCible = niveauGraviteCible; }
    public int getPriorite() { return priorite; }
    public void setPriorite(int priorite) { this.priorite = priorite; }
    public int getPatientsEnAttente() { return patientsEnAttente; }
    public void setPatientsEnAttente(int patientsEnAttente) { this.patientsEnAttente = patientsEnAttente; }

    @Override
    public String toString() {
        return "Salle{" +
                "idSalle=" + idSalle +
                ", nomSalle='" + nomSalle + '\'' +
                ", " + nombreActuel + "/" + capaciteMax +
                ", statut='" + statut + '\'' +
                '}';
    }
}

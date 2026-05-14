package pro.revive.entities.EntitiesAdmission;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Patient {
    private int id;
    private String nom;
    private String prenom;
    private LocalDate dateNaissance;
    private String sexe;
    private String groupeSanguin;
    private String numSecuriteSociale;
    private String telephone;
    private String email;
    private String adresse;
    private String allergies;
    private String antecedents;
    private String nationalite;
    private String numCin;
    private String contactUrgenceNom;
    private String contactUrgenceTel;
    private LocalDateTime dateCreation;
    private boolean actif;

    public Patient() {
        this.nationalite = "Tunisienne";
        this.actif = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public String getGroupeSanguin() { return groupeSanguin; }
    public void setGroupeSanguin(String groupeSanguin) { this.groupeSanguin = groupeSanguin; }

    public String getNumSecuriteSociale() { return numSecuriteSociale; }
    public void setNumSecuriteSociale(String numSecuriteSociale) { this.numSecuriteSociale = numSecuriteSociale; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }

    public String getAntecedents() { return antecedents; }
    public void setAntecedents(String antecedents) { this.antecedents = antecedents; }

    public String getNationalite() { return nationalite; }
    public void setNationalite(String nationalite) { this.nationalite = nationalite; }

    public String getNumCin() { return numCin; }
    public void setNumCin(String numCin) { this.numCin = numCin; }

    public String getContactUrgenceNom() { return contactUrgenceNom; }
    public void setContactUrgenceNom(String contactUrgenceNom) { this.contactUrgenceNom = contactUrgenceNom; }

    public String getContactUrgenceTel() { return contactUrgenceTel; }
    public void setContactUrgenceTel(String contactUrgenceTel) { this.contactUrgenceTel = contactUrgenceTel; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public String getNomComplet() {
        return nom + " " + prenom;
    }

    @Override
    public String toString() {
        return getNomComplet();
    }
}

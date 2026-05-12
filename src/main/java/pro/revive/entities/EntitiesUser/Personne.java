package pro.revive.entities.EntitiesUser;

import java.time.LocalDate;
import java.time.Period;

public class Personne {

    private int idPersonnel;
    private String nom;
    private String prenom;
    private String role;
    private String identifiant;
    private String motDePasse;
    private LocalDate dateNaissance;
    private String telephone;
    private String email;
    private String statut = "ACTIF";
    private boolean premierConnexion = false;

    public Personne() {}

    public Personne(String nom, String prenom, String role, String identifiant, String motDePasse) {
        this.nom = nom;
        this.prenom = prenom;
        this.role = role;
        this.identifiant = identifiant;
        this.motDePasse = motDePasse;
    }

    public int getIdPersonnel() { return idPersonnel; }
    public void setIdPersonnel(int idPersonnel) { this.idPersonnel = idPersonnel; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getIdentifiant() { return identifiant; }
    public void setIdentifiant(String identifiant) { this.identifiant = identifiant; }

    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public boolean isPremierConnexion() { return premierConnexion; }
    public void setPremierConnexion(boolean premierConnexion) { this.premierConnexion = premierConnexion; }

    /** Calcule l'âge en années à partir de la date de naissance. */
    public int getAge() {
        if (dateNaissance == null) return 0;
        return Period.between(dateNaissance, LocalDate.now()).getYears();
    }

    @Override
    public String toString() {
        return "Personne{id=" + idPersonnel + ", nom='" + nom + "', prenom='" + prenom +
               "', role='" + role + "', identifiant='" + identifiant + "'}";
    }
}

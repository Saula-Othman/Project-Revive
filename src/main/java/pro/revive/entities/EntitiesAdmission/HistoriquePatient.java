package pro.revive.entities.EntitiesAdmission;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HistoriquePatient {
    private int id;
    private int patientId;
    private Integer admissionId;
    private LocalDate dateConsultation;
    private String typeDocument;
    private String titre;
    private String contenu;
    private String medecinNom;
    private String etablissement;
    private String source;
    private LocalDateTime dateImport;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPatientId() { return patientId; }
    public void setPatientId(int patientId) { this.patientId = patientId; }

    public Integer getAdmissionId() { return admissionId; }
    public void setAdmissionId(Integer admissionId) { this.admissionId = admissionId; }

    public LocalDate getDateConsultation() { return dateConsultation; }
    public void setDateConsultation(LocalDate dateConsultation) { this.dateConsultation = dateConsultation; }

    public String getTypeDocument() { return typeDocument; }
    public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getMedecinNom() { return medecinNom; }
    public void setMedecinNom(String medecinNom) { this.medecinNom = medecinNom; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getDateImport() { return dateImport; }
    public void setDateImport(LocalDateTime dateImport) { this.dateImport = dateImport; }
}

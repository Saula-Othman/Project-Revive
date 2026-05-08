package pro.revive.entities;

import java.time.LocalDateTime;

public class Triage {

    private int idTriage;
    private int idAdmission;
    private int idPersonnel;
    private int idSalle;
    private float constancesTaSys;
    private float constancesTaDia;
    private int constancesPouls;
    private float constancesTemperature;
    private int spo2;
    private float glycemie;
    private int scoreDouleur;
    private int gcsScore;
    private int frequenceRespiratoire;
    private String symptomes;
    private int scoreCalcule;
    private int niveauAuto;
    private int niveauFinal;
    private String analyseAuto;
    private String overrideNote;
    private LocalDateTime dateOverride;
    private int idPersonnelOverride;
    private String patientState;
    private LocalDateTime dateHeureTriage;
    private LocalDateTime dateLiberation;
    private String nomPatient;
    private String prenomPatient;
    private String nomSalle;

    // ── Champs surveillance épidémiologique ──────────────────────
    private String syndromeCategory;
    private String dureeSymptomes;
    private String contactCasSimilaires;
    private boolean voyageRecent;
    private String voyageDestination;
    private String contagionFlag;
    private String suspectedDisease;

    public Triage() {
    }

    public Triage(int idAdmission, int idPersonnel, float constancesTaSys, float constancesTaDia,
                  int constancesPouls, float constancesTemperature, int spo2, float glycemie,
                  int scoreDouleur, int gcsScore, int frequenceRespiratoire, String symptomes) {
        this.idAdmission = idAdmission;
        this.idPersonnel = idPersonnel;
        this.constancesTaSys = constancesTaSys;
        this.constancesTaDia = constancesTaDia;
        this.constancesPouls = constancesPouls;
        this.constancesTemperature = constancesTemperature;
        this.spo2 = spo2;
        this.glycemie = glycemie;
        this.scoreDouleur = scoreDouleur;
        this.gcsScore = gcsScore;
        this.frequenceRespiratoire = frequenceRespiratoire;
        this.symptomes = symptomes;
        this.patientState = "Triaged";
        this.dateHeureTriage = LocalDateTime.now();
    }

    public int getIdTriage() { return idTriage; }
    public void setIdTriage(int idTriage) { this.idTriage = idTriage; }
    public int getIdAdmission() { return idAdmission; }
    public void setIdAdmission(int idAdmission) { this.idAdmission = idAdmission; }
    public int getIdPersonnel() { return idPersonnel; }
    public void setIdPersonnel(int idPersonnel) { this.idPersonnel = idPersonnel; }
    public int getIdSalle() { return idSalle; }
    public void setIdSalle(int idSalle) { this.idSalle = idSalle; }
    public float getConstancesTaSys() { return constancesTaSys; }
    public void setConstancesTaSys(float constancesTaSys) { this.constancesTaSys = constancesTaSys; }
    public float getConstancesTaDia() { return constancesTaDia; }
    public void setConstancesTaDia(float constancesTaDia) { this.constancesTaDia = constancesTaDia; }
    public int getConstancesPouls() { return constancesPouls; }
    public void setConstancesPouls(int constancesPouls) { this.constancesPouls = constancesPouls; }
    public float getConstancesTemperature() { return constancesTemperature; }
    public void setConstancesTemperature(float constancesTemperature) { this.constancesTemperature = constancesTemperature; }
    public int getSpo2() { return spo2; }
    public void setSpo2(int spo2) { this.spo2 = spo2; }
    public float getGlycemie() { return glycemie; }
    public void setGlycemie(float glycemie) { this.glycemie = glycemie; }
    public int getScoreDouleur() { return scoreDouleur; }
    public void setScoreDouleur(int scoreDouleur) { this.scoreDouleur = scoreDouleur; }
    public int getGcsScore() { return gcsScore; }
    public void setGcsScore(int gcsScore) { this.gcsScore = gcsScore; }
    public int getFrequenceRespiratoire() { return frequenceRespiratoire; }
    public void setFrequenceRespiratoire(int frequenceRespiratoire) { this.frequenceRespiratoire = frequenceRespiratoire; }
    public String getSymptomes() { return symptomes; }
    public void setSymptomes(String symptomes) { this.symptomes = symptomes; }
    public int getScoreCalcule() { return scoreCalcule; }
    public void setScoreCalcule(int scoreCalcule) { this.scoreCalcule = scoreCalcule; }
    public int getNiveauAuto() { return niveauAuto; }
    public void setNiveauAuto(int niveauAuto) { this.niveauAuto = niveauAuto; }
    public int getNiveauFinal() { return niveauFinal; }
    public void setNiveauFinal(int niveauFinal) { this.niveauFinal = niveauFinal; }
    public String getAnalyseAuto() { return analyseAuto; }
    public void setAnalyseAuto(String analyseAuto) { this.analyseAuto = analyseAuto; }
    public String getOverrideNote() { return overrideNote; }
    public void setOverrideNote(String overrideNote) { this.overrideNote = overrideNote; }
    public LocalDateTime getDateOverride() { return dateOverride; }
    public void setDateOverride(LocalDateTime dateOverride) { this.dateOverride = dateOverride; }
    public int getIdPersonnelOverride() { return idPersonnelOverride; }
    public void setIdPersonnelOverride(int idPersonnelOverride) { this.idPersonnelOverride = idPersonnelOverride; }
    public String getPatientState() { return patientState; }
    public void setPatientState(String patientState) { this.patientState = patientState; }
    public LocalDateTime getDateHeureTriage() { return dateHeureTriage; }
    public void setDateHeureTriage(LocalDateTime dateHeureTriage) { this.dateHeureTriage = dateHeureTriage; }
    public LocalDateTime getDateLiberation() { return dateLiberation; }
    public void setDateLiberation(LocalDateTime dateLiberation) { this.dateLiberation = dateLiberation; }
    public String getNomPatient() { return nomPatient; }
    public void setNomPatient(String nomPatient) { this.nomPatient = nomPatient; }
    public String getPrenomPatient() { return prenomPatient; }
    public void setPrenomPatient(String prenomPatient) { this.prenomPatient = prenomPatient; }
    public String getNomSalle() { return nomSalle; }
    public void setNomSalle(String nomSalle) { this.nomSalle = nomSalle; }

    // ── Getters/Setters surveillance épidémiologique ─────────────
    public String getSyndromeCategory() { return syndromeCategory; }
    public void setSyndromeCategory(String syndromeCategory) { this.syndromeCategory = syndromeCategory; }
    public String getDureeSymptomes() { return dureeSymptomes; }
    public void setDureeSymptomes(String dureeSymptomes) { this.dureeSymptomes = dureeSymptomes; }
    public String getContactCasSimilaires() { return contactCasSimilaires; }
    public void setContactCasSimilaires(String contactCasSimilaires) { this.contactCasSimilaires = contactCasSimilaires; }
    public boolean isVoyageRecent() { return voyageRecent; }
    public void setVoyageRecent(boolean voyageRecent) { this.voyageRecent = voyageRecent; }
    public String getVoyageDestination() { return voyageDestination; }
    public void setVoyageDestination(String voyageDestination) { this.voyageDestination = voyageDestination; }
    public String getContagionFlag() { return contagionFlag != null ? contagionFlag : "aucun"; }
    public void setContagionFlag(String contagionFlag) { this.contagionFlag = contagionFlag; }
    public String getSuspectedDisease() { return suspectedDisease; }
    public void setSuspectedDisease(String suspectedDisease) { this.suspectedDisease = suspectedDisease; }

    @Override
    public String toString() {
        return "Triage{" +
                "idTriage=" + idTriage +
                ", patient='" + nomPatient + " " + prenomPatient + '\'' +
                ", niveau=" + niveauFinal +
                ", state='" + patientState + '\'' +
                ", salle='" + nomSalle + '\'' +
                '}';
    }
}

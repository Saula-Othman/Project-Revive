package pro.revive.entities.EntitiesMed;

/**
 * Représente un enregistrement de triage avec les constantes vitales
 * et l'état du patient.
 */
public class TriagePatient {

    private int    idTriage;
    private int    idAdmission;
    private String nomPatient;
    private String prenomPatient;

    // Constantes vitales
    private double taSys;          // Tension systolique
    private double taDia;          // Tension diastolique
    private double poids;
    private double temperature;
    private double spo2;           // Saturation O2
    private double glycemie;
    private double scoreDouleur;
    private int    gcsScore;       // Glasgow Coma Scale
    private int    frequenceRespiratoire;

    // Évaluation
    private String symptomes;
    private int    scoreCalcule;
    private String niveauAuto;     // CRITIQUE, URGENT, MODERE, STABLE
    private String niveauFinal;
    private String analyseAuto;
    private String patientState;   // CRITIQUE, URGENT, MODERE, STABLE

    private String dateHeureTriage;

    // ── Getters & Setters ─────────────────────────────────────────────────

    public int getIdTriage() { return idTriage; }
    public void setIdTriage(int idTriage) { this.idTriage = idTriage; }

    public int getIdAdmission() { return idAdmission; }
    public void setIdAdmission(int idAdmission) { this.idAdmission = idAdmission; }

    public String getNomPatient() { return nomPatient; }
    public void setNomPatient(String nomPatient) { this.nomPatient = nomPatient; }

    public String getPrenomPatient() { return prenomPatient; }
    public void setPrenomPatient(String prenomPatient) { this.prenomPatient = prenomPatient; }

    public String getNomComplet() {
        return (prenomPatient != null ? prenomPatient : "") + " " +
               (nomPatient != null ? nomPatient : "");
    }

    public double getTaSys() { return taSys; }
    public void setTaSys(double taSys) { this.taSys = taSys; }

    public double getTaDia() { return taDia; }
    public void setTaDia(double taDia) { this.taDia = taDia; }

    public double getPoids() { return poids; }
    public void setPoids(double poids) { this.poids = poids; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getSpo2() { return spo2; }
    public void setSpo2(double spo2) { this.spo2 = spo2; }

    public double getGlycemie() { return glycemie; }
    public void setGlycemie(double glycemie) { this.glycemie = glycemie; }

    public double getScoreDouleur() { return scoreDouleur; }
    public void setScoreDouleur(double scoreDouleur) { this.scoreDouleur = scoreDouleur; }

    public int getGcsScore() { return gcsScore; }
    public void setGcsScore(int gcsScore) { this.gcsScore = gcsScore; }

    public int getFrequenceRespiratoire() { return frequenceRespiratoire; }
    public void setFrequenceRespiratoire(int frequenceRespiratoire) { this.frequenceRespiratoire = frequenceRespiratoire; }

    public String getSymptomes() { return symptomes; }
    public void setSymptomes(String symptomes) { this.symptomes = symptomes; }

    public int getScoreCalcule() { return scoreCalcule; }
    public void setScoreCalcule(int scoreCalcule) { this.scoreCalcule = scoreCalcule; }

    public String getNiveauAuto() { return niveauAuto; }
    public void setNiveauAuto(String niveauAuto) { this.niveauAuto = niveauAuto; }

    public String getNiveauFinal() { return niveauFinal; }
    public void setNiveauFinal(String niveauFinal) { this.niveauFinal = niveauFinal; }

    public String getAnalyseAuto() { return analyseAuto; }
    public void setAnalyseAuto(String analyseAuto) { this.analyseAuto = analyseAuto; }

    public String getPatientState() { return patientState; }
    public void setPatientState(String patientState) { this.patientState = patientState; }

    public String getDateHeureTriage() { return dateHeureTriage; }
    public void setDateHeureTriage(String dateHeureTriage) { this.dateHeureTriage = dateHeureTriage; }

    /** Retourne la couleur CSS selon le niveau */
    public String getNiveauColor() {
        String n = niveauFinal != null ? niveauFinal : patientState;
        if (n == null) return "#64748B";
        return switch (n.toUpperCase()) {
            case "CRITIQUE"  -> "#C0392B";   // rouge bordeaux doux
            case "URGENT"    -> "#D35400";   // orange brûlé doux
            case "MODERE"    -> "#B7950B";   // or/moutarde doux
            case "STABLE"    -> "#1E8449";   // vert forêt doux
            default          -> "#64748B";
        };
    }

    /** Retourne la couleur de fond selon le niveau */
    public String getNiveauBgColor() {
        String n = niveauFinal != null ? niveauFinal : patientState;
        if (n == null) return "#F8FAFC";
        return switch (n.toUpperCase()) {
            case "CRITIQUE"  -> "#FDECEA";   // rose très pâle
            case "URGENT"    -> "#FDF0E8";   // pêche très pâle
            case "MODERE"    -> "#FDF8E1";   // crème très pâle
            case "STABLE"    -> "#EAF7EE";   // vert menthe très pâle
            default          -> "#F8FAFC";
        };
    }
}

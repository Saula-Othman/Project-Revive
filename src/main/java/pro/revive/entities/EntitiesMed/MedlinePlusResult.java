package pro.revive.entities.EntitiesMed;

/**
 * POJO pour les resultats de l'API MedlinePlus.
 * Contient les informations de sante pour un code ICD-10.
 */
public class MedlinePlusResult {
    private String diseaseTitle;
    private String summary;
    private String fullInfoUrl;

    public MedlinePlusResult() {}

    public MedlinePlusResult(String diseaseTitle, String summary, String fullInfoUrl) {
        this.diseaseTitle = diseaseTitle;
        this.summary = summary;
        this.fullInfoUrl = fullInfoUrl;
    }

    public String getDiseaseTitle() {
        return diseaseTitle;
    }

    public void setDiseaseTitle(String diseaseTitle) {
        this.diseaseTitle = diseaseTitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFullInfoUrl() {
        return fullInfoUrl;
    }

    public void setFullInfoUrl(String fullInfoUrl) {
        this.fullInfoUrl = fullInfoUrl;
    }

    @Override
    public String toString() {
        return "MedlinePlusResult{" +
                "diseaseTitle='" + diseaseTitle + '\'' +
                ", summary='" + (summary != null && summary.length() > 50 
                    ? summary.substring(0, 50) + "..." : summary) + '\'' +
                ", fullInfoUrl='" + fullInfoUrl + '\'' +
                '}';
    }
}

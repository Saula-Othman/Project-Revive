package pro.revive.entities.EntitiesMed;

import java.util.List;

/**
 * POJO pour les conseils patients en francais.
 * Contient des conseils pratiques et des signes d'alerte par pathologie.
 */
public class AdviceData {
    private List<String> conseils;
    private List<String> alertes;
    private String descriptionFr;

    public AdviceData() {}

    public AdviceData(List<String> conseils, List<String> alertes, String descriptionFr) {
        this.conseils = conseils;
        this.alertes = alertes;
        this.descriptionFr = descriptionFr;
    }

    public List<String> getConseils() {
        return conseils;
    }

    public void setConseils(List<String> conseils) {
        this.conseils = conseils;
    }

    public List<String> getAlertes() {
        return alertes;
    }

    public void setAlertes(List<String> alertes) {
        this.alertes = alertes;
    }

    public String getDescriptionFr() {
        return descriptionFr;
    }

    public void setDescriptionFr(String descriptionFr) {
        this.descriptionFr = descriptionFr;
    }

    @Override
    public String toString() {
        return "AdviceData{" +
                "conseils=" + (conseils != null ? conseils.size() : 0) + " items" +
                ", alertes=" + (alertes != null ? alertes.size() : 0) + " items" +
                ", descriptionFr='" + descriptionFr + '\'' +
                '}';
    }
}

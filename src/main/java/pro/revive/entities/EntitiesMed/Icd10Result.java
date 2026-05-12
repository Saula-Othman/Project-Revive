package pro.revive.entities.EntitiesMed;

/**
 * Représente un résultat de recherche dans la classification ICD-10.
 * Utilisé pour l'auto-complétion et la saisie de diagnostics dans REVIVE — Module 3.
 */
public class Icd10Result {

    private final String code;
    private final String description;

    /**
     * @param code        Code ICD-10 (ex : "J18.9")
     * @param description Libellé associé (ex : "Pneumonie, sans précision")
     */
    public Icd10Result(String code, String description) {
        this.code        = code;
        this.description = description;
    }

    /** @return Le code ICD-10 */
    public String getCode() {
        return code;
    }

    /** @return Le libellé de la pathologie */
    public String getDescription() {
        return description;
    }

    /**
     * Retourne la représentation affichable : "CODE — description".
     */
    public String getLabel() {
        return code + " — " + description;
    }

    /**
     * Retourne une chaîne de stockage séparée par un pipe : "CODE|description".
     */
    public String toStorageString() {
        return code + "|" + description;
    }

    /**
     * Délègue à {@link #getLabel()} pour l'affichage dans les ComboBox JavaFX.
     */
    @Override
    public String toString() {
        return getLabel();
    }
}

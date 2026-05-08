package pro.revive.utils;

/**
 * Simple wrapper for admission ComboBox display.
 */
public class AdmissionItem {
    private final int id;
    private final String display;

    public AdmissionItem(int id, String patientNom, String patientPrenom) {
        this.id = id;
        this.display = patientNom + " " + patientPrenom;
    }

    public int getId() { return id; }

    @Override
    public String toString() { return display; }
}

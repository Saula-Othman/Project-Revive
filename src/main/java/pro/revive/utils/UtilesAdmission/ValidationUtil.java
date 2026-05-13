package pro.revive.utils.UtilesAdmission;

import javafx.scene.control.TextField;
import java.time.LocalDate;

public class ValidationUtil {

    // Lettres + espaces + tirets uniquement (pas de chiffres) pour nom/prénom
    public static void applyLettersOnly(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("[a-zA-Z\u00C0-\u00FF\\s\\-]*")) {
                field.setText(newVal.replaceAll("[^a-zA-Z\u00C0-\u00FF\\s\\-]", ""));
            }
        });
    }

    // Formateur téléphone : détecte le préfixe tunisien et rejette 0/1
    public static void applyPhoneFormatter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Autoriser uniquement chiffres et +
            String cleaned = newVal.replaceAll("[^\\d+]", "");
            if (!cleaned.equals(newVal)) {
                field.setText(cleaned);
                return;
            }
            // Feedback visuel en temps réel si commence par 0 ou 1
            if (!cleaned.isEmpty() && !cleaned.startsWith("+")) {
                String digits = cleaned.replaceAll("\\D", "");
                if (!digits.isEmpty()) {
                    char first = digits.charAt(0);
                    if (first == '0' || first == '1') {
                        field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;");
                    } else {
                        field.setStyle("");
                    }
                }
            } else {
                field.setStyle("");
            }
        });
        field.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) formatPhone(field);
        });
    }

    private static void formatPhone(TextField field) {
        String val = field.getText();
        if (val == null || val.trim().isEmpty()) return;
        String digits = val.replaceAll("\\D", "");
        if (digits.isEmpty()) return;
        // Si déjà préfixé +216
        if (val.startsWith("+")) {
            if (digits.startsWith("216") && digits.length() == 11) {
                String local = digits.substring(3);
                field.setText("+216 " + local.substring(0,2) + " " + local.substring(2,5) + " " + local.substring(5));
            }
            return;
        }
        if (digits.length() == 8) {
            char first = digits.charAt(0);
            // Tunisien valide : 2, 5, 7, 9
            if (first == '2' || first == '5' || first == '7' || first == '9') {
                field.setText("+216 " + digits.substring(0,2) + " " + digits.substring(2,5) + " " + digits.substring(5));
                field.setStyle("");
                return;
            }
            // Invalide : 0 ou 1 — laisser rouge, ne pas formater
        }
        if (digits.length() > 8 && !digits.startsWith("0") && !digits.startsWith("1")) {
            field.setText("+" + digits);
            field.setStyle("");
        }
    }

    // CIN tunisien : exactement 8 chiffres
    public static void applyCinFormatter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("\\D", "");
            if (digits.length() > 8) digits = digits.substring(0, 8);
            if (!digits.equals(newVal)) field.setText(digits);
        });
    }

    // Numéro sécurité sociale : chiffres uniquement, max 20 caractères
    public static void applySecuFormatter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String cleaned = newVal.replaceAll("[^\\d]", "");
            if (cleaned.length() > 20) cleaned = cleaned.substring(0, 20);
            if (!cleaned.equals(newVal)) field.setText(cleaned);
        });
    }

    // Nom/prénom : lettres + espaces + tirets uniquement, PAS de chiffres
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty()
               && name.matches("[a-zA-Z\u00C0-\u00FF\\s\\-]+");
    }

    // Téléphone : rejet si commence par 0 ou 1, ou vide (vide = optionnel → ok)
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) return true;
        // Rejeter les numéros commençant par 0 ou 1
        char first = digits.charAt(0);
        if (first == '0' || first == '1') return false;
        // Longueur 8 (local) ou 11 avec indicatif +216
        return digits.length() == 8 || digits.length() == 11 || digits.length() >= 9;
    }

    // Numéro de sécurité sociale : chiffres uniquement, entre 1 et 20 caractères
    public static boolean isValidNumSecu(String secu) {
        if (secu == null || secu.trim().isEmpty()) return true; // optionnel
        return secu.matches("\\d{1,20}");
    }

    // CIN : 8 chiffres exactement (optionnel)
    public static boolean isValidCin(String cin) {
        if (cin == null || cin.trim().isEmpty()) return true;
        return cin.matches("\\d{8}");
    }

    // Date de naissance : ne peut pas être dans le futur
    public static boolean isDateNaissanceValide(LocalDate date) {
        if (date == null) return true; // optionnel
        return !date.isAfter(LocalDate.now());
    }
}

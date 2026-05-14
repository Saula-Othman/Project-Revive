package pro.revive.utils.UtilesAdmission;

import javafx.scene.control.TextField;
import java.time.LocalDate;

public class ValidationUtil {
    private static final String NAME_PATTERN = "[a-zA-Z\\u00C0-\\u00FF\\s\\-]+";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public static void applyLettersOnly(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.matches("[a-zA-Z\\u00C0-\\u00FF\\s\\-]*")) {
                field.setText(newVal.replaceAll("[^a-zA-Z\\u00C0-\\u00FF\\s\\-]", ""));
            }
        });
    }

    public static void applyPhoneFormatter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String cleaned = newVal.replaceAll("[^\\d+]", "");
            if (!cleaned.equals(newVal)) {
                field.setText(cleaned);
                return;
            }
            if (cleaned.trim().isEmpty() || isValidPhone(cleaned)) {
                field.setStyle("");
            } else {
                field.setStyle("-fx-border-color: #dc2626; -fx-border-width: 1.5px;");
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
        if (digits.startsWith("216") && digits.length() == 11) {
            digits = digits.substring(3);
        }
        if (digits.length() == 8 && isTunisianLocalPhone(digits)) {
            field.setText("+216 " + digits.substring(0, 2) + " " + digits.substring(2, 5) + " " + digits.substring(5));
            field.setStyle("");
        }
    }

    public static void applyCinFormatter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String digits = newVal.replaceAll("\\D", "");
            if (digits.length() > 8) digits = digits.substring(0, 8);
            if (!digits.equals(newVal)) field.setText(digits);
        });
    }

    public static void applySecuFormatter(TextField field) {
        field.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String cleaned = newVal.replaceAll("\\D", "");
            if (cleaned.length() > 10) cleaned = cleaned.substring(0, 10);
            if (!cleaned.equals(newVal)) field.setText(cleaned);
        });
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.trim().matches(NAME_PATTERN);
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return true;
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("216") && digits.length() == 11) {
            digits = digits.substring(3);
        }
        return digits.length() == 8 && isTunisianLocalPhone(digits);
    }

    public static boolean isRequiredValidPhone(String phone) {
        return phone != null && !phone.trim().isEmpty() && isValidPhone(phone);
    }

    public static boolean isValidNumSecu(String secu) {
        if (secu == null || secu.trim().isEmpty()) return true;
        return secu.trim().matches("\\d{10}");
    }

    public static boolean isRequiredValidNumSecu(String secu) {
        return secu != null && !secu.trim().isEmpty() && isValidNumSecu(secu);
    }

    public static boolean isValidCin(String cin) {
        if (cin == null || cin.trim().isEmpty()) return true;
        return cin.trim().matches("\\d{8}");
    }

    public static boolean isRequiredValidCin(String cin) {
        return cin != null && !cin.trim().isEmpty() && isValidCin(cin);
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return true;
        return email.trim().matches(EMAIL_PATTERN);
    }

    public static boolean isDateNaissanceValide(LocalDate date) {
        if (date == null) return true;
        return !date.isAfter(LocalDate.now());
    }

    private static boolean isTunisianLocalPhone(String digits) {
        if (digits == null || digits.length() != 8) return false;
        char first = digits.charAt(0);
        return first == '2' || first == '5' || first == '7' || first == '9';
    }
}

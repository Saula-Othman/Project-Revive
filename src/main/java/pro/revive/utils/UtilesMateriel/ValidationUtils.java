package pro.revive.utils.UtilesMateriel;

import javafx.scene.control.TextField;

import java.util.regex.Pattern;

/**
 * Classe utilitaire pour les validations de saisie
 */
public class ValidationUtils {

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{8,15}$");
    private static final Pattern NUMERO_SERIE_PATTERN = Pattern.compile("^[A-Z]{3}-\\d{3}-[A-Z]{2}$");
    
    /**
     * Applique un filtre pour accepter uniquement les chiffres
     */
    public static void setNumericOnly(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(oldValue);
            }
        });
    }
    
    /**
     * Applique un filtre pour accepter les nombres décimaux
     */
    public static void setDecimalOnly(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*\\.?\\d*")) {
                textField.setText(oldValue);
            }
        });
    }
    
    /**
     * Applique un filtre pour accepter uniquement les lettres et espaces
     */
    public static void setAlphaOnly(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZÀ-ÿ\\s-]*")) {
                textField.setText(oldValue);
            }
        });
    }
    
    /**
     * Applique un filtre pour accepter lettres, chiffres et tirets
     */
    public static void setAlphanumericWithDash(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-Z0-9\\s-]*")) {
                textField.setText(oldValue);
            }
        });
    }
    
    /**
     * Limite le nombre de caractères
     */
    public static void setMaxLength(TextField textField, int maxLength) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue.length() > maxLength) {
                textField.setText(oldValue);
            }
        });
    }
    
    /**
     * Applique un format pour numéro de série (AMB-001-TN)
     */
    public static void setNumeroSerieFormat(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            String filtered = newValue.toUpperCase().replaceAll("[^A-Z0-9-]", "");
            if (!filtered.equals(newValue)) {
                textField.setText(filtered);
            }
        });
        setMaxLength(textField, 11); // AMB-001-TN = 11 caractères
    }
    
    /**
     * Applique un format pour année (4 chiffres, entre 1900 et 2100)
     */
    public static void setYearFormat(TextField textField) {
        textField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d{0,4}")) {
                textField.setText(oldValue);
            } else if (newValue.length() == 4) {
                int year = Integer.parseInt(newValue);
                if (year < 1900 || year > 2100) {
                    textField.setText(oldValue);
                }
            }
        });
    }
    
    /**
     * Vérifie si un champ est vide
     */
    public static boolean isEmpty(TextField textField) {
        return textField.getText() == null || textField.getText().trim().isEmpty();
    }
    
    /**
     * Vérifie si un email est valide
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Vérifie si un numéro de téléphone est valide
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Vérifie si un numéro de série est valide (format AMB-001-TN)
     */
    public static boolean isValidNumeroSerie(String numeroSerie) {
        return numeroSerie != null && NUMERO_SERIE_PATTERN.matcher(numeroSerie).matches();
    }
    
    /**
     * Vérifie si une valeur numérique est positive
     */
    public static boolean isPositive(String value) {
        try {
            double num = Double.parseDouble(value);
            return num >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Vérifie si une valeur est dans une plage
     */
    public static boolean isInRange(String value, double min, double max) {
        try {
            double num = Double.parseDouble(value);
            return num >= min && num <= max;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Ajoute un style d'erreur au champ
     */
    public static void setErrorStyle(TextField textField) {
        textField.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2px; -fx-border-radius: 6;");
    }
    
    /**
     * Retire le style d'erreur du champ
     */
    public static void clearErrorStyle(TextField textField) {
        textField.setStyle("");
    }
    
    /**
     * Valide et retourne un message d'erreur si invalide
     */
    public static String validateRequired(TextField textField, String fieldName) {
        if (isEmpty(textField)) {
            setErrorStyle(textField);
            return fieldName + " est obligatoire";
        }
        clearErrorStyle(textField);
        return null;
    }
    
    /**
     * Valide un champ numérique
     */
    public static String validateNumeric(TextField textField, String fieldName) {
        if (isEmpty(textField)) {
            return null; // Optionnel
        }
        if (!isPositive(textField.getText())) {
            setErrorStyle(textField);
            return fieldName + " doit être un nombre positif";
        }
        clearErrorStyle(textField);
        return null;
    }
}

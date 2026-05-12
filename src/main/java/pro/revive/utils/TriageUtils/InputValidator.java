package pro.revive.utils.TriageUtils;

import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Restrictive input controls — physically blocks invalid characters while typing.
 *
 * - Integer fields  : digits 0-9 only
 * - Decimal fields  : digits + one dot only
 * - Text fields     : free text capped at a max length
 */
public class InputValidator {

    // ── CSS classes ────────────────────────────────────────────────────────
    private static final String STYLE_ERROR   = "field-error";
    private static final String STYLE_SUCCESS = "field-success";

    // ══════════════════════════════════════════════════════════════════════
    // Restrictive listeners — call once in initialize()
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Integer field: only digits 0-9 allowed.
     * Non-digit characters are stripped immediately as the user types.
     */
    public static void attachIntListener(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Keep only digit characters
            String filtered = newVal.replaceAll("[^0-9]", "");
            if (!filtered.equals(newVal)) {
                tf.setText(filtered);
                tf.positionCaret(filtered.length());
            }
        });
    }

    /**
     * Decimal field: only digits and a single dot allowed.
     * Strips anything else; prevents a second dot from being entered.
     */
    public static void attachDecimalListener(TextField tf) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Keep only digits and dot
            String filtered = newVal.replaceAll("[^0-9.]", "");
            // Allow only one dot
            int firstDot = filtered.indexOf('.');
            if (firstDot != -1) {
                String before = filtered.substring(0, firstDot + 1);
                String after  = filtered.substring(firstDot + 1).replace(".", "");
                filtered = before + after;
            }
            if (!filtered.equals(newVal)) {
                tf.setText(filtered);
                tf.positionCaret(filtered.length());
            }
        });
    }

    /**
     * Text field / TextArea: caps input at maxLength characters.
     * Any character beyond the limit is silently dropped.
     */
    public static void attachMaxLengthListener(TextField tf, int maxLength) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > maxLength) {
                tf.setText(newVal.substring(0, maxLength));
                tf.positionCaret(maxLength);
            }
        });
    }

    public static void attachMaxLengthListener(TextArea ta, int maxLength) {
        ta.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.length() > maxLength) {
                ta.setText(newVal.substring(0, maxLength));
                ta.positionCaret(maxLength);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Restrictive range listeners — block input above max while typing,
    // mark red/green on focus-lost if below min
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Integer field: digits only, caps at max while typing, marks red if below min on focus-lost.
     */
    public static void attachIntRestrictiveListener(TextField tf, int min, int max) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^0-9]", "");
            // No capping while typing — out-of-range values are flagged red on focus-lost
            if (!filtered.equals(newVal)) {
                tf.setText(filtered);
                tf.positionCaret(filtered.length());
            }
        });
        tf.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) checkIntRangeNow(tf, min, max);
        });
    }

    /**
     * Decimal field with auto-dot and decimal place limit.
     *
     * @param dotAfterDigits   auto-insert "." after this many integer digits are typed
     * @param maxDecimalPlaces max digits allowed after the dot
     * @param min              mark red on focus-lost if below this value
     * @param max              cap input at this value while typing
     *
     * Examples:
     *   Glycemie   → dotAfterDigits=1, maxDecimalPlaces=2  →  "5.45"
     *   Temperature → dotAfterDigits=2, maxDecimalPlaces=2  → "36.75"
     *   Tension    → dotAfterDigits=2, maxDecimalPlaces=1  → "12.5"
     */
    public static void attachDecimalAutoDotListener(TextField tf, int dotAfterDigits,
                                                     int maxDecimalPlaces, float min, float max) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            // Allow only digits and one dot
            String filtered = newVal.replaceAll("[^0-9.]", "");
            int firstDot = filtered.indexOf('.');
            if (firstDot != -1) {
                String before = filtered.substring(0, firstDot + 1);
                String after  = filtered.substring(firstDot + 1).replace(".", "");
                // Limit digits after the dot
                if (after.length() > maxDecimalPlaces) after = after.substring(0, maxDecimalPlaces);
                filtered = before + after;
            }
            // Auto-insert dot after exactly dotAfterDigits integer digits (only if no dot yet)
            // Skip if the user just manually deleted the dot — don't re-insert it
            boolean userDeletedDot = oldVal != null && oldVal.contains(".")
                                     && !newVal.contains(".")
                                     && newVal.length() < oldVal.length();
            if (!userDeletedDot && filtered.indexOf('.') == -1) {
                long digits = filtered.chars().filter(Character::isDigit).count();
                if (digits >= dotAfterDigits) {
                    int insertAt = 0, seen = 0;
                    for (int i = 0; i < filtered.length(); i++) {
                        if (Character.isDigit(filtered.charAt(i)) && ++seen == dotAfterDigits) {
                            insertAt = i + 1;
                            break;
                        }
                    }
                    filtered = filtered.substring(0, insertAt) + "." + filtered.substring(insertAt);
                }
            }
            // No capping while typing — out-of-range values are flagged red on focus-lost
            if (!filtered.equals(newVal)) {
                tf.setText(filtered);
                tf.positionCaret(filtered.length());
            }
        });
        tf.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) checkDecimalRangeNow(tf, min, max);
        });
    }

    /**
     * Decimal field: digits + one dot, caps at max while typing, marks red if below min on focus-lost.
     */
    public static void attachDecimalRestrictiveListener(TextField tf, float min, float max) {
        tf.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            String filtered = newVal.replaceAll("[^0-9.]", "");
            int firstDot = filtered.indexOf('.');
            if (firstDot != -1) {
                String before = filtered.substring(0, firstDot + 1);
                String after  = filtered.substring(firstDot + 1).replace(".", "");
                filtered = before + after;
            }
            if (!filtered.isEmpty() && !filtered.equals(".")) {
                try {
                    float val = Float.parseFloat(filtered);
                    if (val > max) filtered = String.valueOf(max);
                } catch (NumberFormatException ignored) {}
            }
            if (!filtered.equals(newVal)) {
                tf.setText(filtered);
                tf.positionCaret(filtered.length());
            }
        });
        tf.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) checkDecimalRangeNow(tf, min, max);
        });
    }

    // ══════════════════════════════════════════════════════════════════════
    // Range listeners — call once in initialize(), fire on focus-lost
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Marks field red/green when the user leaves it if the value is out of [min,max].
     */
    public static void attachIntRangeListener(TextField tf, int min, int max) {
        tf.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) checkIntRangeNow(tf, min, max);
        });
    }

    public static void attachDecimalRangeListener(TextField tf, float min, float max) {
        tf.focusedProperty().addListener((obs, wasFocused, focused) -> {
            if (!focused) checkDecimalRangeNow(tf, min, max);
        });
    }

    private static void checkIntRangeNow(TextField tf, int min, int max) {
        String v = tf.getText() == null ? "" : tf.getText().trim();
        if (v.isEmpty()) return; // empty check handled separately
        try {
            int val = Integer.parseInt(v);
            if (val < min || val > max) markError(tf);
            else markSuccess(tf);
        } catch (NumberFormatException e) { markError(tf); }
    }

    private static void checkDecimalRangeNow(TextField tf, float min, float max) {
        String v = tf.getText() == null ? "" : tf.getText().trim();
        if (v.isEmpty()) return;
        // Strip trailing dot before parsing
        if (v.endsWith(".")) v = v.substring(0, v.length() - 1);
        try {
            float val = Float.parseFloat(v);
            if (val < min || val > max) markError(tf);
            else markSuccess(tf);
        } catch (NumberFormatException e) { markError(tf); }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Submit-time validation (used on Save / Update buttons)
    // ══════════════════════════════════════════════════════════════════════

    public static class Result {
        private final java.util.List<String> errors = new java.util.ArrayList<>();
        public void addError(String msg) { errors.add(msg); }
        public boolean isValid()         { return errors.isEmpty(); }
        public String summary()          { return String.join("\n", errors); }
    }

    /**
     * Checks that all vital fields are non-empty before saving.
     * Also marks fields visually.
     */
    public static Result validateNotEmpty(
            TextField tfTaSys, TextField tfTaDia,
            TextField tfPouls, TextField tfTemp,
            TextField tfSpo2,  TextField tfGlyc,
            TextField tfGcs,   TextField tfFreqResp,
            TextArea  taSymptomes) {

        Result r = new Result();
        checkNotEmpty(tfTaSys,    "Tension Systolique",    r);
        checkNotEmpty(tfTaDia,    "Tension Diastolique",   r);
        checkNotEmpty(tfPouls,    "Pouls",                 r);
        checkNotEmpty(tfTemp,     "Temperature",           r);
        checkNotEmpty(tfSpo2,     "SpO2",                  r);
        checkNotEmpty(tfGlyc,     "Glycemie",              r);
        checkNotEmpty(tfGcs,      "GCS Score",             r);
        checkNotEmpty(tfFreqResp, "Freq. Respiratoire",    r);
        checkTextAreaNotEmpty(taSymptomes, "Symptomes",    r);
        return r;
    }

    /**
     * Checks that all vital values are within their medical acceptable range.
     * Call this after validateNotEmpty passes.
     */
    public static Result validateRanges(
            TextField tfTaSys, TextField tfTaDia,
            TextField tfPouls, TextField tfTemp,
            TextField tfSpo2,  TextField tfGlyc,
            TextField tfGcs,   TextField tfFreqResp) {

        Result r = new Result();
        checkFloatRange(tfTaSys,    "Tension Systolique",    5.0f,  30.0f, r);
        checkFloatRange(tfTaDia,    "Tension Diastolique",   2.0f,  20.0f, r);
        checkIntRange  (tfPouls,    "Pouls",                20,   300,  r);
        checkFloatRange(tfTemp,     "Temperature",          30.0f, 45.0f, r);
        checkIntRange  (tfSpo2,     "SpO2",                 50,   100,  r);
        checkFloatRange(tfGlyc,     "Glycemie",             0.1f,  10.0f, r);
        checkIntRange  (tfGcs,      "GCS Score",            3,    15,   r);
        checkIntRange  (tfFreqResp, "Freq. Respiratoire",   1,    60,   r);
        return r;
    }

    private static void checkIntRange(TextField tf, String label, int min, int max, Result r) {
        String v = tf.getText() == null ? "" : tf.getText().trim();
        if (v.isEmpty()) return; // already caught by validateNotEmpty
        try {
            int val = Integer.parseInt(v);
            if (val < min || val > max) {
                markError(tf);
                r.addError("• " + label + " : valeur " + val + " hors plage [" + min + " – " + max + "].");
            } else {
                markSuccess(tf);
            }
        } catch (NumberFormatException e) {
            markError(tf);
            r.addError("• " + label + " : valeur numerique invalide.");
        }
    }

    private static void checkFloatRange(TextField tf, String label, float min, float max, Result r) {
        String v = tf.getText() == null ? "" : tf.getText().trim();
        if (v.isEmpty()) return;
        try {
            float val = Float.parseFloat(v);
            if (val < min || val > max) {
                markError(tf);
                r.addError("• " + label + " : valeur " + val + " hors plage [" + min + " – " + max + "].");
            } else {
                markSuccess(tf);
            }
        } catch (NumberFormatException e) {
            markError(tf);
            r.addError("• " + label + " : valeur numerique invalide.");
        }
    }

    private static void checkNotEmpty(TextField tf, String label, Result r) {
        String v = tf.getText() == null ? "" : tf.getText().trim();
        if (v.isEmpty()) {
            markError(tf);
            r.addError("• " + label + " : champ obligatoire.");
        } else {
            markSuccess(tf);
        }
    }

    private static void checkTextAreaNotEmpty(TextArea ta, String label, Result r) {
        String v = ta.getText() == null ? "" : ta.getText().trim();
        if (v.isEmpty()) {
            markError(ta);
            r.addError("• " + label + " : champ obligatoire.");
        } else {
            markSuccess(ta);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // Visual markers
    // ══════════════════════════════════════════════════════════════════════

    public static void markError(javafx.scene.Node node) {
        node.getStyleClass().remove(STYLE_SUCCESS);
        if (!node.getStyleClass().contains(STYLE_ERROR))
            node.getStyleClass().add(STYLE_ERROR);
    }

    public static void markSuccess(javafx.scene.Node node) {
        node.getStyleClass().remove(STYLE_ERROR);
        if (!node.getStyleClass().contains(STYLE_SUCCESS))
            node.getStyleClass().add(STYLE_SUCCESS);
    }

    public static void markNeutral(javafx.scene.Node node) {
        node.getStyleClass().remove(STYLE_ERROR);
        node.getStyleClass().remove(STYLE_SUCCESS);
    }

    public static void resetAll(javafx.scene.Node... nodes) {
        for (javafx.scene.Node n : nodes) markNeutral(n);
    }

    /** Attach combo listener for visual feedback */
    public static <T> void attachComboListener(ComboBox<T> cb) {
        cb.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) markError(cb);
            else                markSuccess(cb);
        });
    }
}

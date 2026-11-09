package cl.textilcircular.api.shared.validation;

import cl.textilcircular.api.shared.error.ValidationException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Input rules shared by the entities. Limits match the database CHECK constraints and the
 * messages match the mobile app, so the client shows the same text it showed offline.
 */
public final class Validators {

    public static final int ALIAS_MAX = 40;
    public static final int EMAIL_MAX = 254;
    public static final int PERSON_NAME_MAX = 60;
    public static final int BUSINESS_NAME_MAX = 100;
    public static final int BUSINESS_DESCRIPTION_MAX = 500;
    public static final int URL_MAX = 500;
    public static final int PRODUCT_TITLE_MAX = 100;
    public static final int PRODUCT_DESCRIPTION_MAX = 1000;
    public static final int MESSAGE_MAX = 2000;
    public static final int COMMENT_MAX = 500;
    public static final int CONTENT_TITLE_MAX = 150;
    public static final int EVENT_LOCATION_MAX = 200;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern RUT = Pattern.compile("^\\d{7,8}-[\\dK]$");

    private Validators() {
    }

    /** Required text: trimmed, not empty, at most {@code max} characters. */
    public static String text(String field, String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            throw new ValidationException(field, "Ingresa " + label);
        }
        if (value.length() > max) {
            throw new ValidationException(field, tooLong(label, max));
        }
        return value;
    }

    /** Optional text: blank becomes {@code null}. */
    public static String optionalText(String field, String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > max) {
            throw new ValidationException(field, tooLong(label, max));
        }
        return value;
    }

    /** Emails are stored trimmed and lowercase. */
    public static String email(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new ValidationException("email", "Ingresa tu correo");
        }
        if (value.length() > EMAIL_MAX || !EMAIL.matcher(value).matches()) {
            throw new ValidationException("email", "Ingresa un correo válido");
        }
        return value;
    }

    /** 8 or more characters, with at least one letter and one digit. Returns the raw password. */
    public static String password(String raw) {
        String value = raw == null ? "" : raw;
        boolean hasLetter = value.chars().anyMatch(Character::isLetter);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        if (value.length() < 8 || !hasLetter || !hasDigit) {
            throw new ValidationException("password",
                    "La contraseña debe tener 8 o más caracteres, con letras y números");
        }
        return value;
    }

    /** Accepts "12.345.678-5" or "123456785" and returns "12345678-5" after checking the verifier digit. */
    public static String rut(String raw) {
        String value = raw == null ? "" : raw.replace(".", "").replace(" ", "").toUpperCase(Locale.ROOT);
        if (!value.isEmpty() && value.indexOf('-') < 0) {
            value = value.substring(0, value.length() - 1) + "-" + value.charAt(value.length() - 1);
        }
        if (!RUT.matcher(value).matches()) {
            throw new ValidationException("rut", "Ingresa un RUT válido, por ejemplo 12345678-5");
        }
        String body = value.substring(0, value.indexOf('-'));
        if (checkDigit(body) != value.charAt(value.length() - 1)) {
            throw new ValidationException("rut", "El dígito verificador del RUT no coincide");
        }
        return value;
    }

    /** Chilean RUT verifier digit (modulo 11, weights 2..7). */
    public static char checkDigit(String body) {
        int sum = 0;
        int factor = 2;
        for (int i = body.length() - 1; i >= 0; i--) {
            sum += Character.digit(body.charAt(i), 10) * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int result = 11 - sum % 11;
        return switch (result) {
            case 11 -> '0';
            case 10 -> 'K';
            default -> Character.forDigit(result, 10);
        };
    }

    /** Prices are optional CLP amounts and cannot be negative. */
    public static Long price(Long raw) {
        if (raw != null && raw < 0) {
            throw new ValidationException("price", "El precio no puede ser negativo");
        }
        return raw;
    }

    /** Fails with {@code message} on {@code field} when a required reference is missing. */
    public static <T> T required(String field, T value, String message) {
        if (value == null) {
            throw new ValidationException(field, message);
        }
        return value;
    }

    private static String tooLong(String label, int max) {
        return Character.toUpperCase(label.charAt(0)) + label.substring(1) + " admite hasta " + max + " caracteres";
    }
}

package dev.denison.api.pedeai.core.application.shared;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public static void requireNonNull(Object o, String message) {
        if (o == null) {
            throw new ValidationException(message);
        }
    }

    public static String requireNonBlank(String text, String message) {
        requireNonNull(text, message);
        text = text.trim();
        if (text.isBlank()) {
            throw new ValidationException(message);
        }
        return text;
    }

    public static void requirePositive(Long number, String message) {
        requireNonNull(number, message);
        if (number <= 0L) {
            throw new ValidationException(message);
        }
    }
}

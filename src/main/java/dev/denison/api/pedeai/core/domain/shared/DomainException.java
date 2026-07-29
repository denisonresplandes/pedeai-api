package dev.denison.api.pedeai.core.domain.shared;

public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public static void requireNonNull(Object o, String message) {
        if (o == null) {
            throw new DomainException(message);
        }
    }

    public static String requireNonBlank(String text, String message) {
        requireNonNull(text, message);
        text = text.trim();
        if (text.isBlank()) {
            throw new DomainException(message);
        }
        return text;
    }

    public static void requirePositive(Long number, String message) {
        requireNonNull(number, message);
        if (number <= 0L) {
            throw new DomainException(message);
        }
    }
}

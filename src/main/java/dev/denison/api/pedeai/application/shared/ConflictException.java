package dev.denison.api.pedeai.application.shared;

public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

package dev.denison.pedeai.kitchen.exception;

import dev.denison.pedeai.shared.exception.ConflictException;

public class KitchenNameAlreadyExistsException extends ConflictException {

    public KitchenNameAlreadyExistsException(String name) {
        super("Kitchen name already in use: %s".formatted(name));
    }
}

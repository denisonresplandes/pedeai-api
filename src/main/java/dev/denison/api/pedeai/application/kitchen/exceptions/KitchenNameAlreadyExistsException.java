package dev.denison.api.pedeai.application.kitchen.exceptions;

import dev.denison.api.pedeai.application.shared.ConflictException;

public class KitchenNameAlreadyExistsException extends ConflictException {

    public KitchenNameAlreadyExistsException(String message) {
        super(message);
    }
}

package dev.denison.api.pedeai.core.application.kitchen.exceptions;

import dev.denison.api.pedeai.core.application.shared.ConflictException;

public class KitchenNameAlreadyExistsException extends ConflictException {

    public KitchenNameAlreadyExistsException(String message) {
        super(message);
    }
}

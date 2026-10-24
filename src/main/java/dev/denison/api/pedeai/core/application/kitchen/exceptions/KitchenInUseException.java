package dev.denison.api.pedeai.core.application.kitchen.exceptions;

import dev.denison.api.pedeai.core.application.shared.ConflictException;

public class KitchenInUseException extends ConflictException {

    public KitchenInUseException(Long id) {
        super("The kitchen %d cannot be deleted because it has linked restaurants.".formatted(id));
    }
}

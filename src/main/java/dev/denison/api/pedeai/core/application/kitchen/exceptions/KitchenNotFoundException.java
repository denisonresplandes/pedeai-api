package dev.denison.api.pedeai.core.application.kitchen.exceptions;

import dev.denison.api.pedeai.core.application.shared.ResourceNotFoundException;

public class KitchenNotFoundException extends ResourceNotFoundException {

    public KitchenNotFoundException(Long id) {
        super("Kitchen with id %d not found.".formatted(id));
    }
}

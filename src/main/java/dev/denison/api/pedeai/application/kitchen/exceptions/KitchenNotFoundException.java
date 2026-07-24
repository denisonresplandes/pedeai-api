package dev.denison.api.pedeai.application.kitchen.exceptions;

import dev.denison.api.pedeai.application.shared.ResourceNotFoundException;

public class KitchenNotFoundException extends ResourceNotFoundException {

    public KitchenNotFoundException(Long id) {
        super("Kitchen with id %d not found.".formatted(id));
    }

    public KitchenNotFoundException(String name) {
        super("Kitchen with name %s not found.".formatted(name));
    }
}

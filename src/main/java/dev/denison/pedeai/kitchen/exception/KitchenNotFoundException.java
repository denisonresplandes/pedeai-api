package dev.denison.pedeai.kitchen.exception;

import dev.denison.pedeai.shared.exception.NotFoundException;

public class KitchenNotFoundException extends NotFoundException {

    public KitchenNotFoundException(Long id) {
        super("Kitchen not found id: %d".formatted(id));
    }
}

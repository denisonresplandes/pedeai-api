package dev.denison.api.pedeai.core.application.restaurant.exceptions;

import dev.denison.api.pedeai.core.application.shared.ConflictException;

public class RestaurantNameAlreadyExistsException extends ConflictException {

    public RestaurantNameAlreadyExistsException(String name) {
        super("This %s already exists.".formatted(name));
    }
}

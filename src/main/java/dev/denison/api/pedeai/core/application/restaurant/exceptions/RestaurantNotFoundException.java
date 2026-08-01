package dev.denison.api.pedeai.core.application.restaurant.exceptions;

import dev.denison.api.pedeai.core.application.shared.ResourceNotFoundException;

public class RestaurantNotFoundException extends ResourceNotFoundException {

    public RestaurantNotFoundException(Long id) {
        super("Restaurant with id %d not found.".formatted(id));
    }

    public RestaurantNotFoundException(String name) {
        super("Restaurant with name %s not found.".formatted(name));
    }
}

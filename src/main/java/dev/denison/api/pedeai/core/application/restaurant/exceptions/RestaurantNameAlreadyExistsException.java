package dev.denison.api.pedeai.core.application.restaurant.exceptions;

import dev.denison.api.pedeai.core.application.shared.ConflictException;

public class RestaurantNameAlreadyExistsException extends ConflictException {

    // A mensagem vira o "detail" do 409, então cita o nome entre aspas (ele pode ter espaços).
    public RestaurantNameAlreadyExistsException(String name) {
        super("A restaurant named '%s' already exists.".formatted(name));
    }
}

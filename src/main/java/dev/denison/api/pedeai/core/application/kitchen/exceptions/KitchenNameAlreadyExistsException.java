package dev.denison.api.pedeai.core.application.kitchen.exceptions;

import dev.denison.api.pedeai.core.application.shared.ConflictException;

public class KitchenNameAlreadyExistsException extends ConflictException {

    // A mensagem vira o "detail" do 409, então cita o nome entre aspas (ele pode ter espaços).
    public KitchenNameAlreadyExistsException(String name) {
        super("A kitchen named '%s' already exists.".formatted(name));
    }
}

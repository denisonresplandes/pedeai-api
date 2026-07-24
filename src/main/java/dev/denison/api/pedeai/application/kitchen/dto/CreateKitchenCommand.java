package dev.denison.api.pedeai.application.kitchen.dto;

import dev.denison.api.pedeai.application.shared.ValidationException;

public record CreateKitchenCommand(
        String name
) {
    public CreateKitchenCommand {
        name = ValidationException.requireNonBlank(
                name,
                "Kitchen name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new ValidationException("kitchen name must be at least 2 characters long.");
        }
    }
}
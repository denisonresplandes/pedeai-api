package dev.denison.api.pedeai.core.application.kitchen.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;

public record UpdateKitchenCommand(
        Long id,
        String name
) {
    public UpdateKitchenCommand {
        ValidationException.requirePositive(
                id,
                "The kitchen id must be a positive number."
        );
        name = ValidationException.requireNonBlank(
                name,
                "The kitchen name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new ValidationException("The kitchen name must be at least 2 characters long.");
        }
    }
}
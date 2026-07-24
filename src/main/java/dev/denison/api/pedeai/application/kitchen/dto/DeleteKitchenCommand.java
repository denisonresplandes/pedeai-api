package dev.denison.api.pedeai.application.kitchen.dto;

import dev.denison.api.pedeai.application.shared.ValidationException;

public record DeleteKitchenCommand(
        Long id
) {
    public DeleteKitchenCommand {
        ValidationException.requirePositive(
                id,
                "Kitchen ID must be a positive number."
        );
    }
}

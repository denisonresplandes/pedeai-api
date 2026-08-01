package dev.denison.api.pedeai.core.application.restaurant.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;

public record DeleteRestaurantCommand(
        Long id
) {
    public DeleteRestaurantCommand {
        ValidationException.requirePositive(
                id,
                "Restaurant ID must be a positive number."
        );
    }
}

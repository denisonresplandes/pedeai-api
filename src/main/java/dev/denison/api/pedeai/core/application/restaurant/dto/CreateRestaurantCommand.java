package dev.denison.api.pedeai.core.application.restaurant.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;

public record CreateRestaurantCommand(
        String name
) {
    public CreateRestaurantCommand {
        name = ValidationException.requireNonBlank(
                name,
                "Restaurant name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new ValidationException("restaurant name must be at least 2 characters long.");
        }
    }
}

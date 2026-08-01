package dev.denison.api.pedeai.core.application.restaurant.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;

public record UpdateRestaurantCommand(
        Long id,
        String name
) {
    public UpdateRestaurantCommand {
        ValidationException.requirePositive(
                id,
                "The restaurant id must be a positive number."
        );
        name = ValidationException.requireNonBlank(
                name,
                "The restaurant name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new ValidationException("The restaurant name must be at least 2 characters long.");
        }
    }
}

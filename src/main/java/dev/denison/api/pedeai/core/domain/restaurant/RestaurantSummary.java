package dev.denison.api.pedeai.core.domain.restaurant;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record RestaurantSummary(
        Long id,
        String name
) {
    public RestaurantSummary {
        DomainException.requirePositive(
                id,
                "Restaurant ID must be a positive number."
        );
        name = DomainException.requireNonBlank(
                name,
                "Restaurant name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new DomainException("Restaurant name must be at least 2 characters long.");
        }
    }
}

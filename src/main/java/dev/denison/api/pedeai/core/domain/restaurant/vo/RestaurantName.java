package dev.denison.api.pedeai.core.domain.restaurant.vo;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record RestaurantName(
        String value
) {
    public RestaurantName {
        value = DomainException.requireNonBlank(
                value,
                "Restaurant name cannot be empty or blank."
        );
    }
}

package dev.denison.api.pedeai.core.domain.restaurant.vo;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record RestaurantId(
        Long value
) {
    public RestaurantId {
        DomainException.requirePositive(
                value,
                "Restaurant ID must be a positive number."
        );
    }
}

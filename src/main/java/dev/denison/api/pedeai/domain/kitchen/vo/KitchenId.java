package dev.denison.api.pedeai.domain.kitchen.vo;

import dev.denison.api.pedeai.domain.shared.DomainException;

public record KitchenId(
        Long value
) {
    public KitchenId {
        DomainException.requirePositive(
                value,
                "Kitchen ID must be a positive number."
        );
    }
}
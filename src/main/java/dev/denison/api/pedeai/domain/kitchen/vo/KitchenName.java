package dev.denison.api.pedeai.domain.kitchen.vo;

import dev.denison.api.pedeai.domain.shared.DomainException;

public record KitchenName(
        String value
) {
    public KitchenName {
        value = DomainException.requireNonBlank(
                value,
                "Kitchen name cannot be empty or blank."
        );
    }
}
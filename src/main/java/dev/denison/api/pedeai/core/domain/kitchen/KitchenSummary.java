package dev.denison.api.pedeai.core.domain.kitchen;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record KitchenSummary(
        Long id,
        String name
) {
    public KitchenSummary {
        DomainException.requirePositive(
                id,
                "Kitchen ID must be a positive number."
        );
        name = DomainException.requireNonBlank(
                name,
                "Kitchen name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new DomainException("Kitchen name must be at least 2 characters long.");
        }
    }
}

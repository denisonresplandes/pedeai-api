package dev.denison.api.pedeai.core.domain.restaurant;

import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.shared.DomainException;

import java.util.Set;

public record RestaurantSummary(
        Long id,
        String name,
        Long kitchenId,
        Set<PaymentMethod> paymentMethods
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
        DomainException.requirePositive(
                kitchenId,
                "Kitchen id must be a positive number."
        );
    }
}

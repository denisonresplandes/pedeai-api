package dev.denison.api.pedeai.core.application.restaurant.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;

import java.util.Set;

public record CreateRestaurantCommand(
        String name,
        Long kitchenId,
        Set<PaymentMethod> paymentMethods
) {
    public CreateRestaurantCommand {
        name = ValidationException.requireNonBlank(
                name,
                "Restaurant name cannot be empty or blank."
        );
        if (name.length() < 2) {
            throw new ValidationException("restaurant name must be at least 2 characters long.");
        }
        ValidationException.requirePositive(
                kitchenId,
                "Kitchen id must be greater than zero."
        );
        paymentMethods = paymentMethods == null ? Set.of() : paymentMethods;
    }
}

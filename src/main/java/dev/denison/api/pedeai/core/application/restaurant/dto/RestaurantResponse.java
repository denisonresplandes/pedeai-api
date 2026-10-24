package dev.denison.api.pedeai.core.application.restaurant.dto;

import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;

import java.util.Set;

public record RestaurantResponse(
        Long id,
        String name,
        Long kitchenId,
        Set<PaymentMethod> paymentMethods
) {
}

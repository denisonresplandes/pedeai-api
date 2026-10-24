package dev.denison.api.pedeai.presentation.restaurant.dto;

import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateRestaurantRequest(
        @NotBlank(message = "{restaurant.name.required}")
        @Size(min = 2, message = "{restaurant.name.size}")
        String name,

        @NotNull(message = "{kitchen.id.require}")
        @Positive(message = "{kitchen.id.positive}")
        Long kitchenId,

        Set<PaymentMethod> paymentMethods
) {
    public CreateRestaurantCommand toCommand() {
        return new CreateRestaurantCommand(name, kitchenId, paymentMethods);
    }
}

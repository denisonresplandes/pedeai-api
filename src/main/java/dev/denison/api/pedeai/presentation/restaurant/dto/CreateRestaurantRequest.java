package dev.denison.api.pedeai.presentation.restaurant.dto;

import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(
        @NotBlank(message = "{restaurant.name.required}")
        @Size(min = 2, message = "{restaurant.name.size}")
        String name,

        @NotNull(message = "{kitchen.id.require}")
        @Positive(message = "{kitchen.id.positive}")
        Long kitchenId
) {
    public CreateRestaurantCommand toCommand() {
        return new CreateRestaurantCommand(name, kitchenId);
    }
}

package dev.denison.api.pedeai.presentation.restaurant.dto;

import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(
        @NotBlank(message = "{restaurant.name.required}")
        @Size(min = 2, message = "{restaurant.name.size}")
        String name
) {
    public CreateRestaurantCommand toCommand() {
        return new CreateRestaurantCommand(name);
    }
}

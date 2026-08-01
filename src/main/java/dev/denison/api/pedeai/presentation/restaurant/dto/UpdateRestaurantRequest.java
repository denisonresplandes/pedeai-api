package dev.denison.api.pedeai.presentation.restaurant.dto;

import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(
        @NotBlank(message = "{restaurant.name.required}")
        @Size(min = 2, message = "{restaurant.name.size}")
        String name
) {
    public UpdateRestaurantCommand toCommand(Long id) {
        return new UpdateRestaurantCommand(id, name);
    }
}

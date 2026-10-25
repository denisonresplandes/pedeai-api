package dev.denison.api.pedeai.core.application.restaurant.dto;

public record UpdateRestaurantCommand(
        Long id,
        String name
) {
}

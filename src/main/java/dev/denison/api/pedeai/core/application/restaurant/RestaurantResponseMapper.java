package dev.denison.api.pedeai.core.application.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantSummary;

public final class RestaurantResponseMapper {

    private RestaurantResponseMapper() {}

    public static RestaurantResponse toResponse(RestaurantSummary summary) {
        return new RestaurantResponse(
                summary.id(),
                summary.name(),
                summary.kitchenId()
        );
    }

    public static RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId().value(),
                restaurant.getName().value(),
                restaurant.getKitchenId().value()
        );
    }
}

package dev.denison.api.pedeai.core.application.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;

public final class RestaurantResponseMapper {

    private RestaurantResponseMapper() {}

    public static RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId().value(),
                restaurant.getName().value(),
                restaurant.getKitchenId().value(),
                restaurant.getPaymentMethods()
        );
    }
}

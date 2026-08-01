package dev.denison.api.pedeai.core.domain.restaurant;

import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;

import java.util.Optional;

public interface RestaurantRepository {

    Restaurant save(Restaurant restaurant);

    Optional<Restaurant> findById(RestaurantId id);

    void delete(Restaurant restaurant);

    boolean existsByName(RestaurantName name);
}

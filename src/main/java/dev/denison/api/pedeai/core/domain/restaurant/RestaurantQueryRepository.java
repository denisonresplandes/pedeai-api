package dev.denison.api.pedeai.core.domain.restaurant;

import java.util.List;
import java.util.Optional;

public interface RestaurantQueryRepository {

    Optional<RestaurantSummary> findById(Long id);

    Optional<RestaurantSummary> findByName(String name);

    List<RestaurantSummary> findAll();
}

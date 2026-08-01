package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.domain.restaurant.RestaurantSummary;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface SpringDataRestaurantQueryRepository extends Repository<RestaurantEntity, Long> {

    List<RestaurantSummary> findBy();

    Optional<RestaurantSummary> findSummaryById(Long id);

    Optional<RestaurantSummary> findSummaryByNameIgnoreCase(String name);

}

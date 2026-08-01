package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.domain.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantSummary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RestaurantQueryRepositoryAdapter implements RestaurantQueryRepository {

    private final SpringDataRestaurantQueryRepository queryRepository;

    public RestaurantQueryRepositoryAdapter(SpringDataRestaurantQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Override
    public Optional<RestaurantSummary> findById(Long id) {
        return queryRepository.findSummaryById(id);
    }

    @Override
    public Optional<RestaurantSummary> findByName(String name) {
        return queryRepository.findSummaryByNameIgnoreCase(name);
    }

    @Override
    public List<RestaurantSummary> findAll() {
        return queryRepository.findBy();
    }
}

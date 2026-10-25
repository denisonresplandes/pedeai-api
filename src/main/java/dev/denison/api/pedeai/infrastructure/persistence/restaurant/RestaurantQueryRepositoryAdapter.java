package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
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
    public Optional<RestaurantResponse> findById(Long id) {
        return queryRepository.findById(id).map(RestaurantMapper::toResponse);
    }

    @Override
    public Optional<RestaurantResponse> findByName(String name) {
        return queryRepository.findByNameIgnoreCase(name).map(RestaurantMapper::toResponse);
    }

    @Override
    public boolean existsByKitchenId(KitchenId kitchenId) {
        return queryRepository.existsByKitchenId(kitchenId.value());
    }

    @Override
    public List<RestaurantResponse> findAll() {
        return queryRepository.findAll().stream()
                .map(RestaurantMapper::toResponse)
                .toList();
    }
}

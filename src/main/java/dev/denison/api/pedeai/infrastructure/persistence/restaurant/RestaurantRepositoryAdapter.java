package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RestaurantRepositoryAdapter implements RestaurantRepository {

    private final SpringDataRestaurantRepository repository;

    public RestaurantRepositoryAdapter(SpringDataRestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        RestaurantEntity entityToSave = RestaurantMapper.toEntity(restaurant);
        RestaurantEntity savedEntity = repository.save(entityToSave);
        return RestaurantMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Restaurant> findById(RestaurantId id) {
        return repository.findById(id.value()).map(RestaurantMapper::toDomain);
    }

    @Override
    public void delete(Restaurant restaurant) {
        repository.delete(RestaurantMapper.toEntity(restaurant));
    }

    @Override
    public boolean existsByName(RestaurantName name) {
        return repository.existsByName(name.value());
    }
}

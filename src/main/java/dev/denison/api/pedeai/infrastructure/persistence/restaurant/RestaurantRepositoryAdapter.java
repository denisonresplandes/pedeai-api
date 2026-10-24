package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
public class RestaurantRepositoryAdapter implements RestaurantRepository {

    private final SpringDataRestaurantRepository repository;

    public RestaurantRepositoryAdapter(SpringDataRestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Restaurant save(Restaurant restaurant) {
        if (Objects.isNull(restaurant.getId())) {
            RestaurantEntity savedEntity = repository.save(RestaurantMapper.toEntity(restaurant));
            return RestaurantMapper.toDomain(savedEntity);
        }
        // Entidade gerenciada: o dirty checking gera o UPDATE no commit, sem o SELECT extra do merge.
        Long id = restaurant.getId().value();
        RestaurantEntity entity = repository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));
        RestaurantMapper.updateEntity(entity, restaurant);
        return RestaurantMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Restaurant> findById(RestaurantId id) {
        return repository.findById(id.value()).map(RestaurantMapper::toDomain);
    }

    @Override
    public boolean existsById(RestaurantId id) {
        return repository.existsById(id.value());
    }

    @Override
    public void deleteById(RestaurantId id) {
        repository.deleteById(id.value());
    }

    @Override
    public boolean existsByName(RestaurantName name) {
        return repository.existsByNameIgnoreCase(name.value());
    }
}

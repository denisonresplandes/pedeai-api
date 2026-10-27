package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
public class RestaurantRepositoryAdapter implements RestaurantRepository {

    private final SpringDataRestaurantRepository repository;

    public RestaurantRepositoryAdapter(SpringDataRestaurantRepository repository) {
        this.repository = repository;
    }

    /*
     * MANDATORY: este método NÃO abre transação, ele EXIGE que o chamador já tenha aberto uma (quem abre
     * é o TransactionalRestaurantCommandHandlerDecorator). Sem transação, falha na hora com
     * IllegalTransactionStateException.
     *
     * Por que exigir? O update abaixo depende do dirty checking: a entidade carregada precisa continuar
     * "gerenciada" até o COMMIT, que é quando o Hibernate descobre o que mudou e gera o UPDATE. Fora de
     * uma transação o findById devolve a entidade já solta, a alteração seria feita nela e nunca chegaria
     * ao banco, sem erro nenhum. Melhor falhar alto do que perder o dado em silêncio.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
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

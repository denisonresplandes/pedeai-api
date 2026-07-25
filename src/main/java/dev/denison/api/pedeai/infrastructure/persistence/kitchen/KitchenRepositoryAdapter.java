package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class KitchenRepositoryAdapter implements KitchenRepository {

    private final SpringDataKitchenRepository repository;

    public KitchenRepositoryAdapter(SpringDataKitchenRepository repository) {
        this.repository = repository;
    }

    @Override
    public Kitchen save(Kitchen kitchen) {
        KitchenEntity entityToSave = KitchenMapper.toEntity(kitchen);
        KitchenEntity savedEntity = repository.save(entityToSave);
        return KitchenMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Kitchen> findById(KitchenId id) {
        return repository.findById(id.value())
                .map(KitchenMapper::toDomain);
    }

    @Override
    public void delete(Kitchen kitchen) {
        repository.delete(KitchenMapper.toEntity(kitchen));
    }

    @Override
    public boolean existsByName(KitchenName name) {
        return repository.existsByName(name.value());
    }
}

package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;
import org.springframework.stereotype.Repository;

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
    public void deleteById(KitchenId id) {
        repository.deleteById(id.value());
    }

    @Override
    public boolean existsById(KitchenId id) {
        return repository.existsById(id.value());
    }

    @Override
    public boolean existsByName(KitchenName name) {
        return repository.existsByName(name.value());
    }
}

package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class KitchenQueryRepositoryAdapter implements KitchenQueryRepository {

    private final SpringDataKitchenQueryRepository queryRepository;

    public KitchenQueryRepositoryAdapter(SpringDataKitchenQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @Override
    public Optional<KitchenResponse> findById(Long id) {
        return queryRepository.findResponseById(id);
    }

    @Override
    public Optional<KitchenResponse> findByName(String name) {
        return queryRepository.findResponseByNameIgnoreCase(name);
    }

    @Override
    public List<KitchenResponse> findAll() {
        return queryRepository.findBy();
    }

    @Override
    public boolean existsById(KitchenId id) {
        return queryRepository.existsKitchenById(id.value());
    }
}
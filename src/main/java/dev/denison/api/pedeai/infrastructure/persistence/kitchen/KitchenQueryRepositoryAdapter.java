package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.core.domain.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenSummary;
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
    public Optional<KitchenSummary> findById(Long id) {
        return queryRepository.findSummaryById(id);
    }

    @Override
    public Optional<KitchenSummary> findByName(String name) {
        return queryRepository.findSummaryByNameIgnoreCase(name);
    }

    @Override
    public List<KitchenSummary> findAll() {
        return queryRepository.findBy();
    }

    @Override
    public boolean existsById(KitchenId id) {
        return queryRepository.existsSummaryById(id.value());
    }
}
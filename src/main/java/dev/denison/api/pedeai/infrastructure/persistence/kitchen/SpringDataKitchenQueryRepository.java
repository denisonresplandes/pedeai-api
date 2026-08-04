package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.core.domain.kitchen.KitchenSummary;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface SpringDataKitchenQueryRepository extends Repository<KitchenEntity, Long> {

    // Não especifica nenhuma propriedade depois de By. Equivale ao findAll.
    List<KitchenSummary> findBy();

    Optional<KitchenSummary> findSummaryById(Long id);

    Optional<KitchenSummary> findSummaryByNameIgnoreCase(String name);

    boolean existsSummaryById(Long id);
}

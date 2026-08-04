package dev.denison.api.pedeai.core.domain.kitchen;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;

import java.util.List;
import java.util.Optional;

public interface KitchenQueryRepository {

    Optional<KitchenSummary> findById(Long id);

    Optional<KitchenSummary> findByName(String name);

    List<KitchenSummary> findAll();

    boolean existsById(KitchenId id);
}
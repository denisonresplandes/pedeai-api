package dev.denison.api.pedeai.domain.kitchen;

import java.util.List;
import java.util.Optional;

public interface KitchenQueryRepository {

    Optional<KitchenSummary> findById(Long id);

    Optional<KitchenSummary> findByName(String name);

    List<KitchenSummary> findAll();
}
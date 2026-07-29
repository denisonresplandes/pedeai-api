package dev.denison.api.pedeai.core.domain.kitchen;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;

import java.util.Optional;

public interface KitchenRepository {

    Kitchen save(Kitchen kitchen);

    Optional<Kitchen> findById(KitchenId id);

    void delete(Kitchen kitchen);

    boolean existsByName(KitchenName name);
}

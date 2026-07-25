package dev.denison.api.pedeai.domain.kitchen;

import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;

import java.util.Optional;

public interface KitchenRepository {

    Kitchen save(Kitchen kitchen);

    Optional<Kitchen> findById(KitchenId id);

    void deleteById(KitchenId id);

    boolean existsByName(KitchenName name);

    boolean existsById(KitchenId id);
}

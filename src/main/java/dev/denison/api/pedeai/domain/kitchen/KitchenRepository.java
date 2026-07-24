package dev.denison.api.pedeai.domain.kitchen;

import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;

public interface KitchenRepository {

    Kitchen save(Kitchen kitchen);

    void deleteById(KitchenId id);

    boolean existsByName(KitchenName name);

    boolean existsById(KitchenId id);
}

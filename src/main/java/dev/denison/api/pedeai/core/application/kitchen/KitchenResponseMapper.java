package dev.denison.api.pedeai.core.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.domain.kitchen.Kitchen;

public final class KitchenResponseMapper {

    private KitchenResponseMapper() {}

    public static KitchenResponse toResponse(Kitchen kitchen) {
        return new KitchenResponse(
                kitchen.getId().value(),
                kitchen.getName().value()
        );
    }
}

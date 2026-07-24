package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.domain.kitchen.KitchenSummary;

public final class KitchenResponseMapper {

    private KitchenResponseMapper() {}

    public static KitchenResponse toResponse(KitchenSummary summary) {
        return new KitchenResponse(
                summary.id(),
                summary.name()
        );
    }

    public static KitchenResponse toResponse(Kitchen kitchen) {
        return new KitchenResponse(
                kitchen.getId().value(),
                kitchen.getName().value()
        );
    }
}

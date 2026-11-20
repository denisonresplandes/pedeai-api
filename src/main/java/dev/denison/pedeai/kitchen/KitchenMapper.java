package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.kitchen.dto.KitchenResponse;

final class KitchenMapper {

    private KitchenMapper() {}

    static KitchenResponse toResponse(Kitchen kitchen) {
        return new KitchenResponse(kitchen.getId(), kitchen.getName());
    }

}

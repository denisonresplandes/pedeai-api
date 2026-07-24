package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.domain.kitchen.Kitchen;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;

import java.util.Objects;

public final class KitchenMapper {

    private KitchenMapper() {}

    public static Kitchen toDomain(KitchenEntity entity) {
        return Kitchen.restore(
                new KitchenId(entity.getId()),
                new KitchenName(entity.getName())
        );
    }

    public static KitchenEntity toEntity(Kitchen kitchen) {
        /*
        * Novo registro se o ID for nulo. Executa um INSERT.
        * Atualização se o ID não for nulo. Executa um UPDATE.
        * */
        Long id = Objects.nonNull(kitchen.getId()) ? kitchen.getId().value() : null;
        return new KitchenEntity(
                id,
                kitchen.getName().value()
        );
    }
}

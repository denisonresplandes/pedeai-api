package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;

import java.util.Objects;

public final class RestaurantMapper {

    private RestaurantMapper() {}

    public static Restaurant toDomain(RestaurantEntity entity) {
        return Restaurant.restore(
                new RestaurantId(entity.getId()),
                new RestaurantName(entity.getName())
        );
    }

    public static RestaurantEntity toEntity(Restaurant restaurant) {
        /*
         * Novo registro se o ID for nulo. Executa um INSERT.
         * Atualização se o ID não for nulo. Executa um UPDATE.
         * */
        Long id = Objects.nonNull(restaurant.getId()) ? restaurant.getId().value() : null;
        return new RestaurantEntity(
                id,
                restaurant.getName().value()
        );
    }
}

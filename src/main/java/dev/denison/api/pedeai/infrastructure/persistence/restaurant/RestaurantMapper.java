package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantSummary;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.infrastructure.persistence.kitchen.KitchenEntity;

import java.util.Objects;
import java.util.Set;

public final class RestaurantMapper {

    private RestaurantMapper() {}

    public static Restaurant toDomain(RestaurantEntity entity) {
        return Restaurant.restore(
                new RestaurantId(entity.getId()),
                new RestaurantName(entity.getName()),
                new KitchenId(entity.getKitchen().getId()),
                entity.getPaymentMethods()
        );
    }

    public static RestaurantSummary toSummary(RestaurantEntity entity) {
        return new RestaurantSummary(
                entity.getId(),
                entity.getName(),
                entity.getKitchen().getId(),
                Set.copyOf(entity.getPaymentMethods())
        );
    }

    public static void updateEntity(RestaurantEntity entity, Restaurant restaurant) {
        entity.setName(restaurant.getName().value());
        Long kitchenId = restaurant.getKitchenId().value();
        if (!Objects.equals(entity.getKitchen().getId(), kitchenId)) {
            entity.setKitchen(new KitchenEntity(kitchenId));
        }
        Set<PaymentMethod> desired = restaurant.getPaymentMethods();
        Set.copyOf(entity.getPaymentMethods()).stream()
                .filter(method -> !desired.contains(method))
                .forEach(entity::removePaymentMethod);
        desired.forEach(entity::addPaymentMethod);
    }

    public static RestaurantEntity toEntity(Restaurant restaurant) {
        /*
         * Novo registro se o ID for nulo. Executa um INSERT.
         * Atualização se o ID não for nulo. Executa um UPDATE.
         * */
        KitchenEntity kitchenEntity = new KitchenEntity(restaurant.getKitchenId().value());
        Long id = Objects.nonNull(restaurant.getId()) ? restaurant.getId().value() : null;
        RestaurantEntity entity = new RestaurantEntity(
                id,
                restaurant.getName().value(),
                kitchenEntity
        );
        restaurant.getPaymentMethods().forEach(entity::addPaymentMethod);
        return entity;
    }
}

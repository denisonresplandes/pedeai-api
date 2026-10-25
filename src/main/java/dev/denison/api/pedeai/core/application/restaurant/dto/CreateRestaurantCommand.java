package dev.denison.api.pedeai.core.application.restaurant.dto;

import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;

import java.util.Set;

/*
 * Sem validação (veja CreateKitchenCommand): nome e kitchenId são validados pelo Request e pelos
 * VOs RestaurantName/KitchenId.
 */
public record CreateRestaurantCommand(
        String name,
        Long kitchenId,
        Set<PaymentMethod> paymentMethods
) {
    public CreateRestaurantCommand {
        // Isto NÃO é validação, é um valor padrão: o JSON sem "paymentMethods" chega como null e
        // significa "nenhum além do CASH" (que o Restaurant.create acrescenta). Sem isto, o
        // Restaurant.create rejeitaria o null e a criação sem métodos viraria erro 400.
        paymentMethods = paymentMethods == null ? Set.of() : paymentMethods;
    }
}

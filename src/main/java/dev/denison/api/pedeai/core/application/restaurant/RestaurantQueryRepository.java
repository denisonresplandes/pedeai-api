package dev.denison.api.pedeai.core.application.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;

import java.util.List;
import java.util.Optional;

/*
 * Porta de LEITURA (lado Query do CQRS), na aplicação pelo mesmo motivo de KitchenQueryRepository:
 * devolve o modelo de leitura (RestaurantResponse) e o domínio não a usa.
 */
public interface RestaurantQueryRepository {

    Optional<RestaurantResponse> findById(Long id);

    Optional<RestaurantResponse> findByName(String name);

    List<RestaurantResponse> findAll();

    boolean existsByKitchenId(KitchenId kitchenId);
}

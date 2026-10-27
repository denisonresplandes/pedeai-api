package dev.denison.api.pedeai.core.application.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;

// Contrato dos casos de uso de ESCRITA de Restaurant. O porquê da interface está no KitchenCommandHandler.
public interface RestaurantCommandHandler {

    RestaurantResponse create(CreateRestaurantCommand command);

    RestaurantResponse update(UpdateRestaurantCommand command);

    void delete(Long id);
}

package dev.denison.api.pedeai.core.application.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;

import java.util.List;

public class RestaurantQueryHandler {

    private final RestaurantQueryRepository queryRepository;

    public RestaurantQueryHandler(RestaurantQueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    public RestaurantResponse findById(Long id) {
        return queryRepository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));
    }

    // Devolve lista (0 ou 1 elemento): veja KitchenQueryHandler.findByName.
    public List<RestaurantResponse> findByName(String name) {
        return queryRepository.findByName(name)
                .map(List::of)
                .orElseGet(List::of);
    }

    public List<RestaurantResponse> findAll() {
        return queryRepository.findAll();
    }
}

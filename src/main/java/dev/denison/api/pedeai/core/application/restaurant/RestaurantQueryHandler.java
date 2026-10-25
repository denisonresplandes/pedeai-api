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

    public RestaurantResponse findByName(String name) {
        return queryRepository.findByName(name)
                .orElseThrow(() -> new RestaurantNotFoundException(name));
    }

    public List<RestaurantResponse> findAll() {
        return queryRepository.findAll();
    }
}

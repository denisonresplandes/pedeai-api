package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryHandler;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestaurantConfig {

    @Bean
    RestaurantQueryHandler restaurantQueryHandler(RestaurantQueryRepository queryRepository) {
        return new RestaurantQueryHandler(queryRepository);
    }

    @Bean
    RestaurantCommandHandler restaurantCommandHandler(RestaurantRepository repository) {
        return new RestaurantCommandHandler(repository);
    }
}

package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KitchenConfig {

    @Bean
    KitchenQueryHandler kitchenQueryHandler(KitchenQueryRepository queryRepository) {
        return new KitchenQueryHandler(queryRepository);
    }

    @Bean
    KitchenCommandHandler kitchenCommandHandler(KitchenRepository repository,
                                               RestaurantQueryRepository restaurantQueryRepository) {
        return new KitchenCommandHandler(repository, restaurantQueryRepository);
    }
}

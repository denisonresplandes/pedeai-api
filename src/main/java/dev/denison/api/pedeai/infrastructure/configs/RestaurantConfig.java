package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.restaurant.DefaultRestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryHandler;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.infrastructure.transaction.TransactionalRestaurantCommandHandlerDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class RestaurantConfig {

    @Bean
    RestaurantQueryHandler restaurantQueryHandler(RestaurantQueryRepository queryRepository) {
        return new RestaurantQueryHandler(queryRepository);
    }

    // Só o decorator vira bean; o porquê está em KitchenConfig.kitchenCommandHandler.
    @Bean
    RestaurantCommandHandler restaurantCommandHandler(RestaurantRepository repository,
                                                      KitchenQueryRepository kitchenQueryRepository,
                                                      TransactionTemplate transactionTemplate) {
        RestaurantCommandHandler handler = new DefaultRestaurantCommandHandler(repository, kitchenQueryRepository);
        return new TransactionalRestaurantCommandHandlerDecorator(handler, transactionTemplate);
    }
}

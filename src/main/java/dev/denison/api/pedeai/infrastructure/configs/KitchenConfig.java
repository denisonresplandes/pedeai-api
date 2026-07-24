package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.domain.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.domain.kitchen.KitchenRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KitchenConfig {

    @Bean
    KitchenQueryHandler queryHandler(KitchenQueryRepository queryRepository) {
        return new KitchenQueryHandler(queryRepository);
    }

    @Bean
    KitchenCommandHandler commandHandler(KitchenRepository repository) {
        return new KitchenCommandHandler(repository);
    }
}

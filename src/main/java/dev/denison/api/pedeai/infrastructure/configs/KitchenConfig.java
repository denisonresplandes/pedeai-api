package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.core.application.kitchen.DefaultKitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.domain.kitchen.KitchenRepository;
import dev.denison.api.pedeai.infrastructure.transaction.TransactionalKitchenCommandHandlerDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class KitchenConfig {

    // Consulta: sem transação em volta (cada consulta é uma chamada só e já devolve o Response pronto).
    @Bean
    KitchenQueryHandler kitchenQueryHandler(KitchenQueryRepository queryRepository) {
        return new KitchenQueryHandler(queryRepository);
    }

    /*
     * Escrita: o bean exposto é o DECORATOR, que embrulha o handler de verdade numa transação.
     * O handler (Default...) é criado aqui dentro e não vira bean, de propósito: se virasse, haveria
     * dois beans do tipo KitchenCommandHandler e o controller poderia receber o handler "pelado",
     * sem transação. O TransactionTemplate já é fornecido pelo Spring Boot.
     */
    @Bean
    KitchenCommandHandler kitchenCommandHandler(KitchenRepository repository,
                                                RestaurantQueryRepository restaurantQueryRepository,
                                                TransactionTemplate transactionTemplate) {
        KitchenCommandHandler handler = new DefaultKitchenCommandHandler(repository, restaurantQueryRepository);
        return new TransactionalKitchenCommandHandlerDecorator(handler, transactionTemplate);
    }
}

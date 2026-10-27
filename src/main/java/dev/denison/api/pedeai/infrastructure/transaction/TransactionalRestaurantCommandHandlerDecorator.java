package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * Abre a transação em volta de cada caso de uso de escrita de Restaurant. O modelo e o raciocínio
 * estão em TransactionalKitchenCommandHandlerDecorator.
 *
 * Aqui o ganho é maior: o update carregava o restaurante e seus métodos de pagamento duas vezes
 * (6 instruções SQL). Com uma transação só em volta do caso de uso, a segunda carga sai da cache
 * de 1º nível e o PUT executa 4 instruções (veja TransactionBoundaryTest).
 */
public class TransactionalRestaurantCommandHandlerDecorator implements RestaurantCommandHandler {

    private final RestaurantCommandHandler delegate;
    private final TransactionTemplate transactionTemplate;

    public TransactionalRestaurantCommandHandlerDecorator(RestaurantCommandHandler delegate,
                                                          TransactionTemplate transactionTemplate) {
        this.delegate = delegate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public RestaurantResponse create(CreateRestaurantCommand command) {
        return transactionTemplate.execute(status -> delegate.create(command));
    }

    @Override
    public RestaurantResponse update(UpdateRestaurantCommand command) {
        return transactionTemplate.execute(status -> delegate.update(command));
    }

    @Override
    public void delete(Long id) {
        transactionTemplate.executeWithoutResult(status -> delegate.delete(id));
    }
}

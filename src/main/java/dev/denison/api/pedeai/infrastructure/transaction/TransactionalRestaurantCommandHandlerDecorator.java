package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * PADRÃO DECORATOR: mesma interface do handler (RestaurantCommandHandler) e uma referência para o handler
 * de verdade (DefaultRestaurantCommandHandler). Cada método abre a transação, repassa a chamada e termina
 * com COMMIT (ou ROLLBACK se o handler lançar exceção). O modelo completo e o raciocínio estão em
 * TransactionalKitchenCommandHandlerDecorator.
 */
public class TransactionalRestaurantCommandHandlerDecorator implements RestaurantCommandHandler {

    private final RestaurantCommandHandler delegate;
    private final TransactionTemplate transactionTemplate;

    public TransactionalRestaurantCommandHandlerDecorator(RestaurantCommandHandler delegate,
                                                          TransactionTemplate transactionTemplate) {
        this.delegate = delegate;
        this.transactionTemplate = transactionTemplate;
    }

    /*
     * TRANSAÇÃO: checar o nome, conferir se a cozinha existe e gravar acontecem na mesma transação. Se
     * qualquer passo lançar exceção, nada fica gravado (ROLLBACK).
     */
    @Override
    public RestaurantResponse create(CreateRestaurantCommand command) {
        return transactionTemplate.execute(status -> delegate.create(command));
    }

    /*
     * TRANSAÇÃO: aqui o ganho é maior. O update carregava o restaurante e seus métodos de pagamento duas
     * vezes (6 instruções SQL). Com uma transação só em volta do caso de uso, a segunda carga sai da cache
     * de 1º nível e o PUT executa 4 instruções (veja TransactionBoundaryTest).
     * O save do RestaurantRepositoryAdapter EXIGE esta transação (Propagation.MANDATORY).
     */
    @Override
    public RestaurantResponse update(UpdateRestaurantCommand command) {
        return transactionTemplate.execute(status -> delegate.update(command));
    }

    /*
     * TRANSAÇÃO: conferir se o restaurante existe e excluir acontecem na mesma transação. Sem retorno:
     * executeWithoutResult no lugar de execute.
     */
    @Override
    public void delete(Long id) {
        transactionTemplate.executeWithoutResult(status -> delegate.delete(id));
    }
}

package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * DECORATOR: tem a mesma interface do handler e "embrulha" cada chamada numa transação.
 *
 *   Controller --update()--> [ este decorator ] --update()--> DefaultKitchenCommandHandler
 *                              BEGIN                             (regras de negócio)
 *                              ...chama o handler...
 *                              COMMIT (ou ROLLBACK)
 *
 * Isto faz o mesmo que um @Transactional faria, só que escrito à mão, para dar para ver e depurar.
 * O TransactionTemplate cuida do resto:
 *   - o handler terminou normalmente  -> COMMIT;
 *   - o handler lançou RuntimeException (ou Error) -> ROLLBACK e a exceção continua subindo, intacta,
 *     até o GlobalExceptionHandler (todas as exceções do projeto são RuntimeException).
 *
 * Por que importa: a transação "mora" na thread. Enquanto ela está aberta, todos os repositórios usam o
 * mesmo EntityManager (a cache de 1º nível). O findById do handler e o do save passam a ser a mesma
 * entidade, sem carregar de novo, e o UPDATE sai uma vez só, no COMMIT (dirty checking).
 *
 * O handler não sabe que existe transação, e o core continua Java puro (sem @Transactional).
 * O KitchenConfig monta o conjunto; os outros decorators (Restaurant e Payment) seguem este modelo.
 */
public class TransactionalKitchenCommandHandlerDecorator implements KitchenCommandHandler {

    private final KitchenCommandHandler delegate;
    private final TransactionTemplate transactionTemplate;

    public TransactionalKitchenCommandHandlerDecorator(KitchenCommandHandler delegate,
                                                       TransactionTemplate transactionTemplate) {
        this.delegate = delegate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public KitchenResponse create(CreateKitchenCommand command) {
        return transactionTemplate.execute(status -> delegate.create(command));
    }

    @Override
    public KitchenResponse update(UpdateKitchenCommand command) {
        return transactionTemplate.execute(status -> delegate.update(command));
    }

    @Override
    public void delete(Long id) {
        // Sem retorno: executeWithoutResult no lugar de execute.
        transactionTemplate.executeWithoutResult(status -> delegate.delete(id));
    }
}

package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * PADRÃO DECORATOR: esta classe tem a mesma interface do handler (KitchenCommandHandler) e guarda uma
 * referência para o handler de verdade (o "delegate"). Cada método acrescenta um comportamento (a transação)
 * antes e depois de repassar a chamada, sem alterar o handler.
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

    /*
     * TRANSAÇÃO: checar se o nome já existe e gravar acontecem na mesma transação. Se qualquer passo
     * lançar exceção (nome duplicado, erro do banco), nada fica gravado (ROLLBACK).
     */
    @Override
    public KitchenResponse create(CreateKitchenCommand command) {
        return transactionTemplate.execute(status -> delegate.create(command));
    }

    /*
     * TRANSAÇÃO: buscar a cozinha, checar o nome e gravar compartilham o mesmo EntityManager (cache de
     * 1º nível). O findById do handler e o do save são a mesma entidade, e o UPDATE sai uma vez só, no
     * COMMIT. Medido: 3 instruções SQL (TransactionBoundaryTest).
     */
    @Override
    public KitchenResponse update(UpdateKitchenCommand command) {
        return transactionTemplate.execute(status -> delegate.update(command));
    }

    /*
     * TRANSAÇÃO: buscar a cozinha, conferir se há restaurantes vinculados e excluir: ou tudo acontece,
     * ou nada. Sem retorno: executeWithoutResult no lugar de execute.
     */
    @Override
    public void delete(Long id) {
        transactionTemplate.executeWithoutResult(status -> delegate.delete(id));
    }
}

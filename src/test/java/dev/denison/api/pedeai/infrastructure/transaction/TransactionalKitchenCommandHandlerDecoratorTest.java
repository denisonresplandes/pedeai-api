package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.CreateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.dto.UpdateKitchenCommand;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNameAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * O decorator não tem regra de negócio: o que ele garante é a ORDEM begin -> handler -> commit
 * (ou begin -> handler -> rollback). Por isso o gerenciador de transação é um mock e o
 * TransactionTemplate é o de verdade: o que se verifica é o que o template pede ao gerenciador.
 * Os outros dois decorators têm testes iguais a estes (Restaurant e Payment).
 */
@ExtendWith(MockitoExtension.class)
class TransactionalKitchenCommandHandlerDecoratorTest {

    @Mock
    private KitchenCommandHandler delegate;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus status;

    private TransactionalKitchenCommandHandlerDecorator decorator;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        decorator = new TransactionalKitchenCommandHandlerDecorator(
                delegate, new TransactionTemplate(transactionManager));
    }

    @Test
    @DisplayName("create: chama o handler dentro da transação, faz commit e devolve a resposta dele.")
    void createRunsInsideTheTransactionAndCommits() {
        CreateKitchenCommand command = new CreateKitchenCommand("Indiana");
        KitchenResponse response = new KitchenResponse(1L, "Indiana");
        when(delegate.create(command)).thenReturn(response);

        assertSame(response, decorator.create(command));

        InOrder order = inOrder(transactionManager, delegate);
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(delegate).create(command);
        order.verify(transactionManager).commit(status);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    @DisplayName("update: chama o handler dentro da transação, faz commit e devolve a resposta dele.")
    void updateRunsInsideTheTransactionAndCommits() {
        UpdateKitchenCommand command = new UpdateKitchenCommand(1L, "Tailandesa");
        KitchenResponse response = new KitchenResponse(1L, "Tailandesa");
        when(delegate.update(command)).thenReturn(response);

        assertSame(response, decorator.update(command));

        InOrder order = inOrder(transactionManager, delegate);
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(delegate).update(command);
        order.verify(transactionManager).commit(status);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    @DisplayName("delete: chama o handler dentro da transação e faz commit.")
    void deleteRunsInsideTheTransactionAndCommits() {
        decorator.delete(1L);

        InOrder order = inOrder(transactionManager, delegate);
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(delegate).delete(1L);
        order.verify(transactionManager).commit(status);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    @DisplayName("Quando o handler lança uma exceção: faz rollback, não faz commit e a mesma exceção sobe.")
    void rollsBackAndRethrowsWhenTheHandlerFails() {
        UpdateKitchenCommand command = new UpdateKitchenCommand(1L, "Indiana");
        KitchenNameAlreadyExistsException failure = new KitchenNameAlreadyExistsException("Indiana");
        doThrow(failure).when(delegate).update(command);

        // A mesma instância: o decorator não embrulha nem traduz a exceção, então o
        // GlobalExceptionHandler continua recebendo a exceção de negócio original (409).
        assertSame(failure, assertThrows(KitchenNameAlreadyExistsException.class,
                () -> decorator.update(command)));

        verify(transactionManager).rollback(status);
        verify(transactionManager, never()).commit(any());
    }
}

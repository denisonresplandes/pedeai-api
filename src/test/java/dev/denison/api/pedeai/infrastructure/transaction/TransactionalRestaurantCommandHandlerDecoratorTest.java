package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.CreateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.dto.UpdateRestaurantCommand;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Veja TransactionalKitchenCommandHandlerDecoratorTest para o raciocínio destes testes.
@ExtendWith(MockitoExtension.class)
class TransactionalRestaurantCommandHandlerDecoratorTest {

    @Mock
    private RestaurantCommandHandler delegate;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus status;

    private TransactionalRestaurantCommandHandlerDecorator decorator;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        decorator = new TransactionalRestaurantCommandHandlerDecorator(
                delegate, new TransactionTemplate(transactionManager));
    }

    @Test
    @DisplayName("create: chama o handler dentro da transação, faz commit e devolve a resposta dele.")
    void createRunsInsideTheTransactionAndCommits() {
        CreateRestaurantCommand command = new CreateRestaurantCommand("Ilha Verde", 1L, Set.of(PaymentMethod.PIX));
        RestaurantResponse response = new RestaurantResponse(1L, "Ilha Verde", 1L, Set.of(PaymentMethod.CASH));
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
        UpdateRestaurantCommand command = new UpdateRestaurantCommand(1L, "Quero Mais");
        RestaurantResponse response = new RestaurantResponse(1L, "Quero Mais", 1L, Set.of(PaymentMethod.CASH));
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
        RestaurantNotFoundException failure = new RestaurantNotFoundException(99L);
        doThrow(failure).when(delegate).delete(99L);

        assertSame(failure, assertThrows(RestaurantNotFoundException.class, () -> decorator.delete(99L)));

        verify(transactionManager).rollback(status);
        verify(transactionManager, never()).commit(any());
    }
}

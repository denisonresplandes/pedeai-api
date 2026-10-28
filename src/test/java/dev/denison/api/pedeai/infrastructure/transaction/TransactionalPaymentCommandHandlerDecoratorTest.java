package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.exceptions.PaymentNotFoundException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.shared.Money;
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

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

// Veja TransactionalKitchenCommandHandlerDecoratorTest para o raciocínio destes testes.
@ExtendWith(MockitoExtension.class)
class TransactionalPaymentCommandHandlerDecoratorTest {

    @Mock
    private PaymentCommandHandler delegate;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus status;

    private TransactionalPaymentCommandHandlerDecorator decorator;

    @BeforeEach
    void setUp() {
        // lenient: o teste do processPayment não abre transação, então não usa este stub (de propósito).
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(status);
        decorator = new TransactionalPaymentCommandHandlerDecorator(
                delegate, new TransactionTemplate(transactionManager));
    }

    /*
     * De propósito SEM transação: o processPayment chama o gateway (rede), e uma chamada externa lenta
     * não pode segurar uma conexão do banco aberta. O handler grava o pagamento numa transação curta
     * (a do próprio save) e chama o gateway depois do commit. Veja TransactionBoundaryTest.
     */
    @Test
    @DisplayName("processPayment: chama o handler SEM abrir transação (o gateway não pode rodar dentro de uma).")
    void processPaymentDoesNotOpenATransaction() {
        ProcessPaymentCommand command = new ProcessPaymentCommand(new Money(new BigDecimal("10.50")), PaymentMethod.PIX);

        decorator.processPayment(command);

        verify(delegate).processPayment(command);
        verifyNoInteractions(transactionManager);
    }

    @Test
    @DisplayName("confirmPaymentFromWebhook: chama o handler dentro da transação e faz commit.")
    void confirmPaymentRunsInsideTheTransactionAndCommits() {
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(1L, true);

        decorator.confirmPaymentFromWebhook(command);

        InOrder order = inOrder(transactionManager, delegate);
        order.verify(transactionManager).getTransaction(any(TransactionDefinition.class));
        order.verify(delegate).confirmPaymentFromWebhook(command);
        order.verify(transactionManager).commit(status);
        verify(transactionManager, never()).rollback(any());
    }

    @Test
    @DisplayName("Quando o handler lança uma exceção: faz rollback, não faz commit e a mesma exceção sobe.")
    void rollsBackAndRethrowsWhenTheHandlerFails() {
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(99L, true);
        PaymentNotFoundException failure = new PaymentNotFoundException(99L);
        doThrow(failure).when(delegate).confirmPaymentFromWebhook(command);

        assertSame(failure, assertThrows(PaymentNotFoundException.class,
                () -> decorator.confirmPaymentFromWebhook(command)));

        verify(transactionManager).rollback(status);
        verify(transactionManager, never()).commit(any());
    }
}

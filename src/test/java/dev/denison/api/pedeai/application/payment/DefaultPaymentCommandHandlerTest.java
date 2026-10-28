package dev.denison.api.pedeai.application.payment;

import dev.denison.api.pedeai.core.application.payment.DefaultPaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.exceptions.PaymentNotFoundException;
import dev.denison.api.pedeai.core.application.payment.exceptions.UnsupportedPaymentMethodException;
import dev.denison.api.pedeai.core.application.shared.ResourceNotFoundException;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import dev.denison.api.pedeai.core.domain.payment.PaymentStatus;
import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import dev.denison.api.pedeai.core.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/*
 * Foco deste teste: a HIERARQUIA das exceções do módulo de pagamento.
 *
 * O GlobalExceptionHandler traduz exceções para HTTP pelas classes-base
 * (ResourceNotFoundException -> 404, ValidationException -> 400, ConflictException -> 409).
 * Uma exceção que estende RuntimeException direto não casa com nenhum handler e vira 500.
 * Verificar o tipo aqui garante que ninguém volte a quebrar esse contrato sem perceber.
 *
 * O handler é criado "na mão" (e não com @InjectMocks) porque recebe uma List<Strategy>,
 * algo que o Mockito não sabe preencher com mocks.
 */
@ExtendWith(MockitoExtension.class)
class DefaultPaymentCommandHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentProcessorStrategy gateway;

    @Test
    @DisplayName("Deve lançar uma ValidationException (400) sem gravar nada quando nenhuma estratégia suporta o método.")
    void shouldThrowValidationExceptionWhenMethodIsNotSupported() {
        // Arrange: nenhuma estratégia registrada -> nenhum método é suportado
        DefaultPaymentCommandHandler handler = new DefaultPaymentCommandHandler(paymentRepository, List.of());
        ProcessPaymentCommand command = new ProcessPaymentCommand(new Money(BigDecimal.TEN), PaymentMethod.PIX);

        // Act
        UnsupportedPaymentMethodException ex = assertThrows(
                UnsupportedPaymentMethodException.class,
                () -> handler.processPayment(command)
        );

        // Assert: o tipo importa, é ele que decide o status HTTP
        assertInstanceOf(ValidationException.class, ex);
        // A estratégia é escolhida ANTES de gravar: um pedido que não será atendido não toca o banco
        // (antes gravava o pagamento e dependia do rollback para desfazer).
        verifyNoInteractions(paymentRepository);
    }

    /*
     * Ordem do processPayment: (1) escolher a estratégia, (2) gravar o pagamento PENDING, (3) chamar o
     * gateway. O passo 2 termina (commit) antes do passo 3, para a conexão do banco não ficar presa
     * enquanto esperamos uma chamada externa lenta.
     */
    @Test
    @DisplayName("Deve escolher a estratégia, depois gravar o pagamento e só então chamar o gateway com o pagamento gravado.")
    void shouldChooseTheGatewayThenSaveThenCallTheGateway() {
        DefaultPaymentCommandHandler handler =
                new DefaultPaymentCommandHandler(paymentRepository, List.of(gateway));
        Payment saved = Payment.restore(
                new PaymentId(7L), PaymentStatus.PENDING, PaymentMethod.PIX, new Money(BigDecimal.TEN));
        when(gateway.supports(PaymentMethod.PIX)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);

        handler.processPayment(new ProcessPaymentCommand(new Money(BigDecimal.TEN), PaymentMethod.PIX));

        InOrder order = inOrder(gateway, paymentRepository);
        order.verify(gateway).supports(PaymentMethod.PIX);
        order.verify(paymentRepository).save(any(Payment.class));
        // O gateway recebe o pagamento JÁ GRAVADO (com id), que é o que ele precisa para identificá-lo.
        order.verify(gateway).process(saved);
    }

    /*
     * Se o gateway falha, o pagamento já está gravado como PENDING (o commit foi antes) e a exceção sobe.
     * Não marcamos FAILED: uma falha de rede não diz se o gateway chegou a processar o pedido, e um
     * webhook SUCCESS que chegasse depois seria recusado em um pagamento FAILED. Quem decide é o webhook
     * (ou uma conciliação futura).
     */
    @Test
    @DisplayName("Deve deixar o pagamento PENDING (gravado uma vez só) e repassar a exceção quando o gateway falha.")
    void shouldKeepThePaymentPendingAndRethrowWhenTheGatewayFails() {
        DefaultPaymentCommandHandler handler =
                new DefaultPaymentCommandHandler(paymentRepository, List.of(gateway));
        Payment saved = Payment.restore(
                new PaymentId(7L), PaymentStatus.PENDING, PaymentMethod.PIX, new Money(BigDecimal.TEN));
        IllegalStateException failure = new IllegalStateException("gateway timeout");
        when(gateway.supports(PaymentMethod.PIX)).thenReturn(true);
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        doThrow(failure).when(gateway).process(saved);

        ProcessPaymentCommand command = new ProcessPaymentCommand(new Money(BigDecimal.TEN), PaymentMethod.PIX);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> handler.processPayment(command)));

        // Um único save (o PENDING inicial): nada reescreveu o status depois da falha.
        verify(paymentRepository, times(1)).save(any(Payment.class));
        assertEquals(PaymentStatus.PENDING, saved.getStatus());
    }

    @Test
    @DisplayName("Deve lançar uma ResourceNotFoundException (404) quando o pagamento do webhook não existe.")
    void shouldThrowResourceNotFoundExceptionWhenPaymentDoesNotExist() {
        DefaultPaymentCommandHandler handler = new DefaultPaymentCommandHandler(paymentRepository, List.of());
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

        PaymentNotFoundException ex = assertThrows(
                PaymentNotFoundException.class,
                () -> handler.confirmPaymentFromWebhook(new ConfirmPaymentCommand(999L, true))
        );

        assertInstanceOf(ResourceNotFoundException.class, ex);
    }

    /*
     * Os Commands não validam mais nada (etapa 9): o domínio (Payment, PaymentId) é quem recusa
     * entrada inválida, e isso acontece antes de qualquer acesso ao repositório.
     */
    @Test
    @DisplayName("Deve lançar DomainException (do Payment) sem salvar nada quando o método é nulo.")
    void shouldRejectNullMethodBeforeSaving() {
        DefaultPaymentCommandHandler handler = new DefaultPaymentCommandHandler(paymentRepository, List.of());
        ProcessPaymentCommand command = new ProcessPaymentCommand(new Money(BigDecimal.TEN), null);

        assertThrows(DomainException.class, () -> handler.processPayment(command));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Deve lançar DomainException (do Payment) sem salvar nada quando o valor é nulo.")
    void shouldRejectNullAmountBeforeSaving() {
        DefaultPaymentCommandHandler handler = new DefaultPaymentCommandHandler(paymentRepository, List.of());
        ProcessPaymentCommand command = new ProcessPaymentCommand(null, PaymentMethod.CASH);

        assertThrows(DomainException.class, () -> handler.processPayment(command));

        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Deve lançar DomainException (do PaymentId) sem consultar o banco quando o id não é positivo.")
    void shouldRejectNonPositivePaymentIdBeforeQuerying() {
        DefaultPaymentCommandHandler handler = new DefaultPaymentCommandHandler(paymentRepository, List.of());
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(0L, true);

        assertThrows(DomainException.class, () -> handler.confirmPaymentFromWebhook(command));

        verifyNoInteractions(paymentRepository);
    }
}

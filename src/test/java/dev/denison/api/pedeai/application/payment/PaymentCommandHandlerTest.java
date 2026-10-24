package dev.denison.api.pedeai.application.payment;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
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
import dev.denison.api.pedeai.core.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
class PaymentCommandHandlerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Deve lançar uma ValidationException (400) quando nenhuma estratégia suporta o método.")
    void shouldThrowValidationExceptionWhenMethodIsNotSupported() {
        // Arrange: nenhuma estratégia registrada -> nenhum método é suportado
        PaymentCommandHandler handler = new PaymentCommandHandler(paymentRepository, List.of());
        Payment saved = Payment.restore(
                new PaymentId(1L), PaymentStatus.PENDING, PaymentMethod.PIX, new Money(BigDecimal.TEN));
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        ProcessPaymentCommand command = new ProcessPaymentCommand(new Money(BigDecimal.TEN), PaymentMethod.PIX);

        // Act
        UnsupportedPaymentMethodException ex = assertThrows(
                UnsupportedPaymentMethodException.class,
                () -> handler.processPayment(command)
        );

        // Assert: o tipo importa, é ele que decide o status HTTP
        assertInstanceOf(ValidationException.class, ex);
    }

    @Test
    @DisplayName("Deve lançar uma ResourceNotFoundException (404) quando o pagamento do webhook não existe.")
    void shouldThrowResourceNotFoundExceptionWhenPaymentDoesNotExist() {
        PaymentCommandHandler handler = new PaymentCommandHandler(paymentRepository, List.of());
        when(paymentRepository.findById(any(PaymentId.class))).thenReturn(Optional.empty());

        PaymentNotFoundException ex = assertThrows(
                PaymentNotFoundException.class,
                () -> handler.confirmPaymentFromWebhook(new ConfirmPaymentCommand(999L, true))
        );

        assertInstanceOf(ResourceNotFoundException.class, ex);
    }
}

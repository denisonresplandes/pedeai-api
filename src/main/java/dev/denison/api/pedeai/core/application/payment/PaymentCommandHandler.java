package dev.denison.api.pedeai.core.application.payment;

import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.exceptions.PaymentNotFoundException;
import dev.denison.api.pedeai.core.application.payment.exceptions.UnsupportedPaymentMethodException;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;

import java.util.List;

public class PaymentCommandHandler {

    private final PaymentRepository paymentRepository;
    private final List<PaymentProcessorStrategy> strategies;

    public PaymentCommandHandler(PaymentRepository paymentRepository,
                                 List<PaymentProcessorStrategy> strategies) {
        this.paymentRepository = paymentRepository;
        this.strategies = strategies;
    }

    // FLUXO 1: O cliente clica em "Pagar" no App
    public void processPayment(ProcessPaymentCommand command) {
        // cria a entidade de domínio e salva no banco com status PENDING.
        Payment payment = paymentRepository.save(
                Payment.create(
                        command.method(),
                        command.amount()
                )
        );
        // encontra a estratégia correta (cartão, PIX, etc)
        PaymentProcessorStrategy processor = strategies.stream()
                .filter(strategy -> strategy.supports(payment.getMethod()))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentMethodException(payment.getMethod()));

        // dispara a requisição para o Gateway
        processor.process(payment);
    }

    // FLUXO 2: O Stripe avisa de forma assíncrona que o cartão passou
    public void confirmPaymentFromWebhook(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findById(new PaymentId(command.paymentId()))
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        // aplica a intenção no domínio
        if (command.isSuccess()) {
            payment.markAsPaid();
            // TODO disparar um DomainEvent para avisar a Cozinha/Pedido.
        }
        else {
            payment.markAsFailed();
        }
        paymentRepository.save(payment);
    }
}

package dev.denison.api.pedeai.core.application.payment;

import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;

/*
    Interface do Gateway de Pagamento.
    Cada métoodo de pagamento vai implementar.
 */
public interface PaymentProcessorStrategy {

    boolean supports(PaymentMethod method);

    void process(Payment payment);
}

package dev.denison.api.pedeai.infrastructure.gateways.internal;

import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import static dev.denison.api.pedeai.core.domain.payment.PaymentMethod.*;

@Component
public class CashPaymentProcessor implements PaymentProcessorStrategy {

    @Override
    public boolean supports(PaymentMethod method) {
        return method == CASH;
    }

    @Override
    public void process(Payment payment) {
        System.out.println("|>> Pagamento em dinheiro selecionado. Aguardando pagamento na entrega.");
        // Não chama nenhuma API. Apenas registra que o métoodo é ‘offline’.
    }
}

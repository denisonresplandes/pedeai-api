package dev.denison.api.pedeai.infrastructure.gateways.stripe;

import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import static dev.denison.api.pedeai.core.domain.payment.PaymentMethod.CREDIT_CARD;

@Component
public class StripeCreditCardProcessor implements PaymentProcessorStrategy {

    @Override
    public boolean supports(PaymentMethod method) {
        return method == CREDIT_CARD;
    }

    @Override
    public void process(Payment payment) {
        System.out.println("|>> Iniciando transação no STRIPE para o pagamento ID: " +
                payment.getId().value());
        // Aqui vai o código real que monta o JSON e faz o POST HTTP para a API do Stripe.
    }
}

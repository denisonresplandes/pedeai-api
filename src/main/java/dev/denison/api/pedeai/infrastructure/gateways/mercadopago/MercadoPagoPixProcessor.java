package dev.denison.api.pedeai.infrastructure.gateways.mercadopago;

import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import org.springframework.stereotype.Component;

import static dev.denison.api.pedeai.core.domain.payment.PaymentMethod.PIX;

@Component
public class MercadoPagoPixProcessor implements PaymentProcessorStrategy {

    @Override
    public boolean supports(PaymentMethod method) {
        return method == PIX;
    }

    @Override
    public void process(Payment payment) {
        System.out.println("Gerando QR Code PIX no MERCADO PAGO para o pagamento ID: " +
                payment.getId().value());
        // código real que chama a API do Mercado Pago para gerar o copia-e-cola.
    }
}

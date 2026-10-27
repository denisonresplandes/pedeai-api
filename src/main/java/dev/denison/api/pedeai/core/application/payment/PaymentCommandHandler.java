package dev.denison.api.pedeai.core.application.payment;

import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;

// Contrato dos casos de uso de ESCRITA de Payment. O porquê da interface está no KitchenCommandHandler.
public interface PaymentCommandHandler {

    // FLUXO 1: o cliente clica em "Pagar" no app.
    void processPayment(ProcessPaymentCommand command);

    // FLUXO 2: o Stripe avisa, de forma assíncrona, o resultado do pagamento.
    void confirmPaymentFromWebhook(ConfirmPaymentCommand command);
}

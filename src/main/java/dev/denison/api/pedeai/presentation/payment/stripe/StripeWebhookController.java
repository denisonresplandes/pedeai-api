package dev.denison.api.pedeai.presentation.payment.stripe;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final PaymentCommandHandler commandHandler;

    public StripeWebhookController(PaymentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    // chamado pelos servidores do Stripe
    @PostMapping
    @Transactional
    public ResponseEntity<Void> handleStripeCallback(@RequestBody StripeWebhookPayload payload) {
        /*
        * Validações de segurança do payload do Stripe entrra aqui
        * */

        // Verifica se o status é de sucesso
        boolean isSuccess = payload.status().isSuccess();
        // Cria o Command com o ID do pagamento que o Stripe devolveu
        ConfirmPaymentCommand command = new ConfirmPaymentCommand(payload.paymentId(), isSuccess);

        commandHandler.confirmPaymentFromWebhook(command);
        return ResponseEntity.ok().build(); // Stripe exige resposta 200 para confirmar o recebimento.
    }
}

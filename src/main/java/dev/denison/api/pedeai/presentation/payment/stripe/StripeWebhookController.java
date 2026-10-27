package dev.denison.api.pedeai.presentation.payment.stripe;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> handleStripeCallback(@RequestBody @Valid StripeWebhookPayload payload) {
        /*
        * Sem o @Valid, as anotações do StripeWebhookPayload (@NotNull, @Positive) são apenas
        * decoração: o Bean Validation só roda no parâmetro marcado com @Valid. Antes, um payload
        * sem "status" passava direto e estourava NullPointerException (500) logo abaixo; agora
        * o Spring responde 400 (MethodArgumentNotValidException -> GlobalExceptionHandler)
        * antes de entrar no método.
        * */
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

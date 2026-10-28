package dev.denison.api.pedeai.presentation.payment;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.presentation.payment.dto.ProcessPaymentRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    // Na prática é o TransactionalPaymentCommandHandlerDecorator (montado no PaymentConfig). Atenção: o
    // processPayment NÃO roda em transação (chama o gateway); o decorator só repassa a chamada.
    private final PaymentCommandHandler commandHandler;

    public PaymentController(PaymentCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    // chamado pelo app (front-end)
    @PostMapping
    public ResponseEntity<Void> pay(@RequestBody @Valid ProcessPaymentRequest request) {
        commandHandler.processPayment(request.toCommand());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}

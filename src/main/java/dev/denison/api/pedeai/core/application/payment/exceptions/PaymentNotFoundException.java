package dev.denison.api.pedeai.core.application.payment.exceptions;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long id) {
        super("Payment with id %d not found.".formatted(id));
    }
}

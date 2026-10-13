package dev.denison.api.pedeai.core.application.payment.exceptions;

import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;

public class UnsupportedPaymentMethodException extends RuntimeException {

    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("Unsupported payment method: %s".formatted(method));
    }
}

package dev.denison.api.pedeai.core.application.payment.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.shared.Money;

public record ProcessPaymentCommand(
//      Long orderId,
        Money amount,
        PaymentMethod method
) {
    public ProcessPaymentCommand {
        ValidationException.requireNonNull(
                amount,
                "Payment Amount cannot be null"
        );
        ValidationException.requireNonNull(
                method,
                "Payment Method cannot be null"
        );
    }
}
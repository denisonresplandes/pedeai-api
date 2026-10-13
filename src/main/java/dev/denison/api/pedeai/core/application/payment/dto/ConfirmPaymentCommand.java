package dev.denison.api.pedeai.core.application.payment.dto;

import dev.denison.api.pedeai.core.application.shared.ValidationException;

// confirmação para o webhook
public record ConfirmPaymentCommand(
        Long paymentId, // poderia ser PaymentId (VO)
        boolean isSuccess
) {
    public ConfirmPaymentCommand {
        ValidationException.requirePositive(
                paymentId,
                "Payment ID must be a positive number"
        );
    }
}

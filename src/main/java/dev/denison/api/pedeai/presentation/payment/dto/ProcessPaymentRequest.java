package dev.denison.api.pedeai.presentation.payment.dto;

import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.shared.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProcessPaymentRequest(
        // orderId
        @NotNull(message = "{payment.amount.required}")
        @Positive(message = "{payment.amount.positive}")
        BigDecimal amount,

        @NotNull(message = "{payment.method.required}")
        PaymentMethod method
) {
    public ProcessPaymentCommand toCommand() {
        return new ProcessPaymentCommand(
                new Money(amount),
                method
        );
    }
}

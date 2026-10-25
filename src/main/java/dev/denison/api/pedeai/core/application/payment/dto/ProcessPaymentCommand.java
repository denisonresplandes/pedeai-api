package dev.denison.api.pedeai.core.application.payment.dto;

import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.shared.Money;

public record ProcessPaymentCommand(
//      Long orderId,
        Money amount,
        PaymentMethod method
) {
}

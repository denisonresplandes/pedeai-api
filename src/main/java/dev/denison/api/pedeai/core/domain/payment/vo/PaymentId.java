package dev.denison.api.pedeai.core.domain.payment.vo;

import dev.denison.api.pedeai.core.domain.shared.DomainException;

public record PaymentId(
        Long value
) {
    public PaymentId {
        DomainException.requirePositive(
                value,
                "The payment id must be a positive number."
        );
    }
}
package dev.denison.api.pedeai.core.domain.shared;

import java.math.BigDecimal;

public record Money(
        BigDecimal value
) {
    public Money {
        DomainException.requireNonNull(value, "amount cannot be null");
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("amount cannot be negative");
        }
    }
}

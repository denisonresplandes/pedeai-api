package dev.denison.api.pedeai.core.domain.shared;

import java.math.BigDecimal;

public record Money(
        BigDecimal value
) {
    public Money {
        DomainException.requireNonNull(value, "amount cannot be null");
        /*
         * A regra recusa zero E negativo (<= 0): um pagamento de valor zero não faz sentido.
         * A mensagem tem que dizer o mesmo; "cannot be negative" sugeria que o zero era aceito.
         * O @Positive do ProcessPaymentRequest espelha esta regra na borda da API.
         */
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("amount must be greater than zero");
        }
    }
}

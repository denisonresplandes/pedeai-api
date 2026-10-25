package dev.denison.api.pedeai.domain.shared;

import dev.denison.api.pedeai.core.domain.shared.DomainException;
import dev.denison.api.pedeai.core.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Teste de domínio puro (sem Spring/Mockito/banco).
 *
 * A regra é "o valor tem que ser MAIOR que zero": tanto o zero quanto o negativo são recusados.
 * A mensagem precisa dizer isso. Antes ela dizia "cannot be negative", o que sugeria que o zero
 * era aceito, mas o código recusava (value <= 0).
 */
class MoneyTest {

    private static final String MUST_BE_POSITIVE = "amount must be greater than zero";

    @Test
    @DisplayName("Deve aceitar um valor positivo.")
    void shouldAcceptPositiveValue() {
        assertEquals(new BigDecimal("10.50"), new Money(new BigDecimal("10.50")).value());
    }

    @Test
    @DisplayName("Deve aceitar o menor valor positivo de duas casas decimais.")
    void shouldAcceptSmallestPositiveValue() {
        assertEquals(new BigDecimal("0.01"), new Money(new BigDecimal("0.01")).value());
    }

    @Test
    @DisplayName("Deve rejeitar zero com uma mensagem que diga que o valor precisa ser maior que zero.")
    void shouldRejectZero() {
        DomainException ex = assertThrows(DomainException.class, () -> new Money(BigDecimal.ZERO));

        assertEquals(MUST_BE_POSITIVE, ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar valor negativo com a mesma mensagem.")
    void shouldRejectNegativeValue() {
        DomainException ex = assertThrows(DomainException.class, () -> new Money(new BigDecimal("-0.01")));

        assertEquals(MUST_BE_POSITIVE, ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar valor nulo.")
    void shouldRejectNull() {
        DomainException ex = assertThrows(DomainException.class, () -> new Money(null));

        assertEquals("amount cannot be null", ex.getMessage());
    }
}

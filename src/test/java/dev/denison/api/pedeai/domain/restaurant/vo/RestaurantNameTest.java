package dev.denison.api.pedeai.domain.restaurant.vo;

import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Teste de domínio puro (sem Spring/Mockito/banco). Veja KitchenNameTest para o porquê
 * dos limites estarem escritos como literais.
 */
class RestaurantNameTest {

    @Test
    @DisplayName("Deve aceitar um nome com exatamente 2 caracteres (limite mínimo).")
    void shouldAcceptNameWithMinimumLength() {
        assertEquals("ab", new RestaurantName("ab").value());
    }

    @Test
    @DisplayName("Deve aceitar um nome com exatamente 50 caracteres (limite máximo).")
    void shouldAcceptNameWithMaximumLength() {
        String name = "x".repeat(50);

        assertEquals(name, new RestaurantName(name).value());
    }

    @Test
    @DisplayName("Deve remover espaços nas pontas antes de medir o tamanho.")
    void shouldTrimBeforeCheckingLength() {
        String name = "x".repeat(50);

        assertEquals(name, new RestaurantName("  " + name + "  ").value());
    }

    @Test
    @DisplayName("Deve rejeitar um nome com 1 caractere.")
    void shouldRejectNameShorterThanMinimum() {
        DomainException ex = assertThrows(DomainException.class, () -> new RestaurantName("a"));

        assertEquals("Restaurant name must be at least 2 characters long.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar um nome que tem 2 caracteres só por causa dos espaços.")
    void shouldRejectNameThatIsTooShortAfterTrim() {
        assertThrows(DomainException.class, () -> new RestaurantName(" a "));
    }

    @Test
    @DisplayName("Deve rejeitar um nome com 51 caracteres.")
    void shouldRejectNameLongerThanMaximum() {
        DomainException ex = assertThrows(DomainException.class, () -> new RestaurantName("x".repeat(51)));

        assertEquals("Restaurant name must be at most 50 characters long.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar nome nulo ou em branco.")
    void shouldRejectNullOrBlankName() {
        assertThrows(DomainException.class, () -> new RestaurantName(null));
        assertThrows(DomainException.class, () -> new RestaurantName("   "));
    }
}

package dev.denison.api.pedeai.domain.kitchen.vo;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Teste de domínio puro: sem Spring, sem Mockito, sem banco.
 * Se o domínio é a fonte da verdade das invariantes, é aqui que as regras de nome
 * precisam ser garantidas (e não só no Request ou no Command).
 *
 * Os limites (2 e 50) estão escritos como literais de propósito: o teste documenta
 * o contrato e falharia se alguém alterasse a regra sem perceber.
 */
class KitchenNameTest {

    @Test
    @DisplayName("Deve aceitar um nome com exatamente 2 caracteres (limite mínimo).")
    void shouldAcceptNameWithMinimumLength() {
        assertEquals("ab", new KitchenName("ab").value());
    }

    @Test
    @DisplayName("Deve aceitar um nome com exatamente 50 caracteres (limite máximo).")
    void shouldAcceptNameWithMaximumLength() {
        String name = "x".repeat(50);

        assertEquals(name, new KitchenName(name).value());
    }

    @Test
    @DisplayName("Deve remover espaços nas pontas antes de medir o tamanho.")
    void shouldTrimBeforeCheckingLength() {
        // 50 caracteres úteis + espaços: só é válido porque o trim vem ANTES da checagem do máximo.
        String name = "x".repeat(50);

        assertEquals(name, new KitchenName("  " + name + "  ").value());
    }

    @Test
    @DisplayName("Deve rejeitar um nome com 1 caractere.")
    void shouldRejectNameShorterThanMinimum() {
        DomainException ex = assertThrows(DomainException.class, () -> new KitchenName("a"));

        assertEquals("Kitchen name must be at least 2 characters long.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar um nome que tem 2 caracteres só por causa dos espaços.")
    void shouldRejectNameThatIsTooShortAfterTrim() {
        assertThrows(DomainException.class, () -> new KitchenName(" a "));
    }

    @Test
    @DisplayName("Deve rejeitar um nome com 51 caracteres.")
    void shouldRejectNameLongerThanMaximum() {
        DomainException ex = assertThrows(DomainException.class, () -> new KitchenName("x".repeat(51)));

        assertEquals("Kitchen name must be at most 50 characters long.", ex.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar nome nulo ou em branco.")
    void shouldRejectNullOrBlankName() {
        assertThrows(DomainException.class, () -> new KitchenName(null));
        assertThrows(DomainException.class, () -> new KitchenName("   "));
    }
}

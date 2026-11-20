package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de unidade da entidade {@link Kitchen}: JUnit puro, sem Spring e sem banco.
 */
class KitchenTest {

    // Os testes usam os limites da própria entidade, para não repetir os números.
    private static String nameWithLength(int length) {
        return "x".repeat(length);
    }

    @Test
    void createsKitchenWithValidNameAndTrimsSpaces() {
        Kitchen kitchen = Kitchen.create("  Japonesa  ");

        assertThat(kitchen.getName()).isEqualTo("Japonesa");
        assertThat(kitchen.getId()).isNull(); // o id só existe depois que o banco salva
    }

    @Test
    void acceptsNameAtTheLengthLimits() {
        String minimum = nameWithLength(Kitchen.NAME_MIN_LENGTH);
        String maximum = nameWithLength(Kitchen.NAME_MAX_LENGTH);

        assertThat(Kitchen.create(minimum).getName()).isEqualTo(minimum);
        assertThat(Kitchen.create(maximum).getName()).isEqualTo(maximum);
        // O tamanho é medido depois de aparar: com espaços nas pontas passa de 60, mas continua válido.
        assertThat(Kitchen.create("  " + maximum + "  ").getName()).isEqualTo(maximum);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "a", "  a  "})
    void rejectsNullOrTooShortName(String name) {
        // "  a  " tem 5 caracteres, mas só 1 depois de aparar os espaços.
        assertThatThrownBy(() -> Kitchen.create(name)).isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsNameLongerThanMaximum() {
        String name = nameWithLength(Kitchen.NAME_MAX_LENGTH + 1);

        assertThatThrownBy(() -> Kitchen.create(name)).isInstanceOf(BusinessException.class);
    }

    @Test
    void changeNameKeepsCurrentNameWhenNewNameIsInvalid() {
        Kitchen kitchen = Kitchen.create("Italiana");

        kitchen.changeName("  Francesa ");
        assertThat(kitchen.getName()).isEqualTo("Francesa");

        assertThatThrownBy(() -> kitchen.changeName("a")).isInstanceOf(BusinessException.class);

        // A regra "uma Kitchen sempre tem nome válido" vale também depois de uma troca recusada.
        assertThat(kitchen.getName()).isEqualTo("Francesa");
    }
}

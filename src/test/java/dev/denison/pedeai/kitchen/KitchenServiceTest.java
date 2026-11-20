package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.kitchen.exception.KitchenNameAlreadyExistsException;
import dev.denison.pedeai.kitchen.exception.KitchenNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes de unidade do {@link KitchenService}. O repository é um mock (de mentira), então não há Spring nem banco:
 * o que se confere é a lógica do service, ou seja, o que ele pergunta ao repository e o que devolve ou lança.
 */
@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock
    private KitchenRepository repository;

    private KitchenService service;

    @BeforeEach
    void setUp() {
        service = new KitchenService(repository);
    }

    @Test
    void createsKitchenAfterCheckingTheTrimmedName() {
        // O mock responde que o nome está livre e, ao salvar, devolve a cozinha já com id, como faria o banco.
        // O stub só vale para "Italiana": se o service consultasse com os espaços ("  Italiana  "), o Mockito
        // (modo estrito) recusaria a chamada e o teste falharia.
        Kitchen saved = Kitchen.restore(10L, "Italiana");
        when(repository.existsByNameIgnoreCase("Italiana")).thenReturn(false);
        when(repository.saveAndFlush(any(Kitchen.class))).thenReturn(saved);

        Kitchen result = service.create("  Italiana  ");

        assertThat(result).isSameAs(saved); // o service devolve o que o repository devolveu

        ArgumentCaptor<Kitchen> captor = ArgumentCaptor.forClass(Kitchen.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Italiana"); // e salvou o nome já aparado
    }

    @Test
    void createThrowsWhenNameIsAlreadyInUse() {
        when(repository.existsByNameIgnoreCase("Italiana")).thenReturn(true);

        // A mensagem traz o nome já aparado, o mesmo que foi conferido no banco.
        assertThatThrownBy(() -> service.create("  Italiana  "))
                .isInstanceOf(KitchenNameAlreadyExistsException.class)
                .hasMessage("Kitchen name already in use: Italiana");

        verify(repository, never()).saveAndFlush(any()); // nome em uso: nada é salvo
    }

    // Perdeu a corrida: a checagem disse "livre", mas outra requisição gravou o mesmo nome antes e o banco recusou o
    // insert. O nome da constraint vem com o prefixo da tabela, como o MySQL o informa.
    @Test
    void createTurnsTheNameUniqueViolationFromTheDatabaseIntoNameAlreadyInUse() {
        when(repository.existsByNameIgnoreCase("Italiana")).thenReturn(false);
        when(repository.saveAndFlush(any(Kitchen.class)))
                .thenThrow(integrityViolation(ConstraintKind.UNIQUE, "tb_kitchen." + Kitchen.UNIQUE_NAME));

        assertThatThrownBy(() -> service.create("Italiana"))
                .isInstanceOf(KitchenNameAlreadyExistsException.class)
                .hasMessage("Kitchen name already in use: Italiana");
    }

    // Só a constraint do nome vira "nome em uso". As outras violações seguem como estão (o handler as responde com
    // um 409 genérico), para não dizer ao cliente que o nome está em uso quando o problema é outro.
    @ParameterizedTest
    @MethodSource("otherIntegrityViolations")
    void createDoesNotHideOtherIntegrityViolations(DataIntegrityViolationException violation) {
        when(repository.existsByNameIgnoreCase("Italiana")).thenReturn(false);
        when(repository.saveAndFlush(any(Kitchen.class))).thenThrow(violation);

        assertThatThrownBy(() -> service.create("Italiana")).isSameAs(violation);
    }

    static Stream<DataIntegrityViolationException> otherIntegrityViolations() {
        return Stream.of(
                integrityViolation(ConstraintKind.UNIQUE, "tb_kitchen.uk_outra_constraint"),
                integrityViolation(ConstraintKind.NOT_NULL, "tb_kitchen.name"),
                new DataIntegrityViolationException("sem a exceção do Hibernate na cadeia"));
    }

    private static DataIntegrityViolationException integrityViolation(ConstraintKind kind, String constraintName) {
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("could not execute statement", new SQLException(), "insert",
                        kind, constraintName));
    }

    @Test
    void findByIdThrowsWhenKitchenDoesNotExist() {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(1L))
                .isInstanceOf(KitchenNotFoundException.class)
                .hasMessage("Kitchen not found id: 1");
    }
}

package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.kitchen.exception.KitchenNameAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Confere, no MySQL real, a defesa do {@link KitchenService#create} contra a corrida entre duas requisições. O teste
 * unitário do service monta a exceção do banco à mão; este é o que prova que a violação que o MySQL de fato devolve é
 * reconhecida como "nome em uso". Também precisa do MySQL no ar ({@code replace = NONE}) e desfaz tudo ao fim.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KitchenServiceDatabaseTest {

    @Autowired
    private KitchenRepository repository;

    @Test
    void losingTheRaceAgainstTheDatabaseBecomesNameAlreadyInUse() {
        // "Test-" + UUID tem 41 caracteres, dentro do limite de 60 (o import.sql já cadastra outras cozinhas).
        String name = "Test-" + UUID.randomUUID();
        repository.saveAndFlush(Kitchen.create(name)); // a outra requisição já gravou este nome

        // Só a checagem é enganada: ela responde "livre", como responderia se lesse antes do commit da outra
        // requisição. O insert é o real, e quem o recusa é a constraint UNIQUE do MySQL.
        KitchenRepository staleCheck = mock(KitchenRepository.class);
        when(staleCheck.existsByNameIgnoreCase(name)).thenReturn(false);
        when(staleCheck.saveAndFlush(any(Kitchen.class)))
                .thenAnswer(invocation -> repository.saveAndFlush(invocation.getArgument(0)));

        assertThatThrownBy(() -> new KitchenService(staleCheck).create(name))
                .isInstanceOf(KitchenNameAlreadyExistsException.class)
                .hasMessage("Kitchen name already in use: " + name);
    }
}

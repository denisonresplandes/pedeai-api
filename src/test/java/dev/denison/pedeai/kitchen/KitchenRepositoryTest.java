package dev.denison.pedeai.kitchen;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes da persistência de {@link Kitchen}. Rodam no MySQL configurado, e não num banco em memória
 * ({@code replace = NONE}), por isso o MySQL precisa estar no ar. O {@code @DataJpaTest} desfaz tudo ao
 * fim de cada teste (rollback).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class KitchenRepositoryTest {

    @Autowired
    private KitchenRepository repository;

    // O import.sql já cadastra algumas cozinhas: cada teste usa um nome único para não esbarrar nelas.
    // "Test-" + UUID tem 41 caracteres, dentro do limite de 60.
    private static String uniqueName() {
        return "Test-" + UUID.randomUUID();
    }

    @Test
    void databaseRejectsDuplicateName() {
        String name = uniqueName();
        repository.saveAndFlush(Kitchen.create(name));

        // A última linha de defesa: mesmo que a checagem do service falhe numa corrida entre duas
        // requisições, a constraint UNIQUE barra o segundo insert. (O service traduz essa violação em
        // "nome em uso": ver KitchenServiceDatabaseTest.)
        assertThatThrownBy(() -> repository.saveAndFlush(Kitchen.create(name)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByNameIgnoreCaseIgnoresTheCase() {
        String name = uniqueName();
        repository.saveAndFlush(Kitchen.create(name));

        assertThat(repository.existsByNameIgnoreCase(name.toUpperCase())).isTrue();
        assertThat(repository.existsByNameIgnoreCase(name.toLowerCase())).isTrue();
        assertThat(repository.existsByNameIgnoreCase(uniqueName())).isFalse();
    }
}

package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KitchenQueryHandlerTest {

    @Mock
    private KitchenQueryRepository queryRepository;

    @InjectMocks
    private KitchenQueryHandler queryHandler;

    @Test
    @DisplayName("Deve retornar KitchenResponse quando a cozinha existir por ID.")
    void shouldReturnKitchenResponseWhenKitchenExistsById() {
        Long kitchenId = 1L;
        KitchenResponse stored = new KitchenResponse(kitchenId, "Classic Kitchen");
        when(queryRepository.findById(kitchenId)).thenReturn(Optional.of(stored));

        KitchenResponse response = queryHandler.findById(kitchenId);

        assertNotNull(response);
        assertEquals(kitchenId, response.id());
        assertEquals("Classic Kitchen", response.name());
        verify(queryRepository, times(1)).findById(kitchenId);
    }

    @Test
    @DisplayName("Deve lançar KitchenNotFoundException quando a cozinha não existir por ID.")
    void shouldThrowExceptionWhenKitchenDoesNotExistById() {
        Long kitchenId = 99L;
        when(queryRepository.findById(kitchenId)).thenReturn(Optional.empty());

        assertThrows(KitchenNotFoundException.class, () -> {
            queryHandler.findById(kitchenId);
        });

        verify(queryRepository, times(1)).findById(kitchenId);
    }

    @Test
    @DisplayName("Deve retornar uma lista com a cozinha quando ela existir por nome.")
    void shouldReturnListWithTheKitchenWhenItExistsByName() {
        when(queryRepository.findByName("indiana")).thenReturn(Optional.of(new KitchenResponse(1L, "Indiana")));

        List<KitchenResponse> response = queryHandler.findByName("indiana");

        assertEquals(List.of(new KitchenResponse(1L, "Indiana")), response);
        verify(queryRepository, times(1)).findByName("indiana");
    }

    @Test
    @DisplayName("Deve retornar lista vazia, sem lançar exceção, quando a cozinha não existir por nome.")
    void shouldReturnEmptyListWhenKitchenDoesNotExistByName() {
        when(queryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertEquals(List.of(), queryHandler.findByName("Unknown"));
    }

    @Test
    @DisplayName("Deve retornar a lista de cozinhas quando o findAll for invocado.")
    void shouldReturnListOfKitchenResponses() {
        List<KitchenResponse> stored = List.of(
                new KitchenResponse(1L, "Kitchen One"),
                new KitchenResponse(2L, "Kitchen Two")
        );
        when(queryRepository.findAll()).thenReturn(stored);

        List<KitchenResponse> responses = queryHandler.findAll();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(queryRepository, times(1)).findAll();
    }

    /*
     * O dado de leitura não é revalidado. Antes existia o KitchenSummary, que validava o nome em
     * toda leitura: uma linha do banco que não seguisse a regra (ex.: gravada por SQL manual)
     * derrubava o GET inteiro com um 400, como se o cliente tivesse errado. Quem valida é a
     * escrita (Request e VO); a leitura devolve o que está gravado.
     */
    @Test
    @DisplayName("Deve devolver o dado de leitura como está, sem revalidar (um GET não vira 400 por dado do banco).")
    void shouldReturnStoredDataWithoutRevalidating() {
        when(queryRepository.findById(1L)).thenReturn(Optional.of(new KitchenResponse(1L, "a")));

        KitchenResponse response = queryHandler.findById(1L);

        assertEquals("a", response.name());
    }
}

package dev.denison.api.pedeai.application.kitchen;

import dev.denison.api.pedeai.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.domain.kitchen.KitchenQueryRepository;
import dev.denison.api.pedeai.domain.kitchen.KitchenSummary;
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
        KitchenSummary summary = new KitchenSummary(kitchenId, "Classic Kitchen");
        when(queryRepository.findById(kitchenId)).thenReturn(Optional.of(summary));

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
    @DisplayName("Deve retornar a lista de cozinhas quando o findAll for invocado.")
    void shouldReturnListOfKitchenResponses() {
        List<KitchenSummary> summaries = List.of(
                new KitchenSummary(1L, "Kitchen One"),
                new KitchenSummary(2L, "Kitchen Two")
        );
        when(queryRepository.findAll()).thenReturn(summaries);

        List<KitchenResponse> responses = queryHandler.findAll();

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(queryRepository, times(1)).findAll();
    }
}

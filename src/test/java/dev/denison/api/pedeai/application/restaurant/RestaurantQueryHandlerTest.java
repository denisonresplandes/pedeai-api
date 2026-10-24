package dev.denison.api.pedeai.application.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryHandler;
import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.application.restaurant.exceptions.RestaurantNotFoundException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantQueryRepository;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantQueryHandlerTest {

    @Mock
    private RestaurantQueryRepository queryRepository;

    @InjectMocks
    private RestaurantQueryHandler queryHandler;

    @Test
    @DisplayName("Deve retornar RestaurantResponse quando o restaurante existir por ID.")
    void shouldReturnRestaurantResponseWhenRestaurantExistsById() {
        RestaurantSummary summary = new RestaurantSummary(1L, "Ilha Verde", 2L, Set.of(PaymentMethod.CASH));
        when(queryRepository.findById(1L)).thenReturn(Optional.of(summary));

        RestaurantResponse response = queryHandler.findById(1L);

        assertEquals(1L, response.id());
        assertEquals("Ilha Verde", response.name());
        assertEquals(2L, response.kitchenId());
        assertEquals(Set.of(PaymentMethod.CASH), response.paymentMethods());
        verify(queryRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar RestaurantNotFoundException quando o restaurante não existir por ID.")
    void shouldThrowExceptionWhenRestaurantDoesNotExistById() {
        when(queryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> queryHandler.findById(99L));

        verify(queryRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve retornar RestaurantResponse quando o restaurante existir por nome.")
    void shouldReturnRestaurantResponseWhenRestaurantExistsByName() {
        RestaurantSummary summary = new RestaurantSummary(1L, "Ilha Verde", 2L, Set.of(PaymentMethod.CASH));
        when(queryRepository.findByName("ilha verde")).thenReturn(Optional.of(summary));

        RestaurantResponse response = queryHandler.findByName("ilha verde");

        assertEquals("Ilha Verde", response.name());
        verify(queryRepository, times(1)).findByName("ilha verde");
    }

    @Test
    @DisplayName("Deve lançar RestaurantNotFoundException quando o restaurante não existir por nome.")
    void shouldThrowExceptionWhenRestaurantDoesNotExistByName() {
        when(queryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> queryHandler.findByName("Unknown"));
    }

    @Test
    @DisplayName("Deve retornar a lista de restaurantes quando o findAll for invocado.")
    void shouldReturnListOfRestaurantResponses() {
        List<RestaurantSummary> summaries = List.of(
                new RestaurantSummary(1L, "Restaurant One", 1L, Set.of(PaymentMethod.CASH)),
                new RestaurantSummary(2L, "Restaurant Two", 2L, Set.of(PaymentMethod.CASH, PaymentMethod.PIX))
        );
        when(queryRepository.findAll()).thenReturn(summaries);

        List<RestaurantResponse> responses = queryHandler.findAll();

        assertEquals(2, responses.size());
        assertEquals("Restaurant Two", responses.get(1).name());
        verify(queryRepository, times(1)).findAll();
    }
}

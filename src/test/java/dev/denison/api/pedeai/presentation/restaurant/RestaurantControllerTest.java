package dev.denison.api.pedeai.presentation.restaurant;

import dev.denison.api.pedeai.core.application.restaurant.RestaurantCommandHandler;
import dev.denison.api.pedeai.core.application.restaurant.RestaurantQueryHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.denison.api.pedeai.core.application.restaurant.dto.RestaurantResponse;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import java.util.Set;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * @WebMvcTest sobe só a camada web (controllers, validação, @RestControllerAdvice).
 * Os handlers do core são mockados: aqui testamos o contrato HTTP, não a regra de negócio.
 * O sufixo "Test" é importante: o surefire só executa *Test/*Tests (o sufixo *IT é do
 * failsafe, que não está configurado neste projeto).
 */
@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    private static final String SIZE_MESSAGE = "The restaurant name must be between 2 and 50 characters long";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantCommandHandler commandHandler;

    @MockitoBean
    private RestaurantQueryHandler queryHandler;

    @Test
    @DisplayName("Deve devolver a mensagem de messages.properties quando o kitchenId não for informado.")
    void shouldReturnRequiredMessageWhenKitchenIdIsMissing() throws Exception {
        // Arrange: JSON sem o campo kitchenId
        String body = """
                {"name": "Ilha Verde", "paymentMethods": ["CASH"]}
                """;

        // Act & Assert
        /*
         * Se a chave usada no @NotNull não existir em messages.properties, o Bean Validation
         * não resolve o placeholder e devolve o texto literal "{chave}" ao cliente.
         * Por isso comparamos com a mensagem já resolvida, e não com a chave.
         */
        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.kitchenId").value("The kitchen id cannot be null"));
    }

    @Test
    @DisplayName("Deve devolver 400 com a mensagem de tamanho ao criar restaurante com 51 caracteres no nome.")
    void shouldReturnBadRequestWhenCreatingWithNameLongerThanMaximum() throws Exception {
        String body = """
                {"name": "%s", "kitchenId": 1, "paymentMethods": ["CASH"]}
                """.formatted("x".repeat(51));

        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(SIZE_MESSAGE));
    }

    @Test
    @DisplayName("Deve devolver 400 com a mensagem de tamanho ao atualizar restaurante com 51 caracteres no nome.")
    void shouldReturnBadRequestWhenUpdatingWithNameLongerThanMaximum() throws Exception {
        String body = """
                {"name": "%s"}
                """.formatted("x".repeat(51));

        mockMvc.perform(put("/restaurants/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(SIZE_MESSAGE));
    }

    @Test
    @DisplayName("Deve devolver a mensagem de messages.properties quando o kitchenId não for positivo.")
    void shouldReturnPositiveMessageWhenKitchenIdIsNotPositive() throws Exception {
        String body = """
                {"name": "Ilha Verde", "kitchenId": 0, "paymentMethods": ["CASH"]}
                """;

        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.kitchenId").value("The kitchen id must be a positive number"));
    }

    // Filtro por nome devolve sempre uma lista (0 ou 1 elemento): veja KitchenControllerTest.
    @Test
    @DisplayName("GET /restaurants?name=: devolve 200 com uma lista com o restaurante encontrado.")
    void shouldReturnListWithTheRestaurantWhenFilteringByName() throws Exception {
        when(queryHandler.findByName("ilha verde")).thenReturn(
                List.of(new RestaurantResponse(1L, "Ilha Verde", 2L, Set.of(PaymentMethod.CASH))));

        mockMvc.perform(get("/restaurants").param("name", "ilha verde"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Ilha Verde"))
                .andExpect(jsonPath("$[0].kitchenId").value(2));
    }

    @Test
    @DisplayName("GET /restaurants?name=: devolve 200 com lista vazia (e não 404) quando nenhum restaurante tem o nome.")
    void shouldReturnEmptyListWhenNoRestaurantHasTheName() throws Exception {
        when(queryHandler.findByName("nope")).thenReturn(List.of());

        mockMvc.perform(get("/restaurants").param("name", "nope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /restaurants sem filtro: lista tudo e não consulta por nome.")
    void shouldListAllWhenThereIsNoNameFilter() throws Exception {
        when(queryHandler.findAll()).thenReturn(List.of(
                new RestaurantResponse(1L, "Restaurant One", 1L, Set.of(PaymentMethod.CASH)),
                new RestaurantResponse(2L, "Restaurant Two", 2L, Set.of(PaymentMethod.CASH))));

        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(queryHandler, never()).findByName(any());
    }
}

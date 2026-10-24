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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}

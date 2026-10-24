package dev.denison.api.pedeai.presentation.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * @WebMvcTest sobe só a camada web; os handlers do core são mockados.
 * Aqui testamos o contrato HTTP (status e mensagens de validação do Request).
 */
@WebMvcTest(KitchenController.class)
class KitchenControllerTest {

    private static final String SIZE_MESSAGE = "The kitchen name must be between 2 and 50 characters long";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KitchenCommandHandler commandHandler;

    @MockitoBean
    private KitchenQueryHandler queryHandler;

    @Test
    @DisplayName("Deve devolver 400 com a mensagem de tamanho ao criar cozinha com 51 caracteres.")
    void shouldReturnBadRequestWhenCreatingWithNameLongerThanMaximum() throws Exception {
        String body = """
                {"name": "%s"}
                """.formatted("x".repeat(51));

        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(SIZE_MESSAGE));
    }

    @Test
    @DisplayName("Deve devolver 400 com a mensagem de tamanho ao criar cozinha com 1 caractere.")
    void shouldReturnBadRequestWhenCreatingWithNameShorterThanMinimum() throws Exception {
        // Garante que o placeholder {min}/{max} da mensagem é interpolado (nada de "{min}" literal).
        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "a"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(SIZE_MESSAGE));
    }

    @Test
    @DisplayName("Deve devolver 201 ao criar cozinha com exatamente 50 caracteres.")
    void shouldCreateWhenNameHasMaximumLength() throws Exception {
        String name = "x".repeat(50);
        when(commandHandler.create(any())).thenReturn(new KitchenResponse(1L, name));

        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    @DisplayName("Deve devolver 400 com a mensagem de tamanho ao atualizar cozinha com 51 caracteres.")
    void shouldReturnBadRequestWhenUpdatingWithNameLongerThanMaximum() throws Exception {
        String body = """
                {"name": "%s"}
                """.formatted("x".repeat(51));

        mockMvc.perform(put("/kitchens/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value(SIZE_MESSAGE));
    }
}

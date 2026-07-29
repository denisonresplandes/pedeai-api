package dev.denison.api.pedeai.presentation.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import dev.denison.api.pedeai.core.application.kitchen.exceptions.KitchenNotFoundException;
import dev.denison.api.pedeai.presentation.kitchen.dto.CreateKitchenRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KitchenController.class) // Inicializa apenas o contexto Web do Spring para este Controller
public class KitchenControllerIT {

    @Autowired
    private MockMvc mockMvc; // Simula as requisições HTTP (GET, POST, etc.)

    @Autowired
    private ObjectMapper objectMapper; // Converte objetos Java para strings JSON

    @MockitoBean
    private KitchenCommandHandler commandHandler; // Mock da camada de aplicação

    @MockitoBean
    private KitchenQueryHandler queryHandler; // Mock da camada de aplicação

    @Test
    @DisplayName("Deve retornar HTTP 201 Created e o payload ao criar uma cozinha válida.")
    void shouldReturnCreatedWhenKitchenIsValid() throws Exception {
        // Arrange
        CreateKitchenRequest request = new CreateKitchenRequest("Italian Kitchen");
        KitchenResponse expectedResponse = new KitchenResponse(1L, "Italian Kitchen");

        // Handler mockado devolve o response quando o controller chamá-lo
        when(commandHandler.create(any())).thenReturn(expectedResponse);

        // Act & Assert (Execução do HTTP e as validações juntas)
        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // Transforma o Request em JSON
                .andExpect(status().isCreated()) // Valida se o status HTTP é 201
                .andExpect(jsonPath("$.id").value(1L)) // Valida o ID no corpo do JSON recebido
                .andExpect(jsonPath("$.name").value("Italian Kitchen")); // Valida o Name no JSON
    }

    @Test
    @DisplayName("Deve retornar HTTP 400 Bad Request ao criar uma cozinha com nome em branco.")
    void shouldReturnBadRequestWhenNameIsBlank() throws Exception {
        // Arrange
        CreateKitchenRequest request = new CreateKitchenRequest("   "); // Nome inválido (em branco)

        // Act & Assert
        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))) // Transforma o Request em JSON.
                .andExpect(status().isBadRequest()); // Valida se o Spring barrou e gerou o HTTP 400
    }

    @Test
    @DisplayName("Deve retornar HTTP 200 OK ao buscar uma cozinha com ID 1")
    void shouldReturnOkWhenIdExists() throws Exception {
        // Arrange
        Long kitchenId = 1L;
        KitchenResponse expectedResponse = new KitchenResponse(kitchenId, "Italian");

        when(queryHandler.findById(kitchenId)).thenReturn(expectedResponse);

        // Act & Assert
        mockMvc.perform(get("/kitchens/{id}", kitchenId) // Forma nativa do MockMvc para passar PathVariable
                        .accept(MediaType.APPLICATION_JSON)) // Indica que o cliente aceita JSON como resposta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Italian"));
    }

    @Test
    @DisplayName("Deve retornar HTTP 404 Not Found ao buscar uma cozinha com ID inexistente.")
    void shouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
        // Arrange
        Long kitchenId = 99L;
        when(queryHandler.findById(kitchenId)).thenThrow(
                new KitchenNotFoundException(kitchenId)
        );

        // Act & Assert
        mockMvc.perform(get("/kitchens/{id}", kitchenId)
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

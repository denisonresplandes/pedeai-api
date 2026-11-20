package dev.denison.pedeai.kitchen;

import dev.denison.pedeai.kitchen.exception.KitchenNameAlreadyExistsException;
import dev.denison.pedeai.kitchen.exception.KitchenNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de API do {@link KitchenController}: status, headers e JSON de cada endpoint, pelo {@code MockMvc}.
 * <p>
 * O {@code @WebMvcTest} sobe só a camada web (controller, {@code GlobalExceptionHandler}, conversores JSON e Bean
 * Validation com o {@code messages.properties}), sem banco. O service é um mock: cada erro de negócio é simulado
 * fazendo o mock lançar a exceção que o service real lançaria. As regras do service e da entidade já têm teste próprio,
 * e o mapeamento de cada exceção em HTTP é fixado no {@code GlobalExceptionHandlerTest}.
 */
@WebMvcTest(KitchenController.class)
class KitchenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KitchenService service;

    // ---------- POST /kitchens ----------

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(service.create("Italiana")).thenReturn(Kitchen.restore(7L, "Italiana"));

        // O nome chega com espaços nas pontas: o Request o apara antes de chamar o service.
        postKitchen("  Italiana  ")
                .andExpect(status().isCreated())
                // O Location inclui o caminho do controller (/kitchens), e não só o id.
                .andExpect(header().string(HttpHeaders.LOCATION, "http://localhost/kitchens/7"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Italiana"));

        verify(service).create("Italiana");
    }

    @Test
    void createRejectsBlankNameWithBothMessages() throws Exception {
        // "   " vira "" depois de aparado e falha nas duas constraints, então o campo tem dois erros, em ordem
        // estável (por mensagem), com o texto do messages.properties.
        postKitchen("   ")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("kitchen name is required"))
                .andExpect(jsonPath("$.errors[1].field").value("name"))
                .andExpect(jsonPath("$.errors[1].message").value(sizeMessage()));

        verifyNoInteractions(service); // o Request barra antes do service
    }

    @Test
    void createReturns409WhenNameIsAlreadyInUse() throws Exception {
        when(service.create("Italiana")).thenThrow(new KitchenNameAlreadyExistsException("Italiana"));

        postKitchen("Italiana")
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Kitchen name already in use: Italiana"));
    }

    // ---------- GET /kitchens/{id} ----------

    @Test
    void findByIdReturns200WithTheKitchen() throws Exception {
        when(service.findById(1L)).thenReturn(Kitchen.restore(1L, "Italiana"));

        mockMvc.perform(get("/kitchens/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Italiana"));
    }

    // O id 0 não é barrado no controller (não há @Positive): chega ao service, que não o acha.
    @ParameterizedTest
    @ValueSource(longs = {99, 0})
    void findByIdReturns404WhenKitchenDoesNotExist(long id) throws Exception {
        when(service.findById(id)).thenThrow(new KitchenNotFoundException(id));

        mockMvc.perform(get("/kitchens/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Kitchen not found id: " + id));
    }

    // ---------- GET /kitchens ----------

    @Test
    void findAllReturns200WithAllKitchens() throws Exception {
        when(service.findAll()).thenReturn(List.of(Kitchen.restore(1L, "Italiana"), Kitchen.restore(2L, "Japonesa")));

        mockMvc.perform(get("/kitchens"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Italiana"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Japonesa"));
    }

    // ---------- apoio ----------

    private ResultActions postKitchen(String name) throws Exception {
        return mockMvc.perform(post("/kitchens")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"%s\"}".formatted(name)));
    }

    // Os números da mensagem vêm das constantes da entidade, usadas na anotação @Size do Request.
    private static String sizeMessage() {
        return "kitchen name must have between %d and %d characters"
                .formatted(Kitchen.NAME_MIN_LENGTH, Kitchen.NAME_MAX_LENGTH);
    }
}

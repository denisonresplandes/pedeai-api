package dev.denison.api.pedeai.presentation.kitchen;

import dev.denison.api.pedeai.core.application.kitchen.KitchenCommandHandler;
import dev.denison.api.pedeai.core.application.kitchen.KitchenQueryHandler;
import dev.denison.api.pedeai.core.application.kitchen.dto.KitchenResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.sql.SQLIntegrityConstraintViolationException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

    /*
     * Rede de segurança contra corrida: duas requisições simultâneas passam pelo existsByName
     * (ambas veem "nome livre") e a segunda estoura a constraint única do banco no INSERT.
     * O Spring traduz isso para DataIntegrityViolationException; sem um handler, vira 500.
     * O detalhe do SQL ("Duplicate entry ... for key ...") NÃO pode vazar para o cliente.
     */
    @Test
    @DisplayName("Deve devolver 409 (e não 500) quando o banco rejeita o INSERT por chave duplicada.")
    void shouldReturnConflictWhenDatabaseRejectsDuplicateKey() throws Exception {
        when(commandHandler.create(any())).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Duplicate entry 'Indiana' for key 'tb_kitchen.unq_kitchen_name'")));

        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Indiana"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("The operation could not be completed because it conflicts with existing data.")
                );
    }

    @Test
    @DisplayName("Deve devolver 409 (e não 500) quando o banco rejeita o DELETE por chave estrangeira.")
    void shouldReturnConflictWhenDatabaseRejectsForeignKeyViolation() throws Exception {
        doThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new SQLIntegrityConstraintViolationException(
                        "Cannot delete or update a parent row: a foreign key constraint fails")))
                .when(commandHandler).delete(any());

        mockMvc.perform(delete("/kitchens/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("The operation could not be completed because it conflicts with existing data."));
    }

    /*
     * Filtro por nome: a resposta tem sempre o formato do GET sem filtro (uma lista). Não achar nada
     * não é erro: filtrar uma coleção sem resultado é 200 com lista vazia; 404 é para um recurso
     * específico (GET /kitchens/{id}). Os nomes são únicos, então a lista tem 0 ou 1 elemento.
     */
    @Test
    @DisplayName("GET /kitchens?name=: devolve 200 com uma lista com a cozinha encontrada.")
    void shouldReturnListWithTheKitchenWhenFilteringByName() throws Exception {
        when(queryHandler.findByName("indiana")).thenReturn(List.of(new KitchenResponse(1L, "Indiana")));

        mockMvc.perform(get("/kitchens").param("name", "indiana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Indiana"));
    }

    @Test
    @DisplayName("GET /kitchens?name=: devolve 200 com lista vazia (e não 404) quando nenhuma cozinha tem o nome.")
    void shouldReturnEmptyListWhenNoKitchenHasTheName() throws Exception {
        when(queryHandler.findByName("nope")).thenReturn(List.of());

        mockMvc.perform(get("/kitchens").param("name", "nope"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET /kitchens sem filtro: lista tudo e não consulta por nome.")
    void shouldListAllWhenThereIsNoNameFilter() throws Exception {
        when(queryHandler.findAll()).thenReturn(
                List.of(new KitchenResponse(1L, "Indiana"), new KitchenResponse(2L, "Tailandesa")));

        mockMvc.perform(get("/kitchens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(queryHandler, never()).findByName(any());
    }
}

package dev.denison.api.pedeai;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * TESTE DE FUMAÇA: sobe a aplicação inteira (contexto do Spring, Spring Data, MySQL local) e percorre
 * o caminho feliz de ponta a ponta pelo HTTP. Não substitui os testes unitários; cobre o que eles não
 * enxergam porque mockam as portas: se a projeção do Spring Data devolve o tipo certo, se os mappers
 * e adapters conversam com o banco de verdade, se os beans estão ligados nas configs.
 *
 * Por que existe: ao unificar Summary e Response (etapa 12), um nome de método do Spring Data
 * (findById) fez o GET devolver 500 (ClassCastException) e os 118 testes unitários continuaram
 * verdes. Só rodar o app mostrou. Este teste teria falhado.
 *
 * Requer o MySQL local rodando, como o PedeaiApplicationTests. O ddl-auto do perfil dev recria as
 * tabelas ao subir o contexto (e o import.sql repõe os dados de desenvolvimento), então o teste não
 * depende dos dados de semente: cada cenário cria os próprios dados, com nomes únicos, e os apaga.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Cozinha: criar, ler por id, por nome e na lista, rejeitar duplicada, renomear e excluir.")
    void kitchenLifecycle() throws Exception {
        String name = uniqueName("Smoke Kitchen");
        long id = createKitchen(name);

        mockMvc.perform(get("/kitchens/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));

        // Por nome, sem diferenciar a caixa. O filtro devolve sempre uma lista.
        mockMvc.perform(get("/kitchens").param("name", name.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value(name));

        // Nome que não existe: 200 com lista vazia (404 é só para um recurso específico, como /kitchens/{id}).
        mockMvc.perform(get("/kitchens").param("name", uniqueName("Nope")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/kitchens"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));

        // Mesmo nome (com outra caixa) é duplicado.
        mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name.toLowerCase())))
                .andExpect(status().isConflict());

        String renamed = uniqueName("Smoke Renamed");
        mockMvc.perform(put("/kitchens/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed));
        mockMvc.perform(get("/kitchens/{id}", id))
                .andExpect(jsonPath("$.name").value(renamed));

        mockMvc.perform(delete("/kitchens/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/kitchens/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Restaurante: criar com métodos de pagamento, ler, renomear sem perder os métodos e excluir.")
    void restaurantLifecycle() throws Exception {
        long kitchenId = createKitchen(uniqueName("Smoke Kitchen"));
        String name = uniqueName("Smoke Restaurant");

        MvcResult created = mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "kitchenId": %d, "paymentMethods": ["PIX"]}
                                """.formatted(name, kitchenId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                // CASH é sempre acrescentado na criação (regra do aggregate).
                .andExpect(jsonPath("$.paymentMethods", hasItems("CASH", "PIX")))
                .andReturn();
        long id = idOf(created);

        mockMvc.perform(get("/restaurants/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.kitchenId").value(kitchenId))
                .andExpect(jsonPath("$.paymentMethods", hasItems("CASH", "PIX")));

        mockMvc.perform(get("/restaurants").param("name", name.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));
        mockMvc.perform(get("/restaurants").param("name", uniqueName("Nope")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));

        // Renomear passa por restore -> changeName -> save; os métodos de pagamento têm que sobreviver.
        String renamed = uniqueName("Smoke Renamed");
        mockMvc.perform(put("/restaurants/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", renamed)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.paymentMethods", hasItems("CASH", "PIX")));
        mockMvc.perform(get("/restaurants/{id}", id))
                .andExpect(jsonPath("$.name").value(renamed))
                .andExpect(jsonPath("$.paymentMethods", hasItems("CASH", "PIX")));

        // Cozinha com restaurante vinculado não pode ser excluída (409), só depois que ele sai.
        mockMvc.perform(delete("/kitchens/{id}", kitchenId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/restaurants/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/restaurants/{id}", id))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/kitchens/{id}", kitchenId))
                .andExpect(status().isNoContent());
    }

    private long createKitchen(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", name)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static String json(String field, String value) {
        return "{\"%s\": \"%s\"}".formatted(field, value);
    }

    // Nome único por execução (máx. 50 caracteres): não colide com dados que sobrarem de outro teste.
    private static String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }
}

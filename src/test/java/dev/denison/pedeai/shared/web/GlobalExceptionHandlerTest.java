package dev.denison.pedeai.shared.web;

import dev.denison.pedeai.shared.exception.BusinessException;
import dev.denison.pedeai.shared.exception.ConflictException;
import dev.denison.pedeai.shared.exception.NotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fixa o contrato de erros do {@link GlobalExceptionHandler}: o status e o corpo ({@code ProblemDetail}) de cada caso.
 * <p>
 * Não depende de nenhum módulo. Um controller de teste (o {@link TestController}, aqui dentro) lança cada exceção e
 * declara as constraints de parâmetro, que nenhum endpoint real tem hoje. O {@code @WebMvcTest} carrega o handler e o
 * Bean Validation, sem banco.
 */
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------- exceções da aplicação ----------

    @ParameterizedTest
    @CsvSource({
            "/test/business,  422, Business rule violated",
            "/test/not-found, 404, Thing not found",
            "/test/conflict,  409, Thing already exists"
    })
    void applicationExceptionsKeepTheirStatusAndMessage(String path, int expectedStatus, String message)
            throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.detail").value(message));
    }

    @Test
    void dataIntegrityViolationBecomes409WithoutTheDatabaseMessage() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("The request conflicts with existing data"))
                // A mensagem do banco expõe o SQL e o nome da constraint: nada disso vai para o cliente.
                .andExpect(content().string(not(containsString("Duplicate entry"))))
                .andExpect(content().string(not(containsString("uk_thing_name"))))
                .andExpect(content().string(not(containsString("insert into"))));
    }

    // ---------- validação do corpo (@Valid) ----------

    @Test
    void invalidBodyBecomes400WithErrorsSortedByFieldThenMessage() throws Exception {
        // Declarados na ordem zeta, alpha, multi; "multi" falha em duas constraints. A resposta sai ordenada por
        // campo e, dentro do campo, por mensagem, e não na ordem em que o Bean Validation as achou.
        postJson("/test/body", """
                {"zeta": "", "alpha": "", "multi": ""}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(4)))
                .andExpect(jsonPath("$.errors[*].field", contains("alpha", "multi", "multi", "zeta")))
                .andExpect(jsonPath("$.errors[*].message",
                        contains("alpha is required", "multi is required", "multi is too short", "zeta is required")));
    }

    // ---------- corpo ilegível: a mensagem do Jackson não vai para o cliente ----------

    @Test
    void malformedJsonBecomes400WithAGenericMessage() throws Exception {
        postJson("/test/body", "{\"zeta\": ")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"))
                .andExpect(content().string(not(containsString("JSON parse error"))))
                .andExpect(content().string(not(containsString("Unexpected end-of-input"))));
    }

    @Test
    void unknownEnumValueBecomes400WithoutTheJacksonMessage() throws Exception {
        // A mensagem do Jackson cita o nome interno da classe do enum, o valor recebido e os valores aceitos.
        postJson("/test/enum", """
                {"color": "PURPLE"}""")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Request body is missing or malformed"))
                .andExpect(content().string(not(containsString("Color"))))
                .andExpect(content().string(not(containsString("PURPLE"))))
                .andExpect(content().string(not(containsString("dev.denison"))));
    }

    // ---------- constraint de parâmetro (HandlerMethodValidationException) ----------

    @Test
    void violatedParameterConstraintBecomes400WithTheParameterNameAsField() throws Exception {
        mockMvc.perform(get("/test/params").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field").value("page"))
                .andExpect(jsonPath("$.errors[0].message").value("page must be at least 1"));
    }

    @Test
    void bodyAndParameterErrorsComeTogetherInTheSameFormat() throws Exception {
        // Com constraint de parâmetro no método, o erro do @Valid no corpo chega pelo mesmo caminho: o "field" do corpo
        // é o campo do Request, o do parâmetro é o nome dele, e a lista é ordenada pelos dois juntos.
        mockMvc.perform(post("/test/params-and-body").param("page", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zeta\": \"ok\", \"alpha\": \"\", \"multi\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field", contains("alpha", "page")))
                .andExpect(jsonPath("$.errors[*].message", contains("alpha is required", "page must be at least 1")));
    }

    // ---------- erros do próprio Spring MVC (ResponseEntityExceptionHandler) ----------

    @Test
    void notANumberInThePathBecomes400AsProblemDetail() throws Exception {
        mockMvc.perform(get("/test/numbers/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- apoio ----------

    private ResultActions postJson(String path, String json) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    /** Request de teste. A ordem dos componentes não é a alfabética, de propósito. */
    record Body(
            @NotBlank(message = "zeta is required") String zeta,
            @NotBlank(message = "alpha is required") String alpha,
            @NotBlank(message = "multi is required") @Size(min = 3, message = "multi is too short") String multi) {
    }

    enum Color {RED, GREEN}

    record Painting(Color color) {
    }

    /**
     * Controller só deste teste: lança cada exceção e declara as constraints de parâmetro. Fica registrado pelo
     * {@code @Import} (o filtro do Spring Boot não o deixa entrar na varredura dos outros testes).
     */
    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException("Business rule violated");
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Thing not found");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("Thing already exists");
        }

        @GetMapping("/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException(
                    "could not execute statement [Duplicate entry 'x' for key 'things.uk_thing_name'] "
                            + "[insert into things (name) values (?)]");
        }

        @PostMapping("/body")
        void body(@RequestBody @Valid Body body) {
        }

        @PostMapping("/enum")
        void enumBody(@RequestBody Painting painting) {
        }

        @GetMapping("/params")
        void params(@RequestParam @Min(value = 1, message = "page must be at least 1") int page) {
        }

        @PostMapping("/params-and-body")
        void paramsAndBody(@RequestParam @Min(value = 1, message = "page must be at least 1") int page,
                           @RequestBody @Valid Body body) {
        }

        @GetMapping("/numbers/{id}")
        void number(@PathVariable Long id) {
        }
    }
}

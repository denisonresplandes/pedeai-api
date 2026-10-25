package dev.denison.api.pedeai.presentation.shared;

import dev.denison.api.pedeai.core.application.shared.ConflictException;
import dev.denison.api.pedeai.core.application.shared.ResourceNotFoundException;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * Contrato do GlobalExceptionHandler: cada tipo de exceção vira UMA resposta no formato ProblemDetail
 * (application/problem+json) com status, title, detail e timestamp.
 *
 * Usa o MockMvc "standalone": só o handler e um controller de mentira que lança cada exceção, sem
 * subir o Spring nem os controllers reais. Assim o teste cobre todos os handlers de uma vez e
 * continua valendo se a implementação do handler for reorganizada.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 com a mensagem da exceção.")
    void shouldMapResourceNotFoundToNotFound() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stub/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.detail").value("Thing 1 not found."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("ConflictException -> 409 com a mensagem da exceção.")
    void shouldMapConflictToConflict() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stub/conflict"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail").value("Thing already exists."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("DataIntegrityViolationException -> 409 com mensagem genérica, sem vazar o SQL.")
    void shouldMapDataIntegrityViolationToConflictWithoutLeakingSql() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stub/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Resource Conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("The operation could not be completed because it conflicts with existing data."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("Duplicate entry"))));
    }

    @Test
    @DisplayName("ValidationException -> 400 com a mensagem da exceção.")
    void shouldMapValidationExceptionToBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stub/validation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Application Validation Error"))
                .andExpect(jsonPath("$.detail").value("Request not accepted."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("DomainException -> 400 com a mensagem da exceção.")
    void shouldMapDomainExceptionToBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stub/domain"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Domain Rule Violation"))
                .andExpect(jsonPath("$.detail").value("Rule broken."))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("ConstraintViolationException -> 400 com a mensagem da exceção.")
    void shouldMapConstraintViolationToBadRequest() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/stub/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Parameter Validation Error"))
                .andExpect(jsonPath("$.detail").value("id: must be positive"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException -> 400 com o mapa de erros por campo.")
    void shouldMapMethodArgumentNotValidToBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/stub/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Web Validation Error"))
                .andExpect(jsonPath("$.detail").value("Validation failed for one or more fields"))
                .andExpect(jsonPath("$.errors.name").value("name is required"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /*
     * Corpo ilegível (HttpMessageNotReadableException): JSON malformado, tipo errado, valor de enum
     * que não existe, corpo ausente. Sem handler próprio, o Spring devolve o formato dele
     * ({timestamp, status, error, path}), diferente do resto da API.
     *
     * O detail é genérico DE PROPÓSITO: a mensagem do Jackson cita nomes internos
     * (ex.: "dev.denison.api.pedeai.core.domain.payment.PaymentMethod") e não deve ir para o cliente.
     */
    @ParameterizedTest(name = "corpo: [{0}]")
    @ValueSource(strings = {
            "{",                                          // JSON malformado
            "not json",                                   // nem é JSON
            "{\"name\": \"x\", \"color\": \"PURPLE\"}",   // valor de enum que não existe
            "{\"name\": [\"x\"], \"color\": \"RED\"}",    // tipo incompatível: array onde se espera texto
            ""                                            // corpo ausente
    })
    @DisplayName("HttpMessageNotReadableException -> 400 ProblemDetail genérico, sem vazar nomes internos.")
    void shouldMapUnreadableBodyToBadRequest(String body) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/stub/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Malformed Request Body"))
                .andExpect(jsonPath("$.detail")
                        .value("The request body is missing, malformed or contains an invalid value."))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(content().string(not(containsString("dev.denison"))));
    }

    /*
     * Controller de mentira: cada endpoint lança uma exceção para o handler traduzir.
     */
    @RestController
    static class StubController {

        enum Color { RED }

        record Body(@NotBlank(message = "name is required") String name, Color color) {
        }

        @GetMapping("/stub/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Thing 1 not found.");
        }

        @GetMapping("/stub/conflict")
        void conflict() {
            throw new ConflictException("Thing already exists.");
        }

        @GetMapping("/stub/data-integrity")
        void dataIntegrity() {
            throw new DataIntegrityViolationException("could not execute statement",
                    new SQLIntegrityConstraintViolationException("Duplicate entry 'x' for key 'tb.unq'"));
        }

        @GetMapping("/stub/validation")
        void validation() {
            throw new ValidationException("Request not accepted.");
        }

        @GetMapping("/stub/domain")
        void domain() {
            throw new DomainException("Rule broken.");
        }

        @GetMapping("/stub/constraint")
        void constraint() {
            throw new ConstraintViolationException("id: must be positive", Set.of());
        }

        @PostMapping("/stub/body")
        void body(@Valid @RequestBody Body body) {
        }
    }
}

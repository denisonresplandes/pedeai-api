package dev.denison.api.pedeai.presentation.shared;

import dev.denison.api.pedeai.core.application.shared.ConflictException;
import dev.denison.api.pedeai.core.application.shared.ResourceNotFoundException;
import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/*
 * Cada handler só decide QUAL status, título e mensagem uma exceção merece; montar o ProblemDetail
 * (RFC 9457) é trabalho do helper problem(). Devolver o ProblemDetail direto basta: o Spring usa o
 * status que está nele e responde com application/problem+json.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Resource Conflict", ex.getMessage());
    }

    /*
     * Violações de integridade que só o banco consegue detectar.
     *
     * Duas situações chegam aqui:
     *  - Rede de segurança: o caminho normal já devolve 409 com mensagem específica (ex.:
     *    KitchenNameAlreadyExistsException), mas essas checagens são "ler e depois escrever" em
     *    momentos diferentes. Se duas requisições passam pelo existsByName ao mesmo tempo, as duas
     *    veem "nome livre" e a segunda estoura a constraint única.
     *  - Caminho principal: excluir uma cozinha que ainda tem restaurantes. O handler não consulta
     *    os restaurantes; a FK fk_restaurant_kitchen recusa o DELETE e a resposta é este 409.
     * Só o banco é atômico, por isso ele é a última linha de defesa e aqui traduzimos o erro.
     *
     * - A mensagem do SQL ("Duplicate entry ... for key ...") vai só para o log: ela expõe
     *   nomes de tabelas/constraints e não deve chegar ao cliente.
     * - Limitação: DataIntegrityViolationException também cobre NOT NULL e dado grande demais.
     *   Hoje isso não deve ocorrer (os Requests e os VOs validam antes) e, se ocorrer, indica
     *   um bug; o log com a causa raiz é o que permite descobrir.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(
                HttpStatus.CONFLICT,
                "Resource Conflict",
                "The operation could not be completed because it conflicts with existing data."
        );
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Application Validation Error", ex.getMessage());
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomainException(DomainException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Domain Rule Violation", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ProblemDetail problem = problem(
                HttpStatus.BAD_REQUEST,
                "Web Validation Error",
                "Validation failed for one or more fields"
        );

        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Parameter Validation Error", ex.getMessage());
    }

    /*
     * Corpo da requisição ilegível: JSON malformado, tipo incompatível, valor de enum que não
     * existe (ex.: "method": "FOO"), null dentro de uma lista ou corpo ausente.
     *
     * Sem este handler o Spring responde 400 no formato padrão dele ({timestamp, status, error,
     * path}), diferente do resto da API, e registra o erro em WARN. Ao assumir a exceção, o log
     * passa a ser responsabilidade daqui.
     *
     * O detail é genérico de propósito: a mensagem do Jackson cita nomes internos (pacotes e
     * classes, ex.: "dev.denison...PaymentMethod") que não devem chegar ao cliente. Ela vai só
     * para o log, o mesmo critério do handler de integridade acima.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("Unreadable request body: {}", ex.getMostSpecificCause().getMessage());
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed Request Body",
                "The request body is missing, malformed or contains an invalid value."
        );
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}

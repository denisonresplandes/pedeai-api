package dev.denison.pedeai.shared.web;

import dev.denison.pedeai.shared.exception.BusinessException;
import dev.denison.pedeai.shared.exception.ConflictException;
import dev.denison.pedeai.shared.exception.NotFoundException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Traduz as exceções em respostas HTTP padronizadas ({@link ProblemDetail}, RFC 9457), para todos os controllers.
 * <p>
 * Estende {@link ResponseEntityExceptionHandler}, então os erros do próprio Spring MVC (método não permitido,
 * tipo de mídia não aceito, id que não é número...) também saem como {@code ProblemDetail}.
 * <p>
 * Duas mensagens nunca vão para o cliente: a do Jackson (cita nomes internos de classe) e a do banco (expõe o SQL e o
 * nome das constraints). Nesses casos a resposta traz um texto fixo.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Um erro de validação. {@code field} é o campo do Request, ou o nome do objeto se o erro for do objeto inteiro.
     */
    public record FieldViolation(String field, String message) {
    }

    /** Regra de negócio violada (422). A mensagem é escrita pela aplicação, então vai para o cliente. */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
    }

    /** Recurso inexistente (404). */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Conflito com o estado atual, como um nome já em uso (409). */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflictException(ConflictException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * O banco recusou a operação (FK, NOT NULL ou um UNIQUE que o service não traduziu), por exemplo a exclusão de uma
     * cozinha que tem restaurantes (409). O UNIQUE do nome da cozinha não chega aqui: o {@code KitchenService} o traduz
     * em {@code KitchenNameAlreadyExistsException}. A mensagem do banco não vai para o cliente.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    /**
     * {@code @Valid} recusou o Request (400). A resposta traz a propriedade {@code errors}, uma lista de
     * {@link FieldViolation}. Um campo pode ter mais de um erro, e a ordem do Bean Validation não é garantida, então a
     * lista sai ordenada por campo e mensagem.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Stream<FieldViolation> violations = ex.getBindingResult().getAllErrors().stream()
                .map(GlobalExceptionHandler::toViolation);
        return validationFailed(ex, violations, headers, status, request);
    }

    /**
     * Uma constraint de parâmetro do controller foi recusada (400), como um {@code @Min} num {@code @RequestParam}.
     * Sem este método, o Spring responderia só {@code "Validation failure"}, sem as mensagens. A resposta tem o mesmo
     * formato do {@code @Valid}: {@code field} é o nome do parâmetro (ou o campo, se o parâmetro for um Request
     * validado por dentro).
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status,
                                                                            WebRequest request) {
        Stream<FieldViolation> violations = ex.getParameterValidationResults().stream()
                .flatMap(GlobalExceptionHandler::toViolations);
        return validationFailed(ex, violations, headers, status, request);
    }

    /**
     * Corpo ilegível (400): JSON malformado, corpo ausente ou valor de enum desconhecido. A mensagem do Jackson não
     * vai para o cliente.
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Request body is missing or malformed");
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private ResponseEntity<Object> validationFailed(Exception ex, Stream<FieldViolation> violations,
                                                    HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = violations
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, "Validation failed");
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static FieldViolation toViolation(ObjectError error) {
        String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        return new FieldViolation(field, messageOf(error));
    }

    private static Stream<FieldViolation> toViolations(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            // Parâmetro que é um objeto validado por dentro (um Request): o erro já sabe o campo.
            return parameterErrors.getAllErrors().stream().map(GlobalExceptionHandler::toViolation);
        }
        MethodParameter parameter = result.getMethodParameter();
        String field = parameter.getParameterName() != null
                ? parameter.getParameterName()
                : "arg" + parameter.getParameterIndex();
        return result.getResolvableErrors().stream()
                .map(error -> new FieldViolation(field, messageOf(error)));
    }

    private static String messageOf(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "invalid value";
    }
}

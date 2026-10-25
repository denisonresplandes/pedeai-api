package dev.denison.api.pedeai.core.application.shared;

/*
 * Erro de entrada na camada de aplicação (vira HTTP 400). Hoje cobre dois casos: o Command nulo
 * e o pedido que a aplicação não sabe atender (UnsupportedPaymentMethodException).
 *
 * As checagens de valor (não vazio, positivo, tamanho) NÃO ficam aqui: pertencem ao domínio
 * (DomainException e os VOs) e, na borda da API, ao Request. Ter uma cópia delas nesta camada
 * era a "terceira validação" que só repetia as outras duas.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public static void requireNonNull(Object o, String message) {
        if (o == null) {
            throw new ValidationException(message);
        }
    }
}

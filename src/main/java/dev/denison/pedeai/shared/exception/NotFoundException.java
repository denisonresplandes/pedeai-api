package dev.denison.pedeai.shared.exception;

/**
 * Base das exceções de "não encontrado". Cada módulo cria a sua, por exemplo {@code KitchenNotFoundException}.
 * <p>
 * É lançada pelo service quando um registro não existe, e o {@code GlobalExceptionHandler} a traduz em HTTP 404.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

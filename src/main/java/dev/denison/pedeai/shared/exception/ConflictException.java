package dev.denison.pedeai.shared.exception;

/**
 * Base das exceções de conflito. Cada módulo cria a sua, por exemplo {@code KitchenNameAlreadyExistsException}
 * para um nome que já está em uso.
 * <p>
 * É lançada pelo service quando a operação conflita com o que já existe, e o {@code GlobalExceptionHandler} a
 * traduz em HTTP 409.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

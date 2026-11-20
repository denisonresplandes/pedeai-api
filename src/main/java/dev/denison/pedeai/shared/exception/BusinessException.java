package dev.denison.pedeai.shared.exception;

/**
 * Violação de uma regra de negócio, por exemplo um nome de cozinha curto demais.
 * <p>
 * É lançada pela entidade (a regra mora nela) e o {@code GlobalExceptionHandler} a traduz em HTTP 422.
 * Herda de {@link RuntimeException}, então não precisa de {@code throws} e, quando sai de um método
 * {@code @Transactional}, desfaz a transação.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public static void requireNonNull(Object o, String message) {
        if (o == null) {
            throw new BusinessException(message);
        }
    }

    public static void requirePositive(Long value, String message) {
        requireNonNull(value, message);
        if (value <= 0L) {
            throw new BusinessException(message);
        }
    }
}

package dev.denison.api.pedeai.core.application.payment.exceptions;

import dev.denison.api.pedeai.core.application.shared.ValidationException;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;

/*
 * O cliente pediu um método de pagamento que nenhuma estratégia sabe processar:
 * é um pedido que a aplicação não aceita, então estende ValidationException (400).
 * Antes estendia RuntimeException direto, não casava com nenhum handler e virava 500.
 */
public class UnsupportedPaymentMethodException extends ValidationException {

    public UnsupportedPaymentMethodException(PaymentMethod method) {
        super("Unsupported payment method: %s".formatted(method));
    }
}

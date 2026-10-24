package dev.denison.api.pedeai.core.application.payment.exceptions;

import dev.denison.api.pedeai.core.application.shared.ResourceNotFoundException;

/*
 * Estende a exceção-base de "não encontrado": o GlobalExceptionHandler já traduz
 * ResourceNotFoundException para 404, então esta classe não precisa de handler próprio
 * (igual KitchenNotFoundException e RestaurantNotFoundException).
 */
public class PaymentNotFoundException extends ResourceNotFoundException {

    public PaymentNotFoundException(Long id) {
        super("Payment with id %d not found.".formatted(id));
    }
}

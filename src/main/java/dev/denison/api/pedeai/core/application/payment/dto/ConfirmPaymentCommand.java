package dev.denison.api.pedeai.core.application.payment.dto;

// confirmação para o webhook
public record ConfirmPaymentCommand(
        Long paymentId, // poderia ser PaymentId (VO)
        boolean isSuccess
) {
}

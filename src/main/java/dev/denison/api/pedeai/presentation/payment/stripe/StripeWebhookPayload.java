package dev.denison.api.pedeai.presentation.payment.stripe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StripeWebhookPayload(
        @NotNull(message = "{payment.status.required}")
        StripePayloadStatus status,

        // @Positive sozinho aceita null (a maioria das constraints considera null válido),
        // por isso o @NotNull é necessário para exigir o campo.
        @NotNull(message = "{payment.id.required}")
        @Positive(message = "{payment.id.positive}")
        Long paymentId
) {
}

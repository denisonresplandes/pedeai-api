package dev.denison.api.pedeai.presentation.payment.stripe;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StripeWebhookPayload(
        @NotNull
        StripePayloadStatus status,
        @Positive
        Long paymentId
) {
}

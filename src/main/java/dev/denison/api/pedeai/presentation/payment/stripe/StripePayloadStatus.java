package dev.denison.api.pedeai.presentation.payment.stripe;

/*
 * Status que o Stripe envia no webhook. Cada constante carrega o próprio dado (campo + construtor):
 * quando a diferença entre as constantes é só um valor, isso é mais curto e mais claro que um
 * método abstrato implementado em cada uma (que gera uma classe anônima por constante).
 * O Jackson continua lendo o enum pelo nome ("SUCCESS", "CANCELLED"); o campo não aparece no JSON.
 */
public enum StripePayloadStatus {

    SUCCESS(true),
    CANCELLED(false);

    private final boolean success;

    StripePayloadStatus(boolean success) {
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }
}

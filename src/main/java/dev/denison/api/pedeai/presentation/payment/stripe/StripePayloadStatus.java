package dev.denison.api.pedeai.presentation.payment.stripe;

public enum StripePayloadStatus {

    SUCCESS {
        @Override
        public boolean isSuccess() {
            return true;
        }
    },
    CANCELLED {
        @Override
        public boolean isSuccess() {
            return false;
        }
    };

    public abstract boolean isSuccess();
}

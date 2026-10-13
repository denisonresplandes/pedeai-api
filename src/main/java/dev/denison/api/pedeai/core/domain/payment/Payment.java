package dev.denison.api.pedeai.core.domain.payment;

import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;
import dev.denison.api.pedeai.core.domain.shared.AggregateRoot;
import dev.denison.api.pedeai.core.domain.shared.Money;
import dev.denison.api.pedeai.core.domain.shared.DomainException;

public class Payment extends AggregateRoot {

    private final PaymentId paymentId;
    private final PaymentMethod method; // PIX, CREDIT, etc
    private final Money amount;
    private PaymentStatus status; // PENDING, PAID, etc
    // orderId

    private Payment(PaymentId paymentId, PaymentStatus status, PaymentMethod method, Money amount) {
        DomainException.requireNonNull(status, "Payment status cannot be null");
        DomainException.requireNonNull(method, "Payment method cannot be null");
        DomainException.requireNonNull(amount, "Payment amount cannot be null");

        this.paymentId = paymentId;
        this.status = status;
        this.method = method;
        this.amount = amount;
    }

    public static Payment create(PaymentMethod method, Money amount) {
        return new Payment(
                null,
                PaymentStatus.PENDING, // o estado inicial é sempre PENDING.
                method,
                amount
        );
    }

    // usado apenas pelo Mapper da infraestrutura.
    public static Payment restore(PaymentId paymentId, PaymentStatus status,
                                  PaymentMethod method, Money amount) {
        DomainException.requireNonNull(
                paymentId,
                "Payment ID cannot be null"
        );
        return new Payment(
                paymentId,
                status,
                method,
                amount
        );
    }

    public void markAsPaid() {
        if (PaymentStatus.PAID.equals(status)) {
            throw new DomainException("This payment has already been processed and paid.");
        }
        /*
        * Não pode pagar algo que já foi dado como falho/recusado.
        * */
        if (PaymentStatus.FAILED.equals(status)) {
            throw new DomainException("Failed payments cannot be paid. Start a new transaction.");
        }
        this.status = PaymentStatus.PAID;
    }

    public void markAsFailed() {
        if (PaymentStatus.PAID.equals(status)) {
            throw new DomainException("A payment that has already been approved cannot be marked as failed.");
        }
        this.status = PaymentStatus.FAILED;
    }

    public PaymentId getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public Money getAmount() {
        return amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Payment payment)) {
            return false;
        }
        return paymentId != null && paymentId.equals(payment.paymentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

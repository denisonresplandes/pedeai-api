package dev.denison.api.pedeai.infrastructure.persistence.payment;

import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;
import dev.denison.api.pedeai.core.domain.shared.Money;

import java.util.Objects;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static Payment toDomain(PaymentEntity entity) {
        return Payment.restore(
                new PaymentId(entity.getId()),
                entity.getStatus(),
                entity.getMethod(),
                new Money(entity.getAmount())
        );
    }

    public static PaymentEntity toEntity(Payment payment) {
        Long id = Objects.nonNull(payment.getId()) ?
                payment.getId().value() : null;
        return new PaymentEntity(
                id,
                payment.getMethod(),
                payment.getStatus(),
                payment.getAmount().value()
        );
    }
}

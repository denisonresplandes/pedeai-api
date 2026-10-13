package dev.denison.api.pedeai.infrastructure.persistence.payment;

import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final SpringDataPaymentRepository repository;

    public PaymentRepositoryAdapter(SpringDataPaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entityToSave = PaymentMapper.toEntity(payment);
        PaymentEntity saved = repository.save(entityToSave);
        return PaymentMapper.toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        return repository.findById(paymentId.value())
                .map(PaymentMapper::toDomain);
    }
}

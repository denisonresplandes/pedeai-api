package dev.denison.api.pedeai.infrastructure.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, Long> {
}

package dev.denison.api.pedeai.infrastructure.persistence.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, Long> {
}

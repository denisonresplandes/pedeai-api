package dev.denison.api.pedeai.core.domain.payment;

import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;

import java.util.Optional;

public interface PaymentRepository {
    /*
    * Pode ser dividido, mantendo o padrão (Repository e QueryRepository),
    * caso seja necessário mais métodos de escrita e consulta.
    * */
    Payment save(Payment payment);

    Optional<Payment> findById(PaymentId paymentId);
}

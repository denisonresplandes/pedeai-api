package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * Abre a transação em volta de cada caso de uso de escrita de Payment. O modelo e o raciocínio
 * estão em TransactionalKitchenCommandHandlerDecorator.
 *
 * ATENÇÃO (etapa 17, B10): hoje o processPayment inteiro roda dentro da transação, inclusive a
 * chamada ao gateway (Stripe, Mercado Pago). Isso já era assim com o @Transactional no controller,
 * então esta etapa não muda o comportamento. Corrigir é decidir onde a transação termina, e isso fica
 * fácil aqui: basta o decorator (ou o handler) deixar a chamada externa de fora.
 */
public class TransactionalPaymentCommandHandlerDecorator implements PaymentCommandHandler {

    private final PaymentCommandHandler delegate;
    private final TransactionTemplate transactionTemplate;

    public TransactionalPaymentCommandHandlerDecorator(PaymentCommandHandler delegate,
                                                       TransactionTemplate transactionTemplate) {
        this.delegate = delegate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void processPayment(ProcessPaymentCommand command) {
        transactionTemplate.executeWithoutResult(status -> delegate.processPayment(command));
    }

    @Override
    public void confirmPaymentFromWebhook(ConfirmPaymentCommand command) {
        transactionTemplate.executeWithoutResult(status -> delegate.confirmPaymentFromWebhook(command));
    }
}

package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * Abre a transação em volta dos casos de uso de escrita de Payment. O modelo e o raciocínio estão em
 * TransactionalKitchenCommandHandlerDecorator.
 *
 * Diferença deste decorator: nem todo método é transacional.
 *  - confirmPaymentFromWebhook: lê o pagamento, muda o status e grava. É uma unidade só, então entra
 *    na transação.
 *  - processPayment: NÃO entra, de propósito. Ele grava o pagamento e depois chama o gateway (rede).
 *    Dentro de uma transação, uma chamada externa lenta manteria uma conexão do banco presa esperando.
 *    Sem transação em volta, o save do handler tem a própria transação curta, que termina (commit)
 *    antes de o gateway ser chamado. Quem escolhe onde cada passo roda é o DefaultPaymentCommandHandler.
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
        // Sem transação de propósito (veja o comentário da classe): só repassa a chamada.
        delegate.processPayment(command);
    }

    @Override
    public void confirmPaymentFromWebhook(ConfirmPaymentCommand command) {
        transactionTemplate.executeWithoutResult(status -> delegate.confirmPaymentFromWebhook(command));
    }
}

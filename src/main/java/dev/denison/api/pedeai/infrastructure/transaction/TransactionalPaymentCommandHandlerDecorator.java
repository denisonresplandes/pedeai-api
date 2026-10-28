package dev.denison.api.pedeai.infrastructure.transaction;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import org.springframework.transaction.support.TransactionTemplate;

/*
 * PADRÃO DECORATOR: mesma interface do handler (PaymentCommandHandler) e uma referência para o handler de
 * verdade (DefaultPaymentCommandHandler). Abre a transação em volta dos casos de uso de escrita de Payment
 * que precisam dela. O modelo completo e o raciocínio estão em TransactionalKitchenCommandHandlerDecorator.
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

    /*
     * SEM TRANSAÇÃO, de propósito: só repassa a chamada. O handler grava o pagamento numa transação curta
     * (a do próprio save) e só depois chama o gateway. Embrulhar este método colocaria a chamada de rede
     * dentro da transação do banco, segurando uma conexão enquanto espera o gateway responder.
     */
    @Override
    public void processPayment(ProcessPaymentCommand command) {
        delegate.processPayment(command);
    }

    /*
     * TRANSAÇÃO: ler o pagamento, mudar o status e gravar acontecem numa unidade só. O save (merge) acha o
     * pagamento já carregado na cache de 1º nível: 2 instruções SQL, contra 3 sem a transação (veja
     * TransactionBoundaryTest).
     */
    @Override
    public void confirmPaymentFromWebhook(ConfirmPaymentCommand command) {
        transactionTemplate.executeWithoutResult(status -> delegate.confirmPaymentFromWebhook(command));
    }
}

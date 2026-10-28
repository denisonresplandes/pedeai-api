package dev.denison.api.pedeai.core.application.payment;

import dev.denison.api.pedeai.core.application.payment.dto.ConfirmPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.dto.ProcessPaymentCommand;
import dev.denison.api.pedeai.core.application.payment.exceptions.PaymentNotFoundException;
import dev.denison.api.pedeai.core.application.payment.exceptions.UnsupportedPaymentMethodException;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import dev.denison.api.pedeai.core.domain.payment.vo.PaymentId;

import java.util.List;

/*
 * Implementação padrão do PaymentCommandHandler: só as regras do caso de uso. Ela não sabe o que é
 * transação; veja DefaultKitchenCommandHandler e TransactionalPaymentCommandHandlerDecorator.
 */
public class DefaultPaymentCommandHandler implements PaymentCommandHandler {

    private final PaymentRepository paymentRepository;
    private final List<PaymentProcessorStrategy> strategies;

    public DefaultPaymentCommandHandler(PaymentRepository paymentRepository,
                                        List<PaymentProcessorStrategy> strategies) {
        this.paymentRepository = paymentRepository;
        this.strategies = strategies;
    }

    /*
     * FLUXO 1: O cliente clica em "Pagar" no App.
     *
     * Três passos, nesta ordem:
     *  1. Cria o Payment (só domínio: valida e não acessa o banco) e escolhe a estratégia. Um método que
     *     nenhuma estratégia atende é recusado aqui, antes de qualquer ida ao banco. Antes o pagamento era
     *     gravado primeiro e o rollback da transação desfazia o INSERT (rollback usado como controle de fluxo).
     *  2. Grava o pagamento como PENDING. Este save tem a própria transação curta, que termina (commit)
     *     quando ele retorna: o processPayment NÃO roda dentro de uma transação (veja o decorator).
     *  3. Chama o gateway, já fora de qualquer transação. Uma chamada externa (rede) pode ser lenta, e dentro
     *     da transação ela manteria uma conexão do banco presa esperando.
     *
     * Se o gateway falhar, o pagamento já está gravado como PENDING e a exceção sobe para o cliente. Não
     * marcamos FAILED de propósito: uma falha de rede não diz se o gateway chegou a processar o pedido, e
     * um webhook SUCCESS que chegasse depois seria recusado em um pagamento FAILED. Quem resolve é o
     * webhook (ou uma conciliação futura, veja o TODO).
     */
    @Override
    public void processPayment(ProcessPaymentCommand command) {
        Payment newPayment = Payment.create(command.method(), command.amount());
        PaymentProcessorStrategy processor = findProcessorFor(newPayment.getMethod());

        Payment payment = paymentRepository.save(newPayment);

        processor.process(payment);
    }

    // FLUXO 2: O Stripe avisa de forma assíncrona que o cartão passou
    @Override
    public void confirmPaymentFromWebhook(ConfirmPaymentCommand command) {
        Payment payment = paymentRepository.findById(new PaymentId(command.paymentId()))
                .orElseThrow(() -> new PaymentNotFoundException(command.paymentId()));
        // aplica a intenção no domínio
        if (command.isSuccess()) {
            payment.markAsPaid();
            // TODO disparar um DomainEvent para avisar a Cozinha/Pedido.
        }
        else {
            payment.markAsFailed();
        }
        paymentRepository.save(payment);
    }

    // encontra a estratégia correta (cartão, PIX, etc)
    private PaymentProcessorStrategy findProcessorFor(PaymentMethod method) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(method))
                .findFirst()
                .orElseThrow(() -> new UnsupportedPaymentMethodException(method));
    }
}

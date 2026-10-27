package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.core.application.payment.DefaultPaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import dev.denison.api.pedeai.infrastructure.transaction.TransactionalPaymentCommandHandlerDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Configuration
public class PaymentConfig {

    // Só o decorator vira bean; o porquê está em KitchenConfig.kitchenCommandHandler.
    @Bean
    PaymentCommandHandler paymentCommandHandler(PaymentRepository repository,
                                                List<PaymentProcessorStrategy> strategies,
                                                TransactionTemplate transactionTemplate) {
        PaymentCommandHandler handler = new DefaultPaymentCommandHandler(repository, strategies);
        return new TransactionalPaymentCommandHandlerDecorator(handler, transactionTemplate);
    }
}

package dev.denison.api.pedeai.infrastructure.configs;

import dev.denison.api.pedeai.core.application.payment.PaymentCommandHandler;
import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PaymentConfig {

    @Bean
    PaymentCommandHandler paymentCommandHandler(PaymentRepository repository,
                                                List<PaymentProcessorStrategy> strategies) {
        return new PaymentCommandHandler(repository, strategies);
    }
}

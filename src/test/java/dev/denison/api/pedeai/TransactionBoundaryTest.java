package dev.denison.api.pedeai;

import com.jayway.jsonpath.JsonPath;
import dev.denison.api.pedeai.core.application.payment.PaymentProcessorStrategy;
import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.payment.Payment;
import dev.denison.api.pedeai.core.domain.payment.PaymentRepository;
import dev.denison.api.pedeai.core.domain.payment.PaymentStatus;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.RestaurantRepository;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.core.domain.shared.Money;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/*
 * FRONTEIRA DA TRANSAÇÃO: conta quantas instruções SQL cada caso de uso de escrita executa.
 *
 * Por que contar? Uma transação aberta em volta do caso de uso inteiro mantém a cache de 1º nível
 * (o EntityManager fica preso na thread): o segundo findById da mesma entidade não vai ao banco.
 * Sem essa transação, cada chamada de repositório abre e fecha a sua e a entidade é carregada de novo.
 * O resultado correto dos testes unitários não muda nos dois casos; só o número de idas ao banco muda,
 * então só contar mostra se a transação está no lugar certo.
 *
 * Como conta: o Hibernate guarda estatísticas (generate_statistics) e Statistics#getPrepareStatementCount
 * devolve quantas instruções JDBC foram preparadas desde o último clear().
 *
 * Requer o MySQL local, como o ApiSmokeTest.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
class TransactionBoundaryTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private SpyPixGateway spyGateway;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("PUT /kitchens/{id} (renomear) executa 3 instruções: select, checagem do nome e update.")
    void renamingAKitchenRunsThreeStatements() throws Exception {
        long id = createKitchen(uniqueName("Tx Kitchen"));

        long statements = countStatements(() -> mockMvc.perform(put("/kitchens/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"%s\"}".formatted(uniqueName("Tx Renamed")))));

        assertEquals(3, statements);
        mockMvc.perform(delete("/kitchens/{id}", id)).andExpect(status().isNoContent());
    }

    /*
     * Era 6: o restaurante e seus métodos de pagamento (2 selects) eram carregados duas vezes, porque o
     * findById do handler e o do save rodavam em transações diferentes. Com uma transação só em volta
     * do update, a segunda carga sai da cache: select do restaurante, select dos métodos de pagamento,
     * checagem do nome e update = 4.
     */
    @Test
    @DisplayName("PUT /restaurants/{id} (renomear) executa 4 instruções: o restaurante é carregado uma vez só.")
    void renamingARestaurantRunsFourStatements() throws Exception {
        long kitchenId = createKitchen(uniqueName("Tx Kitchen"));
        long id = createRestaurant(uniqueName("Tx Restaurant"), kitchenId);

        long statements = countStatements(() -> mockMvc.perform(put("/restaurants/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"%s\"}".formatted(uniqueName("Tx Renamed")))));

        assertEquals(4, statements);
        mockMvc.perform(delete("/restaurants/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/kitchens/{id}", kitchenId)).andExpect(status().isNoContent());
    }

    /*
     * O webhook lê o pagamento, muda o status e grava. Dentro da transação do decorator, o save (merge) acha o
     * pagamento já carregado na cache de 1º nível: select + update = 2. Sem a transação, o merge precisaria
     * carregar o pagamento de novo: select + select + update = 3.
     */
    @Test
    @DisplayName("POST /webhooks/stripe (confirmar o pagamento) executa 2 instruções: select e update.")
    void confirmingAPaymentRunsTwoStatements() throws Exception {
        Payment payment = paymentRepository.save(
                Payment.create(PaymentMethod.CREDIT_CARD, new Money(new BigDecimal("30.00"))));

        long statements = countStatements(() -> mockMvc.perform(post("/webhooks/stripe")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"SUCCESS\", \"paymentId\": %d}".formatted(payment.getId().value()))));

        assertEquals(2, statements);
    }

    /*
     * B10: o gateway é uma chamada externa (rede), possivelmente lenta. Se ela rodasse dentro da transação
     * do banco, a conexão ficaria presa esperando por ela. O SpyPixGateway (abaixo) anota, NO INSTANTE em
     * que é chamado, se há transação aberta e se o pagamento já está visível no banco. Sem transação aberta,
     * só enxerga o pagamento se ele já foi gravado de verdade (commit).
     */
    @Test
    @DisplayName("POST /payments: o gateway é chamado depois do commit do pagamento e fora de qualquer transação.")
    void gatewayIsCalledAfterTheCommitAndOutsideAnyTransaction() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 10.50, \"method\": \"PIX\"}"))
                .andExpect(status().isAccepted());

        assertFalse(spyGateway.transactionActiveWhenCalled, "o gateway rodou dentro de uma transação");
        assertNotNull(spyGateway.statusFoundInDatabaseWhenCalled, "o pagamento ainda não estava gravado");
        assertEquals(PaymentStatus.PENDING, spyGateway.statusFoundInDatabaseWhenCalled);
    }

    /*
     * O save do RestaurantRepositoryAdapter altera uma entidade gerenciada e conta com o COMMIT para o
     * Hibernate gerar o UPDATE. Fora de uma transação a alteração se perderia sem erro nenhum; por isso o
     * método é @Transactional(MANDATORY): chamado sem transação, falha na hora.
     * Aqui o repositório é chamado direto (sem o decorator), justamente para provar esse guarda.
     */
    @Test
    @DisplayName("RestaurantRepository.save falha quando chamado sem transação (em vez de perder o update em silêncio).")
    void restaurantSaveRequiresAnOpenTransaction() {
        Restaurant existing = Restaurant.restore(
                new RestaurantId(1L), new RestaurantName("Ilha Verde"), new KitchenId(1L), Set.of(PaymentMethod.CASH));

        assertThrows(IllegalTransactionStateException.class, () -> restaurantRepository.save(existing));
    }

    private long countStatements(ThrowingRequest request) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        request.perform().andExpect(status().isOk());
        return statistics.getPrepareStatementCount();
    }

    private long createKitchen(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/kitchens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private long createRestaurant(String name, long kitchenId) throws Exception {
        MvcResult result = mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\", \"kitchenId\": %d, \"paymentMethods\": [\"PIX\"]}"
                                .formatted(name, kitchenId)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    // Nome único por execução (máx. 50 caracteres): não colide com dados que sobrarem de outro teste.
    private static String uniqueName(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    /*
     * Gateway de mentira para o método PIX. Tem precedência máxima, então o handler o escolhe no lugar do
     * processador real de PIX (a lista de estratégias é percorrida na ordem e vence o primeiro que suporta).
     */
    @TestConfiguration
    static class SpyGatewayConfiguration {
        @Bean
        SpyPixGateway spyPixGateway(PaymentRepository paymentRepository) {
            return new SpyPixGateway(paymentRepository);
        }
    }

    static class SpyPixGateway implements PaymentProcessorStrategy, Ordered {

        private final PaymentRepository paymentRepository;
        volatile boolean transactionActiveWhenCalled = true;
        volatile PaymentStatus statusFoundInDatabaseWhenCalled;

        SpyPixGateway(PaymentRepository paymentRepository) {
            this.paymentRepository = paymentRepository;
        }

        @Override
        public boolean supports(dev.denison.api.pedeai.core.domain.payment.PaymentMethod method) {
            return method == dev.denison.api.pedeai.core.domain.payment.PaymentMethod.PIX;
        }

        @Override
        public void process(Payment payment) {
            transactionActiveWhenCalled = TransactionSynchronizationManager.isActualTransactionActive();
            statusFoundInDatabaseWhenCalled = paymentRepository.findById(payment.getId())
                    .map(Payment::getStatus)
                    .orElse(null);
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    @FunctionalInterface
    private interface ThrowingRequest {
        ResultActions perform() throws Exception;
    }
}

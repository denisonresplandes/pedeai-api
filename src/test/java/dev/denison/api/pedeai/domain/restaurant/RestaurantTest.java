package dev.denison.api.pedeai.domain.restaurant;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.Restaurant;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.core.domain.shared.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * Teste de domínio puro (sem Spring/Mockito/banco).
 *
 * O null precisa ser barrado pelo próprio aggregate. Hoje o Jackson já recusa "null" dentro de um
 * array do JSON, então pelo HTTP ele não chega aqui; mas o domínio não pode depender disso: outro
 * caso de uso, um teste ou uma mudança de configuração do Jackson o deixariam passar, e o null
 * só estouraria no banco (coluna payment_method é NOT NULL) como um erro de integridade sem sentido.
 *
 * create() e restore() têm papéis diferentes:
 * - create: um restaurante NOVO; a regra de negócio "CASH é obrigatório" é aplicada aqui.
 * - restore: reconstrói o que JÁ está no banco, sem inventar nem corrigir dados. Antes, o construtor
 *   forçava CASH também aqui, então o estado em memória podia divergir do que estava persistido.
 */
class RestaurantTest {

    private static final RestaurantName NAME = new RestaurantName("Ilha Verde");
    private static final KitchenId KITCHEN_ID = new KitchenId(1L);

    private static Restaurant restaurantWithCashOnly() {
        return Restaurant.restore(new RestaurantId(1L), NAME, KITCHEN_ID, Set.of(PaymentMethod.CASH));
    }

    @Test
    @DisplayName("create: deve incluir CASH mesmo quando ele não for informado.")
    void createShouldAlwaysIncludeCash() {
        Restaurant restaurant = Restaurant.create(NAME, KITCHEN_ID, Set.of(PaymentMethod.PIX));

        assertEquals(Set.of(PaymentMethod.CASH, PaymentMethod.PIX), restaurant.getPaymentMethods());
        assertNull(restaurant.getId());
    }

    @Test
    @DisplayName("create: deve aceitar um conjunto vazio e ficar só com CASH.")
    void createShouldAcceptEmptyPaymentMethods() {
        Restaurant restaurant = Restaurant.create(NAME, KITCHEN_ID, Set.of());

        assertEquals(Set.of(PaymentMethod.CASH), restaurant.getPaymentMethods());
    }

    @Test
    @DisplayName("create: deve rejeitar o conjunto de métodos nulo.")
    void createShouldRejectNullPaymentMethods() {
        DomainException ex = assertThrows(DomainException.class,
                () -> Restaurant.create(NAME, KITCHEN_ID, null));

        assertEquals("Payment methods cannot be null.", ex.getMessage());
    }

    @Test
    @DisplayName("create: deve rejeitar um null dentro do conjunto de métodos.")
    void createShouldRejectNullElement() {
        // HashSet aceita null; Set.of() não aceitaria (lançaria NPE antes de chegar ao domínio).
        Set<PaymentMethod> withNull = new HashSet<>();
        withNull.add(PaymentMethod.PIX);
        withNull.add(null);

        DomainException ex = assertThrows(DomainException.class,
                () -> Restaurant.create(NAME, KITCHEN_ID, withNull));

        assertEquals("Payment method cannot be null.", ex.getMessage());
    }

    @Test
    @DisplayName("create: deve copiar o conjunto recebido, sem compartilhar a referência do chamador.")
    void createShouldCopyThePaymentMethods() {
        Set<PaymentMethod> fromCaller = new HashSet<>(Set.of(PaymentMethod.PIX));
        Restaurant restaurant = Restaurant.create(NAME, KITCHEN_ID, fromCaller);

        fromCaller.add(PaymentMethod.CREDIT_CARD);

        assertEquals(Set.of(PaymentMethod.CASH, PaymentMethod.PIX), restaurant.getPaymentMethods());
    }

    @Test
    @DisplayName("restore: deve manter exatamente os métodos persistidos, sem forçar CASH.")
    void restoreShouldKeepExactlyThePersistedPaymentMethods() {
        Restaurant restaurant = Restaurant.restore(
                new RestaurantId(1L), NAME, KITCHEN_ID, Set.of(PaymentMethod.PIX));

        assertEquals(Set.of(PaymentMethod.PIX), restaurant.getPaymentMethods());
    }

    @Test
    @DisplayName("restore: deve rejeitar o conjunto de métodos nulo.")
    void restoreShouldRejectNullPaymentMethods() {
        DomainException ex = assertThrows(DomainException.class,
                () -> Restaurant.restore(new RestaurantId(1L), NAME, KITCHEN_ID, null));

        assertEquals("Payment methods cannot be null.", ex.getMessage());
    }

    @Test
    @DisplayName("restore: deve rejeitar um null dentro do conjunto de métodos.")
    void restoreShouldRejectNullElement() {
        Set<PaymentMethod> withNull = new HashSet<>();
        withNull.add(null);

        DomainException ex = assertThrows(DomainException.class,
                () -> Restaurant.restore(new RestaurantId(1L), NAME, KITCHEN_ID, withNull));

        assertEquals("Payment method cannot be null.", ex.getMessage());
    }

    @Test
    @DisplayName("restore: deve rejeitar id nulo.")
    void restoreShouldRejectNullId() {
        assertThrows(DomainException.class,
                () -> Restaurant.restore(null, NAME, KITCHEN_ID, Set.of(PaymentMethod.CASH)));
    }

    @Test
    @DisplayName("Deve rejeitar null ao adicionar um método de pagamento.")
    void shouldRejectNullWhenAddingPaymentMethod() {
        Restaurant restaurant = restaurantWithCashOnly();

        DomainException ex = assertThrows(DomainException.class, () -> restaurant.addPaymentMethod(null));

        assertEquals("Payment method cannot be null.", ex.getMessage());
        assertEquals(Set.of(PaymentMethod.CASH), restaurant.getPaymentMethods());
    }

    @Test
    @DisplayName("Deve rejeitar null ao remover um método de pagamento.")
    void shouldRejectNullWhenRemovingPaymentMethod() {
        Restaurant restaurant = restaurantWithCashOnly();

        DomainException ex = assertThrows(DomainException.class, () -> restaurant.removePaymentMethod(null));

        assertEquals("Payment method cannot be null.", ex.getMessage());
    }

    @Test
    @DisplayName("Não deve permitir remover CASH, que é obrigatório.")
    void shouldNotAllowRemovingCash() {
        Restaurant restaurant = restaurantWithCashOnly();

        assertThrows(DomainException.class, () -> restaurant.removePaymentMethod(PaymentMethod.CASH));

        assertEquals(Set.of(PaymentMethod.CASH), restaurant.getPaymentMethods());
    }

    @Test
    @DisplayName("Deve adicionar e remover um método de pagamento comum.")
    void shouldAddAndRemoveRegularPaymentMethod() {
        Restaurant restaurant = restaurantWithCashOnly();

        restaurant.addPaymentMethod(PaymentMethod.PIX);
        assertEquals(Set.of(PaymentMethod.CASH, PaymentMethod.PIX), restaurant.getPaymentMethods());

        restaurant.removePaymentMethod(PaymentMethod.PIX);
        assertEquals(Set.of(PaymentMethod.CASH), restaurant.getPaymentMethods());
    }

    @Test
    @DisplayName("Deve expor os métodos de pagamento como uma visão não modificável.")
    void shouldExposeUnmodifiablePaymentMethods() {
        Restaurant restaurant = restaurantWithCashOnly();

        assertThrows(UnsupportedOperationException.class,
                () -> restaurant.getPaymentMethods().add(PaymentMethod.PIX));
    }
}

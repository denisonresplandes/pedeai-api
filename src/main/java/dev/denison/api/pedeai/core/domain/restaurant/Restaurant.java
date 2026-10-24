package dev.denison.api.pedeai.core.domain.restaurant;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.core.domain.shared.AggregateRoot;
import dev.denison.api.pedeai.core.domain.shared.DomainException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Restaurant extends AggregateRoot {

    private final RestaurantId id;
    private RestaurantName name;

    private KitchenId kitchenId;

    private Set<PaymentMethod> paymentMethods = new HashSet<>();

    private Restaurant(RestaurantId id, RestaurantName name, KitchenId kitchenId) {
        DomainException.requireNonNull(
                name,
                "Restaurant name cannot be null."
        );
        DomainException.requireNonNull(
                kitchenId,
                "Kitchen ID cannot be null"
        );
        this.id = id;
        this.name = name;
        this.kitchenId = kitchenId;
        this.paymentMethods.add(PaymentMethod.CASH);
    }

    public static Restaurant create(RestaurantName name,
                                    KitchenId kitchenId) {
        return new Restaurant(null, name, kitchenId);
    }

    public static Restaurant restore(RestaurantId id,
                                     RestaurantName name,
                                     KitchenId kitchenId) {
        DomainException.requireNonNull(
                id,
                "Restaurant id cannot be null."
        );
        return new Restaurant(id, name, kitchenId);
    }

    public RestaurantId getId() {
        return id;
    }

    public RestaurantName getName() {
        return name;
    }

    public void changeName(RestaurantName newName) {
        DomainException.requireNonNull(
                newName,
                "The new restaurant name cannot be null."
        );
        this.name = newName;
    }

    public KitchenId getKitchenId() {
        return kitchenId;
    }

    public void changeKitchen(KitchenId kitchenId) {
        DomainException.requireNonNull(
                kitchenId,
                "Kitchen ID cannot be null"
        );
        this.kitchenId = kitchenId;
    }

    public Set<PaymentMethod> getPaymentMethods() {
        return Collections.unmodifiableSet(paymentMethods);
    }

    public void addPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethods.add(paymentMethod);
    }

    public void removePaymentMethod(PaymentMethod paymentMethod) {
        if (PaymentMethod.CASH.equals(paymentMethod)) {
            throw new DomainException("CASH is mandatory and cannot be removed from the accepted payment methods.");
        }
        this.paymentMethods.remove(paymentMethod);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Restaurant restaurant)) {
            return false;
        }
        return id != null && id.equals(restaurant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

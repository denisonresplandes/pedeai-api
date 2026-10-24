package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.core.domain.payment.PaymentMethod;
import dev.denison.api.pedeai.infrastructure.persistence.kitchen.KitchenEntity;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tb_restaurant", uniqueConstraints = {
        @UniqueConstraint(name = "unq_restaurant_name", columnNames = "name")
})
@DynamicUpdate
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = RestaurantName.MAX_LENGTH)
    private String name;

    @ManyToOne
    @JoinColumn(
            name = "kitchen_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_restaurant_kitchen")
    )
    private KitchenEntity kitchen;

    @ElementCollection
    @CollectionTable(
            name = "tb_restaurant_payment_method",
            joinColumns = @JoinColumn(
                    name = "restaurant_id",
                    foreignKey = @ForeignKey(name = "fk_restaurant_payment_method")
            )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private Set<PaymentMethod> paymentMethods = new HashSet<>();

    RestaurantEntity() {}

    public RestaurantEntity(Long id, String name, KitchenEntity kitchen) {
        this.id = id;
        this.name = name;
        this.kitchen = kitchen;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KitchenEntity getKitchen() {
        return kitchen;
    }

    public void setKitchen(KitchenEntity kitchen) {
        this.kitchen = kitchen;
    }

    public Set<PaymentMethod> getPaymentMethods() {
        return Collections.unmodifiableSet(this.paymentMethods);
    }

    public void addPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethods.add(paymentMethod);
    }

    public void removePaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethods.remove(paymentMethod);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RestaurantEntity restaurant)) {
            return false;
        }
        return id != null && id.equals(restaurant.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

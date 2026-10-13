package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import dev.denison.api.pedeai.infrastructure.persistence.kitchen.KitchenEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "tb_restaurant", uniqueConstraints = {
        @UniqueConstraint(name = "unq_restaurant_name", columnNames = "name")
})
@DynamicUpdate
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @ManyToOne
    @JoinColumn(
            name = "kitchen_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_restaurant_kitchen")
    )
    private KitchenEntity kitchen;

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

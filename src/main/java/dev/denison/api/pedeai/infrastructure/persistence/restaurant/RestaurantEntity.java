package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_restaurant",
        uniqueConstraints = {
            @UniqueConstraint(name = "unq_restaurant_name", columnNames = "name")
        }
)
public class RestaurantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    RestaurantEntity() {}

    public RestaurantEntity(Long id, String name) {
        this.id = id;
        this.name = name;
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

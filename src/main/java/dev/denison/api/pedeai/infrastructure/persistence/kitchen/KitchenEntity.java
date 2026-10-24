package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import dev.denison.api.pedeai.core.domain.kitchen.vo.KitchenName;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_kitchen", uniqueConstraints = {
            @UniqueConstraint(name = "unq_kitchen_name", columnNames = "name")
}) // TODO add flyway depois.
public class KitchenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = KitchenName.MAX_LENGTH)
    private String name;

    KitchenEntity() {}

    public KitchenEntity(Long id) {
        this.id = id;
    }

    public KitchenEntity(Long id, String name) {
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
        if (!(o instanceof KitchenEntity kitchen)) {
            return false;
        }
        return id != null && id.equals(kitchen.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

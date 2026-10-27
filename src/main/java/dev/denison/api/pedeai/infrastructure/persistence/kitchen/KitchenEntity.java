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

    // A unicidade está só no @UniqueConstraint da classe (dá nome à constraint); "unique = true"
    // aqui geraria a mesma constraint, então seria repetição.
    @Column(nullable = false, length = KitchenName.MAX_LENGTH)
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

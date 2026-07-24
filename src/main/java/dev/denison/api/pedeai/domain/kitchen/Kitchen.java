package dev.denison.api.pedeai.domain.kitchen;

import dev.denison.api.pedeai.domain.kitchen.vo.KitchenId;
import dev.denison.api.pedeai.domain.kitchen.vo.KitchenName;
import dev.denison.api.pedeai.domain.shared.AggregateRoot;
import dev.denison.api.pedeai.domain.shared.DomainException;

public class Kitchen extends AggregateRoot {

    private final KitchenId id;
    private final KitchenName name;

    private Kitchen(KitchenId id, KitchenName name) {
        DomainException.requireNonNull(
                name,
                "Kitchen name cannot be null."
        );
        this.id = id;
        this.name = name;
    }

    public static Kitchen create(KitchenName name) {
        return new Kitchen(null, name);
    }

    public static Kitchen restore(KitchenId id, KitchenName name) {
        DomainException.requireNonNull(
                id,
                "Kitchen ID cannot be null."
        );
        return new Kitchen(id, name);
    }

    public KitchenId getId() {
        return id;
    }

    public KitchenName getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Kitchen kitchen)) {
            return false;
        }
        return id != null && id.equals(kitchen.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package dev.denison.api.pedeai.core.domain.restaurant;

import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantId;
import dev.denison.api.pedeai.core.domain.restaurant.vo.RestaurantName;
import dev.denison.api.pedeai.core.domain.shared.AggregateRoot;
import dev.denison.api.pedeai.core.domain.shared.DomainException;

public class Restaurant extends AggregateRoot {

    private final RestaurantId id;
    private RestaurantName name;

    private Restaurant(RestaurantId id, RestaurantName name) {
        DomainException.requireNonNull(
                name,
                "Restaurant name cannot be null."
        );
        this.id = id;
        this.name = name;
    }

    public static Restaurant create(RestaurantName name) {
        return new Restaurant(null, name);
    }

    public static Restaurant restore(RestaurantId id, RestaurantName name) {
        DomainException.requireNonNull(
                id,
                "Restaurant id cannot be null."
        );
        return new Restaurant(id, name);
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

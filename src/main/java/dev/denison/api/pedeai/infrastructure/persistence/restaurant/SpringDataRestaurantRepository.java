package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRestaurantRepository extends JpaRepository<RestaurantEntity, Long> {

    boolean existsByNameIgnoreCase(String name);
}

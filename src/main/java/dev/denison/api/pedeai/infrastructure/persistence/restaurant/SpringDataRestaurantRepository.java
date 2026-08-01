package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRestaurantRepository extends JpaRepository<RestaurantEntity, Long> {

    boolean existsByName(String name);
}

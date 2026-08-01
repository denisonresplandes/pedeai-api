package dev.denison.api.pedeai.infrastructure.persistence.kitchen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataKitchenRepository extends JpaRepository<KitchenEntity, Long> {

    boolean existsByName(String name);
}

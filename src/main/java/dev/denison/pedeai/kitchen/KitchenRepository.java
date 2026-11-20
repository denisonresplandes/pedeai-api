package dev.denison.pedeai.kitchen;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface KitchenRepository extends JpaRepository<Kitchen, Long> {

    boolean existsByNameIgnoreCase(String name);
}

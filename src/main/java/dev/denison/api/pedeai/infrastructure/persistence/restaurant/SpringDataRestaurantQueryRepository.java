package dev.denison.api.pedeai.infrastructure.persistence.restaurant;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

public interface SpringDataRestaurantQueryRepository extends Repository<RestaurantEntity, Long> {

    @Query("""
        SELECT r FROM RestaurantEntity r
            JOIN FETCH r.kitchen
            LEFT JOIN FETCH r.paymentMethods
        """)
    List<RestaurantEntity> findAll();

    @Query("""
        SELECT r FROM RestaurantEntity r
            JOIN FETCH r.kitchen
            LEFT JOIN FETCH r.paymentMethods
        WHERE r.id = :id
    """)
    Optional<RestaurantEntity> findById(Long id);

    @Query("""
        SELECT r FROM RestaurantEntity r
            JOIN FETCH r.kitchen
            LEFT JOIN FETCH r.paymentMethods
        WHERE LOWER(r.name) = LOWER(:name)
    """)
    Optional<RestaurantEntity> findByNameIgnoreCase(String name);

    boolean existsByKitchenId(Long kitchenId);

}

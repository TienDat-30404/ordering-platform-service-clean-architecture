package com.example.demo.adapters.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // import đúng
import org.springframework.data.repository.query.Param; // import đúng
import com.example.demo.adapters.out.persistence.entity.RestaurantJpaEntity;

public interface RestaurantJpaRepository extends JpaRepository<RestaurantJpaEntity, Long> {

    @Query("""
            select r from RestaurantJpaEntity r
            left join fetch r.menu
            where r.id = :id
            """)
    Optional<RestaurantJpaEntity> findByIdWithMenu(@Param("id") Long id);

    @Override
    @EntityGraph(attributePaths = "menu")
    Optional<RestaurantJpaEntity> findById(Long id);
}

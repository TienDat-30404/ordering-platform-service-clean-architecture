package com.example.demo.adapters.out.persistence.repository;

import com.example.demo.adapters.out.persistence.entity.MenuItemJpaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, Long> {

    List<MenuItemJpaEntity> findByIdIn(List<Long> ids);

    // Giảm số lượng nếu còn đủ hàng
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update MenuItemJpaEntity m
              set m.quantity = m.quantity - :qty
            where m.id = :id and m.quantity >= :qty
           """)
    int deductStock(@Param("id") Long id, @Param("qty") int qty);
}

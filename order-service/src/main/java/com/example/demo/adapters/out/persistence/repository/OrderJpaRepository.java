package com.example.demo.adapters.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.adapters.out.persistence.dto.OrderStatisticsJpaProjection;
import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {
    List<OrderJpaEntity> findByUserId(Long userId);

    @Query("SELECT " +
            "  COUNT(o.id) AS totalOrders, " +
            "  SUM(o.amount) AS totalRevenue, " +
            "  AVG(o.amount) AS averageOrderValue " +
            "FROM OrderJpaEntity o")
    OrderStatisticsJpaProjection getOrderStatistics();
}
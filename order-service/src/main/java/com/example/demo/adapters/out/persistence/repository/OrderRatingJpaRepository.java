package com.example.demo.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.adapters.out.persistence.entity.OrderRatingJpaEntity;

public interface OrderRatingJpaRepository extends JpaRepository<OrderRatingJpaEntity, Long> {
    
}

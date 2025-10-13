package com.example.demo.adapters.out.persistence.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.adapters.out.persistence.entity.RestaurantJpaEntity;

public interface RestaurantJpaRepository extends JpaRepository<RestaurantJpaEntity, Long> {

}
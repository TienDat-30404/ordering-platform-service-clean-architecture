package com.example.user_service.adapters.out.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_service.adapters.out.persistence.entity.UserJpaEntity;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByUserName(String userName);
}

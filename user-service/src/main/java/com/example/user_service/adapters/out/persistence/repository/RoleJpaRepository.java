package com.example.user_service.adapters.out.persistence.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_service.adapters.out.persistence.entity.RoleJpaEntity;

public interface RoleJpaRepository extends JpaRepository<RoleJpaEntity, Long> {

}

package com.example.user_service.adapters.out.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.user_service.adapters.out.persistence.entity.RoleJpaEntity;
import com.example.user_service.adapters.out.persistence.mapper.RolePersistenceMapper;
import com.example.user_service.adapters.out.persistence.repository.RoleJpaRepository;
import com.example.user_service.application.ports.output.repository.RoleRepositoryPort;
import com.example.user_service.domain.entity.Role;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository roleJpaRepository;
    private final RolePersistenceMapper roleMapper;

    public Optional<Role> findById(Long id) {
        Optional<RoleJpaEntity> roleJpaEntity = roleJpaRepository.findById(id);
        return roleJpaEntity.map(roleMapper::toDomainEntity); 
    }

}

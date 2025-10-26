package com.example.user_service.application.ports.output.repository;

import java.util.Optional;

import com.example.user_service.domain.entity.Role;

public interface RoleRepositoryPort {
    Optional<Role> findById(Long id);
}

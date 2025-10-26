package com.example.user_service.application.ports.output.repository;

import java.util.Optional;

import com.example.user_service.domain.entity.User;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findByUserName(String userName);

}

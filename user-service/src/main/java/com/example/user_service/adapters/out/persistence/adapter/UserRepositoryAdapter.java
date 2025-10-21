package com.example.user_service.adapters.out.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.user_service.adapters.out.persistence.entity.UserJpaEntity;
import com.example.user_service.adapters.out.persistence.mapper.UserPersistenceMapper;
import com.example.user_service.adapters.out.persistence.repository.UserJpaRepository;
import com.example.user_service.application.ports.output.repository.UserRepositoryPort;
import com.example.user_service.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {
    private final UserJpaRepository userJpaRepository;
    private final UserPersistenceMapper userPersistenceMapper;

    public User save(User user) {
        UserJpaEntity userJpaEntity = userPersistenceMapper.toJpaEntity(user);
        UserJpaEntity savedEntity = userJpaRepository.save(userJpaEntity);
        return userPersistenceMapper.toDomainEntity(savedEntity);
    }

    public Optional<User> findByUserName(String userName) {
        Optional<UserJpaEntity> userJpaEntity = userJpaRepository.findByUserName(userName);
        return userJpaEntity.map(userPersistenceMapper::toDomainEntity);
    }

}

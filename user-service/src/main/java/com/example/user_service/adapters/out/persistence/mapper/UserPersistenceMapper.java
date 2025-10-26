package com.example.user_service.adapters.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.example.user_service.adapters.out.persistence.entity.RoleJpaEntity;
import com.example.user_service.adapters.out.persistence.entity.UserJpaEntity;
import com.example.user_service.domain.entity.Role;
import com.example.user_service.domain.entity.User;
import com.example.user_service.domain.valueobject.UserId;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserPersistenceMapper {
    private final RolePersistenceMapper roleMapper;

    public UserJpaEntity toJpaEntity(User user) {
        UserJpaEntity jpaEntity = new UserJpaEntity();
        jpaEntity.setName(user.getName());
        jpaEntity.setUserName(user.getUserName());
        jpaEntity.setPassword(user.getPassword());

        RoleJpaEntity roleJpaEntity = new RoleJpaEntity(
            user.getRole().getId().value(), 
            user.getRole().getName()
        );
        jpaEntity.setRole(roleJpaEntity);
        return jpaEntity;
    }

    public User toDomainEntity(UserJpaEntity jpaEntity) {
        UserId userId = new UserId(jpaEntity.getId());
        Role role = roleMapper.toDomainEntity(jpaEntity.getRole());
        return new User(
                userId,
                jpaEntity.getName(),
                jpaEntity.getUserName(),
                jpaEntity.getPassword(),
                role
        );
    }
}

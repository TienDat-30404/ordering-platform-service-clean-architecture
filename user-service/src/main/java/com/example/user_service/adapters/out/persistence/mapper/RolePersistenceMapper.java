package com.example.user_service.adapters.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.example.user_service.adapters.out.persistence.entity.RoleJpaEntity;
import com.example.user_service.adapters.out.persistence.entity.UserJpaEntity;
import com.example.user_service.domain.entity.Role;
import com.example.user_service.domain.entity.User;
import com.example.user_service.domain.valueobject.RoleId;
import com.example.user_service.domain.valueobject.UserId;

@Component
public class RolePersistenceMapper {
    public Role toDomainEntity(RoleJpaEntity jpaEntity) {
        RoleId roleId = new RoleId(jpaEntity.getId());
        return new Role(
                roleId,
                jpaEntity.getName()
        );
    }
}

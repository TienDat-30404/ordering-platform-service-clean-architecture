package com.example.user_service.application.mapper;

import com.example.user_service.application.dto.command.RegisterCommand;
import com.example.user_service.application.dto.output.UserResponse;
import com.example.user_service.domain.entity.Role;
import com.example.user_service.domain.entity.User;

public interface UserMapper {
    UserResponse toDTO(User user);
    User toDomain(RegisterCommand command, Role role);
}

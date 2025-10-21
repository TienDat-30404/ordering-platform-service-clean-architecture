package com.example.user_service.application.usecases;

import org.springframework.stereotype.Service;

import com.example.user_service.application.dto.command.RegisterCommand;
import com.example.user_service.application.dto.output.UserResponse;
import com.example.user_service.application.mapper.UserMapper;
import com.example.user_service.application.ports.input.RegisterUseCase;
import com.example.user_service.application.ports.output.repository.RoleRepositoryPort;
import com.example.user_service.application.ports.output.repository.UserRepositoryPort;
import com.example.user_service.domain.entity.Role;
import com.example.user_service.domain.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterUseCaseImpl implements RegisterUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final UserMapper userMapper;
    public UserResponse register(RegisterCommand command) {

        Role defaultRole = roleRepositoryPort.findById(command.getRoleId())
                            .orElseThrow(() -> new IllegalArgumentException("Role ID không hợp lệ hoặc không tồn tại."));
        User user = userMapper.toDomain(command, defaultRole);
        System.out.println("defaultRole" + defaultRole);
        User savedUser = userRepositoryPort.save(user);
        return userMapper.toDTO(savedUser);
    }
}

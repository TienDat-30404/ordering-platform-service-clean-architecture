package com.example.user_service.application.usecases;

import java.security.InvalidAlgorithmParameterException;

import org.springframework.stereotype.Service;

import com.example.user_service.application.dto.command.LoginCommand;
import com.example.user_service.application.dto.output.AuthResponse;
import com.example.user_service.application.dto.output.UserResponse;
import com.example.user_service.application.mapper.UserMapper;
import com.example.user_service.application.ports.input.LoginUseCase;
import com.example.user_service.application.ports.output.TokenGeneratorPort;
import com.example.user_service.application.ports.output.repository.UserRepositoryPort;
import com.example.user_service.domain.entity.User;
import com.example.user_service.domain.exception.UserNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {
    private final UserRepositoryPort userRepositoryPort;
    private final UserMapper userMapper;
    private final TokenGeneratorPort tokenGenerator; 

    public AuthResponse<UserResponse> login(LoginCommand command) {

        String userName = command.getUserName();
        String password = command.getPassword();

        User user = userRepositoryPort.findByUserName(userName)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // if (!password.equals(user.getPassword())) {
        //     // Nếu hai chuỗi KHÔNG khớp nhau
        //     throw new InvalidAlgorithmParameterException("Mật khẩu không hợp lệ");
        // }
        UserResponse response = userMapper.toDTO(user);

        String accessToken = tokenGenerator.generateAccessToken(user.getId().value(), user.getRole().getName());
     

        return new AuthResponse<>(accessToken, response);
    }
}

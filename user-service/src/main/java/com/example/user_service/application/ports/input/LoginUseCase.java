package com.example.user_service.application.ports.input;

import com.example.user_service.application.dto.command.LoginCommand;
import com.example.user_service.application.dto.output.AuthResponse;
import com.example.user_service.application.dto.output.UserResponse;

public interface LoginUseCase {
    AuthResponse<UserResponse> login(LoginCommand command);
}

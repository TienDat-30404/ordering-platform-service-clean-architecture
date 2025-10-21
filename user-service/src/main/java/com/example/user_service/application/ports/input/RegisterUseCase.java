package com.example.user_service.application.ports.input;

import com.example.user_service.application.dto.command.RegisterCommand;
import com.example.user_service.application.dto.output.UserResponse;

public interface RegisterUseCase {
    UserResponse register(RegisterCommand command);
}

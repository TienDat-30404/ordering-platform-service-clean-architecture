package com.example.user_service.adapters.in.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.user_service.application.dto.command.LoginCommand;
import com.example.user_service.application.dto.command.RegisterCommand;
import com.example.user_service.application.dto.output.AuthResponse;
import com.example.user_service.application.dto.output.UserResponse;
import com.example.user_service.application.ports.input.LoginUseCase;
import com.example.user_service.application.ports.input.RegisterUseCase;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@RequestBody RegisterCommand request) {
        UserResponse response = registerUseCase.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse<UserResponse>> loginUser(@RequestBody LoginCommand request) {
        AuthResponse<UserResponse> response = loginUseCase.login(request);
        return ResponseEntity.ok(response);
    }
}

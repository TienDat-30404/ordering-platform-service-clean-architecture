package com.example.user_service.application.ports.output;

public interface TokenGeneratorPort {
    public String generateAccessToken(Long userId, String roleName);
}

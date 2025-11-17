package com.example.user_service.application.ports.output;

public interface PasswordEncoderPort {
    String encode(String rawPassword);
}
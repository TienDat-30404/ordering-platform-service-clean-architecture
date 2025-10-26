package com.example.user_service.application.dto.command;

import lombok.Data;

@Data
public class LoginCommand {
    private String userName;
    private String password;
}

package com.example.user_service.application.dto.command;

import lombok.Data;

@Data
public class RegisterCommand {
    private String name;
    private String userName;
    private String password;
    private Long roleId;
}

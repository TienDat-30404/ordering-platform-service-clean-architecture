package com.example.user_service.application.dto.output;

import lombok.Data;


@Data
public class UserResponse {
    private Long id;
    private String name;
    private String userName;
    private RoleResponse role;
}

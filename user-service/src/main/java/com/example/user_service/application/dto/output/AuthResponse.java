package com.example.user_service.application.dto.output;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse<T> {
    private String accessToken;
    private T userInfo;

}
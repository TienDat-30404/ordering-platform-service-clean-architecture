package com.example.user_service.adapters.out.security;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.example.user_service.application.ports.output.TokenGeneratorPort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;;

@Service
public class JwtTokenGeneratorAdapter implements TokenGeneratorPort {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.accessToken.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refreshToken.expiration}")
    private long refreshTokenExpiration;

    public String generateAccessToken(Long userId, String roleName) {
        return JWT.create()
                .withSubject(userId.toString())
                .withClaim("role", roleName)
                .withExpiresAt(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .sign(Algorithm.HMAC256(secret));
    }
}

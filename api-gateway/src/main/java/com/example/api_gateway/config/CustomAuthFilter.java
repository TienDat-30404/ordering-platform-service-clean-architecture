package com.example.api_gateway.config;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class CustomAuthFilter implements WebFilter, Ordered {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomAuthFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    // @Value("#{'${gateway.public.paths:}'.split(',')}")
    // private List<String> publicPathsRaw;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> publicPaths = List.of(
        "/api/v1/auth/**",
        "/api/v1/auth/register", // Thêm register rõ ràng
        "/api/v1/auth/login"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        
      
         if (HttpMethod.OPTIONS.equals(method)) {
            System.out.println("22222222222222222222222222222222222222");
            LOGGER.debug("Skipping authentication for OPTIONS request: {}", path);
            return chain.filter(exchange); // Cho phép OPTIONS request đi qua ngay lập tức
        }


        if (isPublicPath(path)) { 
            return chain.filter(exchange);
        }

        System.out.println("fffffffffffffffffffffff" + request.getHeaders().getFirst("Authorization"));
        final String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warn("Missing or malformed Authorization header for protected route: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        
        try {
            // Sử dụng thư viện com.auth0.jwt để giải mã và xác minh token
            System.out.println("tffffffffffffffffffff" + jwtSecret);
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);

            // Trích xuất thông tin từ payload của JWT
            String userId = jwt.getSubject();
            System.out.println("userIIIIDDDDDDDĐ" + userId);
            String role = jwt.getClaim("role") != null ? jwt.getClaim("role").asString() : "";
            System.out.println("roleeeeee" + role);
            LOGGER.debug("Successfully authenticated user: {} with role: {}", userId, role);

            // Thêm thông tin người dùng vào các header mới
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", userId != null ? userId : "")
                    .header("X-User-Roles", role)
                    .build();
            // Tiếp tục chuỗi filter
            System.out.println("-------------------------------------");
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (JWTVerificationException e) {
            LOGGER.warn("JWT Token validation failed for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            LOGGER.error("An unexpected error occurred during JWT processing for path {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String requestPath) {
    // Chỉ cần kiểm tra xem đường dẫn có khớp với bất kỳ pattern nào trong publicPaths không
    return publicPaths.stream()
        .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));
    }

    @Override
    public int getOrder() {
        return -1; // Đảm bảo filter này chạy trước các filter khác
    }
}


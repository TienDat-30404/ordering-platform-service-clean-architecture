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

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        
        // System.out.println("=== WEB FILTER TRIGGERED ===");
        // System.out.println("methodddddd" + method);
        // System.out.println("pathhhhhh" + path);

         if (HttpMethod.OPTIONS.equals(method)) {
            LOGGER.debug("Skipping authentication for OPTIONS request: {}", path);
            return chain.filter(exchange); // Cho phép OPTIONS request đi qua ngay lập tức
        }

        // System.out.println("checkBoolean" + isPublicPath(path, method));
        // Bỏ qua authentication cho các endpoint public
        // if (isPublicPath(path, method)) {
        //     LOGGER.debug("Skipping authentication for public path: {}", path);
        //     return chain.filter(exchange);
        // }

        final String authHeader = request.getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            LOGGER.warn("Missing or malformed Authorization header for protected route: {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        
        try {
            // Sử dụng thư viện com.auth0.jwt để giải mã và xác minh token
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);

            // Trích xuất thông tin từ payload của JWT
            String userId = jwt.getSubject();
            String role = jwt.getClaim("role") != null ? jwt.getClaim("role").asString() : "";
            LOGGER.debug("Successfully authenticated user: {} with role: {}", userId, role);

            // Thêm thông tin người dùng vào các header mới
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-ID", userId != null ? userId : "")
                    .header("X-User-Roles", role)
                    .build();
            // Tiếp tục chuỗi filter
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

    // private boolean isPublicPath(String requestPath, HttpMethod requestMethod) {
    //     // System.out.println("requestPathhhh" + requestPath);
    //     // System.out.println("requestMethoddđ" + requestMethod);

    //     if (publicPathsRaw == null || publicPathsRaw.isEmpty()) {
    //         System.out.println("No public paths configured");
    //         return false;
    //     }
    //     return publicPathsRaw.stream()
    //             .anyMatch(publicPathEntry -> {
    //                 String trimmedEntry = publicPathEntry.trim();
                    
    //                 if (trimmedEntry.isEmpty()) {
    //                     return false;
    //                 }

    //                 String[] parts = trimmedEntry.split("\\s+", 2); // Tách bằng whitespace
    //                 HttpMethod configuredMethod = null;
    //                 String configuredPathPattern = null;

    //                 if (parts.length == 2) {
    //                     try {
    //                         configuredMethod = HttpMethod.valueOf(parts[0].toUpperCase());
    //                     } catch (IllegalArgumentException e) {
    //                         LOGGER.warn("Invalid HTTP method in public path configuration: {}", parts[0]);
    //                         return false;
    //                     }
    //                     configuredPathPattern = parts[1];
    //                 } else if (parts.length == 1 && !parts[0].isEmpty()) {
    //                     // Nếu chỉ có đường dẫn, mặc định là public cho MỌI phương thức
    //                     configuredMethod = null;
    //                     configuredPathPattern = parts[0];
    //                 } else {
    //                     return false;
    //                 }
                
    //                 // System.out.println("111111111111111111" + configuredPathPattern);
    //                 // System.out.println("22222222222222222222" + requestPath);
    //                 boolean methodMatches = (configuredMethod == null || configuredMethod.equals(requestMethod));
    //                 boolean pathMatches = pathMatcher.match(configuredPathPattern, requestPath);

    //                 // System.out.println("methodMathcessss" + methodMatches);
    //                 // System.out.println("pathMatchesss" + pathMatches);

    //                 return methodMatches && pathMatches;
    //             });
    // }

    @Override
    public int getOrder() {
        return -1; // Đảm bảo filter này chạy trước các filter khác
    }
}


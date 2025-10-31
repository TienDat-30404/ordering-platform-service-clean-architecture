package com.example.api_gateway.config;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomAuthFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthFilter.class);

    @Value("${jwt.secret:}")
    private String jwtSecret;

    // Cho phép các service nội bộ gọi qua header này (đã cấu hình trong routes)
    @Value("${gateway.internal-token:secret-gateway-key}")
    private String internalToken;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // WHITELIST các route public (login/register, docs, actuator, eureka…)
    private final List<String> publicPaths = List.of(
            "/api/v1/auth/**",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            // actuator/monitoring
            "/actuator/**",
            // eureka dashboard/static (gateway có thể trả qua route/rewrite)
            "/eureka/**",
            // openapi/swagger
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/",
            "/favicon.ico"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // 1) CORS preflight
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // 2) Public paths
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 3) Nội bộ: cho phép nếu có X-Internal-Token khớp
        String internalHdr = request.getHeaders().getFirst("X-Internal-Token");
        if (internalHdr != null && internalHdr.equals(internalToken)) {
            return chain.filter(exchange);
        }

        // 4) JWT
        final String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            log.debug("Missing/invalid Authorization on {}", path);
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7).trim();

        try {
            if (jwtSecret == null || jwtSecret.isBlank()) {
                log.error("jwt.secret is empty -> cannot verify JWT");
                exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                return exchange.getResponse().setComplete();
            }

            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);

            String userId = jwt.getSubject();
            String role = jwt.getClaim("role") != null ? jwt.getClaim("role").asString() : "";

            // Propagate context to downstream services
            ServerHttpRequest modified = request.mutate()
                    .header("X-User-ID", userId != null ? userId : "")
                    .header("X-User-Roles", role != null ? role : "")
                    .build();

            return chain.filter(exchange.mutate().request(modified).build());

        } catch (JWTVerificationException e) {
            log.warn("JWT verification failed on {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        } catch (Exception e) {
            log.error("Unexpected JWT error on {}: {}", path, e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicPath(String requestPath) {
        return publicPaths.stream().anyMatch(p -> pathMatcher.match(p, requestPath));
    }

    @Override
    public int getOrder() {
        return -1; // chạy sớm
    }
}

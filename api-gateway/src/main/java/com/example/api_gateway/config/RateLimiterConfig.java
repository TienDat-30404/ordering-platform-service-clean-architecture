package com.example.api_gateway.config; 

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver customerIdKeyResolver() {
        return new KeyResolver() {
            @Override
            public Mono<String> resolve(ServerWebExchange exchange) {
                // Lấy ID khách hàng từ Header "X-Customer-ID"
                String customerId = exchange.getRequest().getHeaders().getFirst("X-Customer-ID");
                System.out.println("Rate Limiter - Customer ID: " + customerId);
                if (customerId != null && !customerId.isEmpty()) {
                    System.out.println("Rate Limiter - Using Customer ID as Key");
                    return Mono.just(customerId); 
                } else {
                    System.out.println("Rate Limiter - Using IP Address as Key");
                    return Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
                }
            }
        };
    }
}
package com.example.demo.domain.event;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class SagaEnvelope {
    private String eventType;
    private String orderId;
    private String restaurantId;
    private Object payload;      // map/DTO tùy bạn
    private String timestamp;    // ISO-8601
}
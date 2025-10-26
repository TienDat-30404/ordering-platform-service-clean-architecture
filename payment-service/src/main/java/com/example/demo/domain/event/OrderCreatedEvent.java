package com.example.demo.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
// @AllArgsConstructor
public class OrderCreatedEvent extends DomainEvent {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String restaurantId;

    public OrderCreatedEvent(Long orderId, Long userId, BigDecimal amount, String restaurantId) {
        super(orderId.toString(), "OrderCreated");
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.restaurantId = restaurantId;
    }
}
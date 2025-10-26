package com.example.demo.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
// @AllArgsConstructor
public class RestaurantRejectedEvent extends DomainEvent {
    private Long orderId;
    private Long restaurantId;
    private String reason;

    public RestaurantRejectedEvent(Long orderId, Long restaurantId, String reason) {
        super(orderId.toString(), "RestaurantRejected");
        this.orderId = orderId;
        this.restaurantId = restaurantId;
        this.reason = reason;
    }
}

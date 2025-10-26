package com.example.demo.domain.event;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
// @AllArgsConstructor
public class PaymentFailedEvent extends DomainEvent {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private String reason;

    public PaymentFailedEvent(Long paymentId, Long orderId, Long userId, String reason) {
        super(orderId.toString(), "PaymentFailed");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.reason = reason;
    }
}
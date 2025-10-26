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
public class RefundRequestedEvent extends DomainEvent {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String reason;

    public RefundRequestedEvent(Long paymentId, Long orderId, BigDecimal amount, String reason) {
        super(orderId.toString(), "RefundRequested");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.reason = reason;
    }
}
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
public class RefundCompletedEvent extends DomainEvent {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String refundTransactionId;

    public RefundCompletedEvent(Long paymentId, Long orderId, BigDecimal amount, String refundTransactionId) {
        super(orderId.toString(), "RefundCompleted");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.refundTransactionId = refundTransactionId;
    }
}
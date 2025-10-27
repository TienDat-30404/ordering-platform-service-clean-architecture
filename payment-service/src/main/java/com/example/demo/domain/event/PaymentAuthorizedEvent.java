package com.example.demo.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
// @AllArgsConstructor
public class PaymentAuthorizedEvent extends DomainEvent {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String transactionId;

    public PaymentAuthorizedEvent(Long paymentId, Long orderId, Long userId, BigDecimal amount, String transactionId) {
        super(orderId.toString(), "PaymentAuthorized");
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.transactionId = transactionId;
    }
}

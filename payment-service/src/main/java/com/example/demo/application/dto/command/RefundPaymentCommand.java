package com.example.demo.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPaymentCommand extends PaymentCommand {
    private Long paymentId;
    private String reason;

    public RefundPaymentCommand(Long orderId, Long paymentId, BigDecimal amount, String reason) {
        super(java.util.UUID.randomUUID().toString(), orderId, null, amount, "RefundPayment");
        this.paymentId = paymentId;
        this.reason = reason;
    }
}
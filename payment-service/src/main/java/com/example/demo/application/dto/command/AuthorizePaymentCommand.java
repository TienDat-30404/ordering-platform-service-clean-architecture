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
public class AuthorizePaymentCommand extends PaymentCommand {
    private Long paymentId;

    public AuthorizePaymentCommand(Long orderId, Long userId, BigDecimal amount, Long paymentId) {
        super(java.util.UUID.randomUUID().toString(), orderId, userId, amount, "AuthorizePayment");
        this.paymentId = paymentId;
    }
}

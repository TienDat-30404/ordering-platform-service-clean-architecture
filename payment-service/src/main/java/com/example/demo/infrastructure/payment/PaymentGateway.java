package com.example.demo.infrastructure.payment;

import java.math.BigDecimal;

public interface PaymentGateway {
   String authorize(BigDecimal amount, Long userId, Long orderId);
   String refund(String transactionId, BigDecimal amount);
}
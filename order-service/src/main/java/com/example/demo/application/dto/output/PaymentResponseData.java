package com.example.demo.application.dto.output;

import java.math.BigDecimal;

public record PaymentResponseData(
    Long paymentId,
    Long orderId,
    Long userId,
    String status, // "AUTHORIZED" hoặc "FAILED"
    BigDecimal amount,
    String transactionId,
    String reason // Lý do thất bại (nếu có)
) {}
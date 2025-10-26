package com.example.common_dtos.dto;

import java.math.BigDecimal;

// Response DTO chỉ gửi dữ liệu cần thiết về Orchestrator
public record PaymentResponseData(
        Long paymentId,
        Long orderId,
        Long userId,
        String status, // "AUTHORIZED" hoặc "FAILED"
        BigDecimal amount,
        String transactionId,
        String reason // Lý do thất bại (nếu có)
) {
}
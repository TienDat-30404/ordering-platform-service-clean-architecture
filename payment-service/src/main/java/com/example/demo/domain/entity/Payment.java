package com.example.demo.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.domain.valueobject.OrderId;
import com.example.demo.domain.valueobject.PaymentId;
import com.example.common_dtos.enums.PaymentStatus;
import com.example.demo.domain.valueobject.UserId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private Long paymentId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String failureReason;
}
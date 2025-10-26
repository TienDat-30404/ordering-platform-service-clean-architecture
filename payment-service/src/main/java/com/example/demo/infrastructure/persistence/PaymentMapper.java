package com.example.demo.infrastructure.persistence;

import com.example.demo.domain.entity.Payment;
import com.example.demo.domain.valueobject.OrderId;
import com.example.demo.domain.valueobject.PaymentId;
import com.example.demo.domain.valueobject.UserId;

public class PaymentMapper {
    
    public static Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .paymentId(entity.getPaymentId())
                .orderId(entity.getOrderId())
                .userId(entity.getUserId())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .transactionId(entity.getTransactionId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .failureReason(entity.getFailureReason())
                .build();
    }
    
    public static PaymentEntity toEntity(Payment domain) {
        return PaymentEntity.builder()
                .paymentId(domain.getPaymentId())
                .orderId(domain.getOrderId())
                .userId(domain.getUserId())
                .amount(domain.getAmount())
                .status(domain.getStatus())
                .transactionId(domain.getTransactionId())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .failureReason(domain.getFailureReason())
                .build();
    }
}

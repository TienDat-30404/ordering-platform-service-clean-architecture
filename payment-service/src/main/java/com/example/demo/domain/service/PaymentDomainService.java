package com.example.demo.domain.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.demo.domain.entity.Payment;
import com.example.demo.domain.valueobject.OrderId;
import com.example.common_dtos.enums.PaymentStatus;
import com.example.demo.domain.valueobject.UserId;

@Service
public class PaymentDomainService {

    public Payment createPayment(Long orderId, Long userId, BigDecimal amount) {
        // validateAmount(amount);

        return Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void completePayment(Payment payment, String transactionId, String failureReason) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Can only complete pending payments");
        }
        if (transactionId != null) {
            // --- LOGIC THÀNH CÔNG ---
            payment.setStatus(PaymentStatus.AUTHORIZED);
            payment.setTransactionId(transactionId);
            payment.setUpdatedAt(LocalDateTime.now());
        } else {
            // --- LOGIC THẤT BẠI ---
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(failureReason != null ? failureReason : "TransactionId ko hợp lệ");
            payment.setUpdatedAt(LocalDateTime.now());
            payment.setTransactionId(null); // Đảm bảo transactionId là null khi thất bại
        }
    }

    // public void authorizePayment(Payment payment, String transactionId) {
    // if (payment.getStatus() != PaymentStatus.PENDING) {
    // throw new IllegalStateException("Can only authorize pending payments");
    // }
    // payment.setStatus(PaymentStatus.AUTHORIZED);
    // payment.setTransactionId(transactionId);
    // payment.setUpdatedAt(LocalDateTime.now());
    // }

    public void failPayment(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        payment.setUpdatedAt(LocalDateTime.now());
    }

    public void requestRefund(Payment payment) {
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw new IllegalStateException("Can only refund authorized payments");
        }
        payment.setStatus(PaymentStatus.REFUND_REQUESTED);
        payment.setUpdatedAt(LocalDateTime.now());
    }

    public void completeRefund(Payment payment) {
        // if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
        // throw new IllegalStateException("Payment must be in refund requested state");
        // }
        payment.setStatus(PaymentStatus.REFUND_COMPLETED);
        payment.setUpdatedAt(LocalDateTime.now());
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
    }
}

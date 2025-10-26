//package com.example.demo.infrastructure.payment;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.math.BigDecimal;
//import java.util.UUID;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class StripePaymentGateway implements PaymentGateway {
//
//    public String authorize(BigDecimal amount, Long userId, Long orderId) {
//        log.info("Authorizing payment with Stripe - Amount: {}, UserId: {}, OrderId: {}", amount, userId, orderId);
//
//        // In production, use Stripe SDK: com.stripe:stripe-java
//        try {
//            // Simulate API call delay
//            Thread.sleep(100);
//
//            // Generate transaction ID
//            String transactionId = "txn_" + UUID.randomUUID().toString();
//            // String transactionId = null;
//
//            log.info("Payment authorized with transaction ID: {}", transactionId);
//            return transactionId;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Payment authorization interrupted", e);
//        }
//    }
//
//    @Override
//    public String refund(String transactionId, BigDecimal amount) {
//        log.info("Refunding payment with Stripe - TransactionId: {}, Amount: {}", transactionId, amount);
//
//        try {
//            // Simulate API call delay
//            Thread.sleep(100);
//
//            // Generate refund transaction ID
//            String refundTransactionId = "ref_" + UUID.randomUUID().toString();
//            log.info("Payment refunded with refund transaction ID: {}", refundTransactionId);
//            return refundTransactionId;
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            throw new RuntimeException("Payment refund interrupted", e);
//        }
//    }
//}

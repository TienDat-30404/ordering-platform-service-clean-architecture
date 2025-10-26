//package com.example.demo.infrastructure.persistence;
//
//
//
//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EnumType;
//import jakarta.persistence.Enumerated;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import lombok.AllArgsConstructor;
//import lombok.Builder;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//import com.example.demo.domain.valueobject.PaymentStatus;
//
//@Entity
//@Table(name = "payments")
//@Data
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class PaymentEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long paymentId;
//
//    @Column(nullable = false)
//
//    private Long orderId;
//
//    @Column(nullable = false)
//    private Long userId;
//
//    @Column(nullable = false)
//    private BigDecimal amount;
//
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private PaymentStatus status;
//
//    private String transactionId;
//
//    @Column(nullable = false)
//    private LocalDateTime createdAt;
//
//    private LocalDateTime updatedAt;
//
//    private String failureReason;
//}

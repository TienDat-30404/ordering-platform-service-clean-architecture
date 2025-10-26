package com.example.demo.application.ports.output.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.example.demo.domain.entity.Payment;


public interface PaymentRepository {
    public Payment save(Payment payment);
    Optional<Payment> findById(Long paymentId);
    Optional<Payment> findByOrderId(Long orderId);
    void deleteById(Long paymentId);
}

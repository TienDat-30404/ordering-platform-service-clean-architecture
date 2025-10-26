package com.example.demo.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.example.demo.application.ports.output.repository.PaymentRepository;
import com.example.demo.domain.entity.Payment;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    public Payment save(Payment payment) {
        PaymentEntity entity = PaymentMapper.toEntity(payment);
        PaymentEntity saved = paymentJpaRepository.save(entity);
        return PaymentMapper.toDomain(saved);
    }

    public Optional<Payment> findById(Long paymentId) {
        return paymentJpaRepository.findById(paymentId)
                .map(PaymentMapper::toDomain);
    }

    public Optional<Payment> findByOrderId(Long orderId) {
        return paymentJpaRepository.findByOrderId(orderId)
                .map(PaymentMapper::toDomain);
    }

    public void deleteById(Long paymentId) {
        paymentJpaRepository.deleteById(paymentId);
    }
}

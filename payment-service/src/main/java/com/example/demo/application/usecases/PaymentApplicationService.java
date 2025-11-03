package com.example.demo.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.common_dtos.enums.PaymentStatus;
import com.example.demo.application.dto.command.AuthorizePaymentCommand;
import com.example.demo.application.dto.command.RefundPaymentCommand;
import com.example.demo.application.ports.output.repository.PaymentRepository;
import com.example.demo.domain.entity.Payment;
import com.example.demo.domain.service.PaymentDomainService;
import com.example.demo.infrastructure.payment.PaymentGateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentDomainService paymentDomainService;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponseData authorizePayment(AuthorizePaymentCommand command) {
        log.info("Authorizing payment for order: {}", command.getOrderId());

        // ---- FIND-OR-CREATE (giữ tối thiểu thay đổi) ----
        Payment payment = null;

        if (command.getPaymentId() != null) {
            payment = paymentRepository.findById(command.getPaymentId()).orElse(null);
        }
        if (payment == null && command.getOrderId() != null) {
            payment = paymentRepository.findByOrderId(command.getOrderId()).orElse(null);
        }
        if (payment == null) {
            // tạo mới với trạng thái PENDING + createdAt đã set trong Domain
            payment = paymentDomainService.createPayment(
                    command.getOrderId(),
                    command.getUserId(),
                    command.getAmount()
            );
            payment = paymentRepository.save(payment);
            log.debug("[PAYMENT] Created new payment row: paymentId={} for orderId={}",
                    payment.getPaymentId(), payment.getOrderId());
        }

        // Idempotency: nếu đã xử lý thì trả về luôn
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment for order {} already processed with status: {}", command.getOrderId(),
                    payment.getStatus());
            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getStatus().toString(), payment.getAmount(), payment.getTransactionId(),
                    payment.getFailureReason());
        }

        try {
            // Gọi cổng thanh toán (authorize/hold)
            String transactionId = paymentGateway.authorize(
                    payment.getAmount(), command.getUserId(), command.getOrderId());

            String finalStatus;
            String reason = null;

            if (transactionId != null && !transactionId.isBlank()) {
                paymentDomainService.completePayment(payment, transactionId, null); // AUTHORIZED
                finalStatus = "AUTHORIZED";
            } else {
                reason = "Payment gateway failed to return transaction ID.";
                paymentDomainService.completePayment(payment, null, reason);        // FAILED
                finalStatus = "FAILED";
            }

            paymentRepository.save(payment);

            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    finalStatus, payment.getAmount(), payment.getTransactionId(), reason);

        } catch (Exception e) {
            log.error("Payment authorization failed due to system error for order: {}", command.getOrderId(), e);
            String reason = "System exception during authorization: " + e.getMessage();
            paymentDomainService.failPayment(payment, reason);
            paymentRepository.save(payment);

            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    "FAILED", payment.getAmount(), null, reason);
        }
    }

    @Transactional
    public PaymentResponseData refundPayment(RefundPaymentCommand command) {
        log.info("Refunding payment for order: {}", command.getOrderId());

        Payment payment = null;
        if (command.getPaymentId() != null) {
            payment = paymentRepository.findById(command.getPaymentId()).orElse(null);
        }
        if (payment == null && command.getOrderId() != null) {
            payment = paymentRepository.findByOrderId(command.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Payment not found by orderId: " + command.getOrderId()));
        }

        if (payment.getStatus() == PaymentStatus.REFUND_COMPLETED) {
            log.warn("[IDEMPOTENCY] Payment for Order {} is already REFUND_COMPLETED. Skip.",
                    command.getOrderId());
            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    "REFUND_COMPLETED", payment.getAmount(), payment.getTransactionId(),
                    "Already refunded successfully");
        }

        try {
            // Nếu bạn muốn gọi gateway.refund(...) thì thêm ở đây; tối thiểu giữ nguyên:
            paymentDomainService.completeRefund(payment);
            paymentRepository.save(payment);

            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    "REFUND_COMPLETED", payment.getAmount(), payment.getTransactionId(),
                    "Compensation executed");
        } catch (Exception e) {
            log.error("Payment refund failed for order: {}", command.getOrderId(), e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void handlePaymentFailure(AuthorizePaymentCommand command, String reason) {
        Payment payment = null;
        if (command.getPaymentId() != null) {
            payment = paymentRepository.findById(command.getPaymentId()).orElse(null);
        }
        if (payment == null && command.getOrderId() != null) {
            payment = paymentRepository.findByOrderId(command.getOrderId()).orElse(null);
        }
        if (payment == null) {
            // Tạo tối thiểu để lưu lỗi (hiếm khi vào đây)
            payment = paymentDomainService.createPayment(
                    command.getOrderId(), command.getUserId(), command.getAmount());
        }

        paymentDomainService.completePayment(payment, null, reason); // set FAILED
        paymentRepository.save(payment);
    }
}

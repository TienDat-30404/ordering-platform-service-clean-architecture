package com.example.demo.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.demo.application.dto.command.AuthorizePaymentCommand;
import com.example.demo.application.dto.command.RefundPaymentCommand;
import com.example.demo.application.ports.output.repository.PaymentRepository;
import com.example.demo.domain.entity.Payment;
import com.example.demo.domain.event.PaymentAuthorizedEvent;
import com.example.demo.domain.event.PaymentFailedEvent;
import com.example.demo.domain.event.RefundCompletedEvent;
import com.example.demo.domain.service.PaymentDomainService;
import com.example.demo.domain.valueobject.PaymentStatus;
import com.example.demo.infrastructure.payment.PaymentGateway;
import com.example.demo.infrastructure.publisher.EventPublisher;
import org.springframework.transaction.annotation.Propagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentApplicationService {

    private final PaymentRepository paymentRepository;
    private final PaymentDomainService paymentDomainService;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    @Transactional
    public PaymentResponseData authorizePayment(AuthorizePaymentCommand command) {
        log.info("Authorizing payment for order: {}", command.getOrderId());

        Payment payment = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + command.getPaymentId()));

        // Trả về kết quả nếu đã xử lý (Xử lý Idempotency)
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment for order {} already processed with status: {}", command.getOrderId(),
                    payment.getStatus());
            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    payment.getStatus().toString(), payment.getAmount(), payment.getTransactionId(),
                    payment.getFailureReason());
        }

        try {
            // Gọi cổng thanh toán
            String transactionId = paymentGateway.authorize(
                    payment.getAmount(), command.getUserId(), command.getOrderId());

            String finalStatus;
            String reason = null;

            if (transactionId != null) {
                // THÀNH CÔNG
                paymentDomainService.completePayment(payment, transactionId, null);
                finalStatus = "AUTHORIZED";
            } else {
                // THẤT BẠI LOGIC (Gateway từ chối/không phản hồi ID)
                reason = "Payment gateway failed to return transaction ID.";
                paymentDomainService.completePayment(payment, null, reason); // Ghi trạng thái FAILED vào DB
                finalStatus = "FAILED";
            }

            paymentRepository.save(payment);

            // **KHÔNG PUBLISH EVENT** - Chỉ trả về phản hồi cho Listener
            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    finalStatus, payment.getAmount(), transactionId, reason);

        } catch (Exception e) {
            log.error("Payment authorization failed due to system error for order: {}", command.getOrderId(), e);

            // THẤT BẠI HỆ THỐNG: Cập nhật Payment sang FAILED và lưu vào DB
            String reason = "System exception during authorization: " + e.getMessage();
            paymentDomainService.failPayment(payment, reason);
            paymentRepository.save(payment);

            // Trả về FAILED cho Orchestrator
            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    "FAILED", payment.getAmount(), null, reason);
            // KHÔNG throw RuntimeException để giao dịch cập nhật FAILED được commit.
        }
    }

    @Transactional
    public PaymentResponseData refundPayment(RefundPaymentCommand command) {
        log.info("Refunding payment for order: {}", command.getOrderId());

        try {
            Payment payment = paymentRepository.findById(command.getPaymentId())
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + command.getPaymentId()));
            if (payment.getStatus() == PaymentStatus.REFUND_COMPLETED) {
                log.warn("[IDEMPOTENCY] Payment for Order {} is already REFUND_COMPLETED. Skipping redundant refund.",
                        command.getOrderId());

                // Trả về DTO thành công để cho Kafka commit offset
                return new PaymentResponseData(
                        payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                        "REFUND_COMPLETED", payment.getAmount(), payment.getTransactionId(),
                        "Already refunded successfully");
            }
            paymentDomainService.completeRefund(payment);
            paymentRepository.save(payment);

            paymentDomainService.completeRefund(payment);
            paymentRepository.save(payment);

            return new PaymentResponseData(
                    payment.getPaymentId(), payment.getOrderId(), payment.getUserId(),
                    "REFUND_COMPLETED", payment.getAmount(), payment.getTransactionId(), "Restaut did not approved");

        } catch (Exception e) {
            log.error("Payment refund failed for order: {}", command.getOrderId(), e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void handlePaymentFailure(AuthorizePaymentCommand command, String reason) {
        Payment payment = paymentRepository.findById(command.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // paymentDomainService.failPayment(payment, reason);
        // paymentRepository.save(payment);

        paymentDomainService.completePayment(payment, null, reason);

        // 2. LƯU (COMMIT) VÀO DB BẰNG TRANSACTION MỚI NÀY
        paymentRepository.save(payment);

        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getUserId(),
                reason);
        eventPublisher.publishPaymentFailed(event);
    }
}
package com.example.demo.application.saga;

import com.example.demo.config.KafkaConfig;
import com.example.common_dtos.dto.PaymentResponseData;
import com.example.demo.application.dto.command.AuthorizePaymentCommand;
import com.example.demo.application.dto.command.RefundPaymentCommand;
import com.example.demo.domain.entity.Payment;
import com.example.demo.domain.event.AuthorizePaymentCommandData;
import com.example.demo.domain.event.OrderCreatedEvent;
import com.example.demo.domain.event.RefundRequestedEvent;
import com.example.demo.domain.event.RestaurantRejectedEvent;
import com.example.demo.domain.service.PaymentDomainService;
import com.example.demo.infrastructure.publisher.OrchestratorResponsePublisher;

import jakarta.transaction.Transactional;

import com.example.demo.application.ports.output.repository.PaymentRepository;
import com.example.demo.application.usecases.PaymentApplicationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSaga {

    private final PaymentApplicationService paymentApplicationService;
    private final PaymentRepository paymentRepository;
    private final PaymentDomainService paymentDomainService;
    private final OrchestratorResponsePublisher orchestratorResponsePublisher;

    @KafkaListener(topics = "payment.authorize.command", groupId = "payment-service-group")
    @Transactional
    public void handleAuthorizePaymentCommand(@Payload AuthorizePaymentCommandData event,
            @Headers Map<String, Object> headers) {
        log.info("[LISTENER] Received AuthorizePayment command for order: {}", event.getOrderId());
        // String sagaId = (String) headers.get("sagaId");
        System.out.println("sagaaaIdddddddd" + headers);
        PaymentResponseData response = null;

        try {
            // 1. Tạo Payment (Idempotency check qua findByOrderId)
            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                    .orElseGet(() -> {
                        log.info("[LISTENER] Creating new Payment record for order: {}", event.getOrderId());
                        Payment newPayment = paymentDomainService.createPayment(
                                event.getOrderId(), event.getUserId(), event.getAmount());
                        return paymentRepository.save(newPayment);
                    });

            // 2. Chuẩn bị Command
            AuthorizePaymentCommand command = new AuthorizePaymentCommand(
                    event.getOrderId(), event.getUserId(), event.getAmount(), payment.getPaymentId());

            // 3. Gọi Application Service và nhận phản hồi
            response = paymentApplicationService.authorizePayment(command);

        } catch (Exception e) {
            log.error("[LISTENER] Critical error during Payment creation/authorization for order: {}",
                    event.getOrderId(), e);

       
            response = new PaymentResponseData(
                    null, event.getOrderId(), event.getUserId(), "FAILED", event.getAmount(), null,
                    "Critical system failure during command handling: " + e.getMessage());

            // return;
        } finally {
            System.out.println("publicResponseeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee");
            System.out.println("responseeeeeeeeeeeeeeeeeeee" + response);
            if (response != null) {
                orchestratorResponsePublisher.publishResponse(response);
            }
        }
    }

    // @KafkaListener(topics = KafkaConfig.REFUND_REQUESTED_TOPIC, groupId =
    // "payment-service-group")
    public void handleRefundRequested(RefundRequestedEvent event) {
        log.info("Received RefundRequested event for order: {}", event.getOrderId());

        try {
            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + event.getOrderId()));

            RefundPaymentCommand command = new RefundPaymentCommand(
                    event.getOrderId(),
                    payment.getPaymentId(),
                    event.getAmount(),
                    event.getReason());
            paymentApplicationService.refundPayment(command);
        } catch (Exception e) {
            log.error("Error handling RefundRequested event", e);
        }
    }

    // @KafkaListener(topics = KafkaConfig.RESTAURANT_REJECTED_TOPIC, groupId =
    // "payment-service-group")
    public void handleRestaurantRejected(RestaurantRejectedEvent event) {
        log.info("Received RestaurantRejected event for order: {}", event.getOrderId());

        try {
            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + event.getOrderId()));

            RefundPaymentCommand command = new RefundPaymentCommand(
                    event.getOrderId(),
                    payment.getPaymentId(),
                    payment.getAmount(),
                    "Restaurant rejected the order");
            paymentApplicationService.refundPayment(command);
        } catch (Exception e) {
            log.error("Error handling RestaurantRejected event", e);
        }
    }
}

// ------------ Mở kakfa ------------------
// docker exec -it kafka /bin/bash

// ------------ reset dữ liệu kafka ------------
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --group payment-service-group \
// --topic payment.authorize.command \
// --reset-offsets \
// --to-latest \
// --execute

// ----------------- xem dữ liệu offset kakfa đã mới nhất -------------
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --describe \
// --group payment-service-group

// --------------------- Đọc tin nhắn tại offset ---------------------
// /usr/bin/kafka-console-consumer \
// --bootstrap-server kafka:9092 \
// --topic payment.authorize.command \
// --offset 212 \
// --partition 0 \
// --max-messages 1 \
// --property print.key=true

// order

// ------------ reset kafka order -------------
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --group order-service-group \
// --topic orchestrator.payment.response \
// --reset-offsets \
// --to-latest \
// --execute

// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --describe \
// --group order-service-group
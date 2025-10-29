//package com.example.demo.application.saga;
//
//import com.example.demo.config.KafkaConfig;
//import com.example.common_dtos.dto.PaymentResponseData;
//import com.example.demo.application.dto.command.AuthorizePaymentCommand;
//import com.example.demo.application.dto.command.RefundPaymentCommand;
//import com.example.demo.domain.entity.Payment;
//import com.example.demo.domain.event.AuthorizePaymentCommandData;
//import com.example.demo.domain.event.OrderCreatedEvent;
//import com.example.demo.domain.event.RefundRequestedEvent;
//import com.example.demo.domain.event.RestaurantRejectedEvent;
//import com.example.demo.domain.service.PaymentDomainService;
//import com.example.demo.infrastructure.publisher.OrchestratorResponsePublisher;
//
//import jakarta.transaction.Transactional;
//
//import com.example.demo.application.ports.output.repository.PaymentRepository;
//import com.example.demo.application.usecases.PaymentApplicationService;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
//import java.math.BigDecimal;
//import java.nio.charset.StandardCharsets;
//import java.util.Map;
//
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.messaging.handler.annotation.Headers;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class PaymentSaga {
//
//    private final PaymentApplicationService paymentApplicationService;
//    private final PaymentRepository paymentRepository;
//    private final PaymentDomainService paymentDomainService;
//    private final OrchestratorResponsePublisher orchestratorResponsePublisher;
//
//    @KafkaListener(topics = "payment.command", groupId = "payment-service-group")
//    @Transactional
//    public void handleAuthorizePaymentCommand(@Payload AuthorizePaymentCommandData event,
//            @Headers Map<String, Object> headers) {
//        System.out.println("headddddddddddddddddddddddđ" + headers);
//        System.out.println("232kkkkkkkkkkkkkkkkkkkkkk" + event.getAmount());
//        PaymentResponseData response = null;
//        byte[] eventTypeBytes = (byte[]) headers.get("eventType");
//        String eventType = new String(eventTypeBytes, StandardCharsets.UTF_8);
//
//        System.out.println("eventTYpeeeeeeeeeeeeeeee: " + eventType);
//        try {
//            switch (eventType) {
//                case "AUTHORIZE_PAYMENT":
//
//                    Payment payment = paymentRepository.findByOrderId(event.getOrderId())
//                            .orElseGet(() -> {
//                                log.info("[LISTENER] Creating new Payment record for order: {}", event.getOrderId());
//                                Payment newPayment = paymentDomainService.createPayment(
//                                        event.getOrderId(), event.getUserId(), event.getAmount());
//                                return paymentRepository.save(newPayment);
//                            });
//
//                    AuthorizePaymentCommand command = new AuthorizePaymentCommand(
//                            event.getOrderId(), event.getUserId(), event.getAmount(), payment.getPaymentId());
//
//                    response = paymentApplicationService.authorizePayment(command);
//                    System.out.println("vvvvvvvvvvvvvvvvvvvvvv" + response);
//                    break;
//                case "CANCEL_PAYMENT":
//                    log.info("[CANCEL] Received CANCEL_PAYMENT command for order: {}", event.getOrderId());
//
//                    // 1. Tìm Payment đã tạo trước đó
//                    Payment existingPayment = paymentRepository.findByOrderId(event.getOrderId())
//                            .orElseThrow(() -> {
//                                // Nếu không tìm thấy, ném lỗi để gửi về DLQ/Retry
//                                log.error("[CANCEL] Refund failed: Payment record not found for order {}", event.getOrderId());
//                                return new RuntimeException("Payment not found for order: " + event.getOrderId());
//                            });
//
//                    // 2. Tạo Refund Command
//                    RefundPaymentCommand refundCommand = new RefundPaymentCommand(
//                            event.getOrderId(),
//                            existingPayment.getPaymentId(),
//                            event.getAmount(),
//                            "Cancelled by Order Saga");
//
//                    // 3. Thực thi Refund & Gán phản hồi
//                    response = paymentApplicationService.refundPayment(refundCommand);
//                    log.info("[CANCEL] Refund successful for order {}. Status: {}", event.getOrderId(), response.status());
//                    break;
//
//            }
//
//        } catch (Exception e) {
//            log.error("[LISTENER] Critical error during Payment creation/authorization for order: {}",
//                    event.getOrderId(), e);
//
//            response = new PaymentResponseData(
//                    null, event.getOrderId(), event.getUserId(), "FAILED", event.getAmount(), null,
//                    "Critical system failure during command handling: " + e.getMessage());
//
//            // return;
//        } finally {
//            if (response != null) {
//                System.out.println("responseeeeeeeeeeeeeeeeeeee" + response.status());
//                System.out.println("headerrrrrrrrrrrrrrrrrrrs" + headers);
//                orchestratorResponsePublisher.publishResponse(response, headers);
//            }
//        }
//    }
//
//    // @KafkaListener(topics = "refund_payment", groupId =
//    // "payment-service-group")
//    public void handleRefundPaymentCommand(@Payload RefundRequestedEvent event,
//            @Headers Map<String, Object> headers) {
//        log.info("[LISTENER] Received Refund command for order: {}", event.getOrderId());
//        PaymentResponseData response = null;
//
//        try {
//
//            // 1. Tìm Payment (Idempotency check qua findByOrderId)
//            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
//                    .orElseThrow(() -> {
//                        log.error("[LISTENER] Refund failed: Payment record not found for order {}",
//                                event.getOrderId());
//                        throw new RuntimeException("Payment not found for order: " + event.getOrderId());
//                    });
//
//            RefundPaymentCommand command = new RefundPaymentCommand(
//                    event.getOrderId(),
//                    payment.getPaymentId(),
//                    event.getAmount(),
//                    event.getReason());
//
//            // 3. GỌI APPLICATION SERVICE VÀ NHẬN PHẢN HỒI
//            // Hàm refundPayment sẽ trả về PaymentResponseData với status cuối cùng
//            response = paymentApplicationService.refundPayment(command);
//
//        } catch (RuntimeException e) {
//            log.error("[LISTENER] Error during Refund command handling for order: {}", event.getOrderId(), e);
//
//            response = new PaymentResponseData(
//                    null,
//                    event.getOrderId(),
//                    event.getUserId(),
//                    "REFUND_FAILED", // Status FAILED
//                    event.getAmount(),
//                    null,
//                    "System error or Payment not found: " + e.getMessage());
//
//        } finally {
//            // 4. GỬI PHẢN HỒI CUỐI CÙNG CHO ORDER ORCHESTRATOR
//            if (response != null) {
//                orchestratorResponsePublisher.publishResponse(response, headers);
//            }
//        }
//    }
//
//    // @KafkaListener(topics = KafkaConfig.RESTAURANT_REJECTED_TOPIC, groupId =
//    // "payment-service-group")
//    public void handleRestaurantRejected(RestaurantRejectedEvent event) {
//        log.info("Received RestaurantRejected event for order: {}", event.getOrderId());
//
//        try {
//            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
//                    .orElseThrow(() -> new RuntimeException("Payment not found for order: " + event.getOrderId()));
//
//            RefundPaymentCommand command = new RefundPaymentCommand(
//                    event.getOrderId(),
//                    payment.getPaymentId(),
//                    payment.getAmount(),
//                    "Restaurant rejected the order");
//            paymentApplicationService.refundPayment(command);
//        } catch (Exception e) {
//            log.error("Error handling RestaurantRejected event", e);
//        }
//    }
//}

// ------------ Mở kakfa ------------------
// docker exec -it kafka /bin/bash


                            // ----------------- xem dữ liệu offset kakfa đã mới nhất ------------------- //
// --------------------------------------------------------------> paymenr-service-group
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --describe \
// --group payment-service-group

// -----------------------------------------------------------------> order-service-group
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --describe \
// --group order-service-group




                            // ------------ -----reset kafka order -----------------------------------//
// --------------------------------------------------------------------> orhestrator.payment-response
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --group order-service-group \
// --topic orchestrator.payment.response \
// --reset-offsets \
// --to-latest \
// --execute


// --------------------------------------------------------------------------> order.saga.reply
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --group order-service-group \
// --topic order.saga.reply \
// --reset-offsets \
// --to-latest \
// --execute


// ------------------------------------------------------------------------> payment.command
// /usr/bin/kafka-consumer-groups \
// --bootstrap-server kafka:9092 \
// --group payment-service-group \
// --topic payment.authorize.command \
// --reset-offsets \
// --to-latest \
// --execute




                            // ----------------- Lệnh lấy dữ liệu của offset tại vị ví ? của topic ??? ----------------
// 
// /usr/bin/kafka-console-consumer \
// --bootstrap-server kafka:9092 \
// --topic payment.command \
// --offset 76072 \
// --partition 0 \
// --max-messages 1 \
// --property print.key=true

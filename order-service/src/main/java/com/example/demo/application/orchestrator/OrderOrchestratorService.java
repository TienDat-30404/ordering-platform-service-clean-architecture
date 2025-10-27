package com.example.demo.application.orchestrator;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.dto.saga.AuthorizePaymentCommandData;
import com.example.demo.application.ports.input.CanceledOrderUseCase;
import com.example.demo.application.ports.input.ConfirmOrderPaidUseCase;
import com.example.demo.application.ports.output.OrderPublisher.OrderEventPublisher;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.event.SagaEnvelope;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestratorService {

    private final ConfirmOrderPaidUseCase confirmOrderPaidUseCase;
    private final CanceledOrderUseCase canceledOrderUseCase;
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderEventPublisher publisher;
    private final ObjectMapper om = new ObjectMapper();

    // ===== Controller có thể truyền Long hoặc String =====
    public void startCreateOrderSagaFromCommand(String orderId,
            Long restaurantId,
            List<CreateOrderItemCommand> items) {
        if (restaurantId == null)
            throw new IllegalArgumentException("restaurantId is required");
        startCreateOrderSagaFromCommand(orderId, String.valueOf(restaurantId), items);
    }

    public void startCreateOrderSagaFromCommand(String orderId,
            String restaurantId,
            List<CreateOrderItemCommand> items) {
        if (orderId == null || orderId.isBlank())
            throw new IllegalArgumentException("orderId is required");
        if (restaurantId == null || restaurantId.isBlank())
            throw new IllegalArgumentException("restaurantId is required");
        if (items == null || items.isEmpty())
            throw new IllegalArgumentException("items are required");

        // Gom item trùng productId, bảo đảm quantity >= 1
        List<Map<String, Object>> itemsPayload = items.stream()
                .collect(Collectors.groupingBy(
                        CreateOrderItemCommand::getProductId,
                        LinkedHashMap::new,
                        Collectors.summingInt(i -> Optional.ofNullable(i.getQuantity()).orElse(0))))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("productId", e.getKey());
                    m.put("quantity", Math.max(1, e.getValue()));
                    return m;
                })
                .collect(Collectors.toList());
        startCreateOrderSaga(orderId, restaurantId, itemsPayload);
    }

    // ===== Publish VALIDATE_MENU_ITEMS bằng SagaEnvelope =====
    public void startCreateOrderSaga(String orderId,
            String restaurantId,
            List<Map<String, Object>> itemsPayload) {
        if (orderId == null || orderId.isBlank())
            throw new IllegalArgumentException("orderId is required");
        if (restaurantId == null || restaurantId.isBlank())
            throw new IllegalArgumentException("restaurantId is required");
        if (itemsPayload == null || itemsPayload.isEmpty())
            throw new IllegalArgumentException("items are required");

        String sagaId = UUID.randomUUID().toString();
        String corrId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("restaurantId", restaurantId); // ✅ PAYLOAD có restaurantId
        payload.put("items", itemsPayload);

        SagaEnvelope env = SagaEnvelope.builder()
                .eventType("VALIDATE_MENU_ITEMS")
                .orderId(orderId)
                .restaurantId(restaurantId) // ✅ ROOT có restaurantId (String)
                .payload(payload)
                .timestamp(Instant.now().toString())
                .build();

        String json = toJson(env);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId);
        headers.put("correlationId", corrId);
        headers.put("replyTo", "order.saga.reply");
        headers.put("eventType", "VALIDATE_MENU_ITEMS");
        headers.put("restaurantId", restaurantId);

        log.info("[SAGA->RESTAURANT] topic={} key={} headers={} payload={}",
                "restaurant.validate.command", orderId, headers, json);

        try {
            publisher.publish("restaurant.validate.command", orderId, json, headers); // đã có retry
        } catch (RuntimeException ex) {
            // Không rethrow để request không fail ngay; chỉ log để theo dõi retry trong
            // logs
            log.error("[SAGA] publish VALIDATE_MENU_ITEMS failed, will keep retrying via publisher. orderId={} err={}",
                    orderId, ex.toString(), ex);
        }

    }

    // ===== Reply handler (giữ nguyên) =====
    public void onReply(ConsumerRecord<String, String> rec) throws Exception {
        String event = header(rec, "eventType");
        var root = om.readTree(rec.value());
        String orderId = root.path("orderId").asText();

        switch (event) {
            case "RESTAURANT_ITEMS_VALIDATED" -> callPaymentAuthorize(rec, orderId, root);
            // case "RESTAURANT_ITEMS_INVALID" -> cancelOrder(orderId, "menu invalid");
            // case "PAYMENT_AUTHORIZED" -> confirmOrder(orderId);
            // case "PAYMENT_FAILED" -> cancelOrder(orderId, "payment failed");
            default -> log.warn("[SAGA] Unknown eventType={} for orderId={}", event, orderId);
        }
    }

    private void callPaymentAuthorize(ConsumerRecord<String, String> rec,
            String orderId,
            com.fasterxml.jackson.databind.JsonNode root) {
        String sagaId = header(rec, "sagaId");

        SagaEnvelope env = SagaEnvelope.builder()
                .eventType("AUTHORIZE_PAYMENT")
                .orderId(orderId)
                .payload(Map.of("amount", root.path("payload").path("total").asText("0")))
                .timestamp(Instant.now().toString())
                .build();

        String json = toJson(env);

        Map<String, String> headers = Map.of(
                "sagaId", sagaId,
                "correlationId", UUID.randomUUID().toString(),
                "replyTo", "order.saga.reply",
                "eventType", "AUTHORIZE_PAYMENT");

        log.info("[SAGA->PAYMENT] topic={} key={} headers={} payload={}",
                "payment.authorize.command", orderId, headers, json);

        publisher.publish("payment.authorize.command", orderId, json, headers);
    }

    private void confirmOrderPaid(OrderId orderId) {
        Order order = orderRepositoryPort.findById(orderId);
        if(order == null) {
            log.warn("[IDEMPOTENCY] Order {} not found in DB during AUTHORIZED. Assuming it was deleted/finalized. Skipping.", orderId);
            return; // Rất quan trọng: Thoát để Kafka Offset được commit.
        }
        if(order.getStatus() == OrderStatus.PAID) {
            log.warn("[IDEMPOTENCY] Order {} is already Paid. Skipping redundant cancellation.", orderId);
            return; 
        }
        confirmOrderPaidUseCase.confirm(order);
    }

    private void cancelOrder(OrderId orderId) {
        Order order = orderRepositoryPort.findById(orderId);
        // if(order == null) {
        //     log.warn("[IDEMPOTENCY] Order {} not found in DB. Assuming it was already finalized or deleted. Skipping.", orderId);
        //     return; // Thoát và cho phép Kafka commit offset
        // }
        // if (order.getStatus() == OrderStatus.CANCELED) { 
        //     log.warn("[IDEMPOTENCY] Order {} is already CANCELLED. Skipping redundant cancellation.", orderId);
        //     return; 
        // }
        canceledOrderUseCase.canceled(order);

    }

    // ===== helpers =====
    private String header(ConsumerRecord<?, ?> rec, String key) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startCreateOrderSaga(Long orderId, // 🛑 SỬA THÀNH LONG
            BigDecimal totalAmount,
            Long userId) { // 🛑 SỬA THÀNH LONG

        // 🛑 SỬA CÁC KIỂM TRA NULL CHO LONG (Không dùng isBlank)
        if (orderId == null)
            throw new IllegalArgumentException("orderId is required");
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }
        if (userId == null)
            throw new IllegalArgumentException("userId is required");

        // Chuyển đổi ID sang String để dùng làm Kafka Key và Headers
        String orderIdStr = orderId.toString();
        String userIdStr = userId.toString();

        String sagaId = UUID.randomUUID().toString();
        String corrId = UUID.randomUUID().toString();

        AuthorizePaymentCommandData env = AuthorizePaymentCommandData.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(totalAmount)
                .build();

        String json = toJson(env);

        // 🛑 2. Gán Headers (Yêu cầu String)s
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId);
        headers.put("correlationId", corrId);
        headers.put("replyTo", "order.saga.reply");
        headers.put("eventType", "AUTHORIZE_PAYMENT");
        headers.put("userId", userIdStr); // 🛑 Sử dụng String

        // 🛑 3. Gửi lệnh đến Topic của Payment Service
        log.info("[SAGA->PAYMENT] topic={} key={} headers={} payload={}",
                "payment.authorize.command", orderIdStr, headers, json);

        try {
            // Sử dụng orderIdStr (String) làm Kafka Key
            publisher.publish("payment.command", orderIdStr, json, headers);
        } catch (RuntimeException ex) {
            log.error("[SAGA] publish AUTHORIZE_PAYMENT failed, will keep retrying via publisher. orderId={} err={}",
                    orderIdStr, ex.toString(), ex);
        }
    }

    @KafkaListener(topics = "order.saga.reply", groupId = "order-service-group")
    @Transactional
    public void handlePaymentResponse(PaymentResponseData response, @Headers Map<String, Object> headers) {
        try {


            // 1. Trích xuất Metadata cần thiết từ Headers
            String sagaId = (String) headers.get("sagaId");
            String corrId = (String) headers.get("correlationId");
            // UserId trong header là String, cần chuyển sang Long nếu DTO yêu cầu
            Long userId = Long.valueOf((String) headers.get("userId"));

            OrderId orderId = new OrderId(response.orderId());
            switch (response.status()) {
                case "AUTHORIZED":
                    System.out.println("1111111111111111111111111111111111111111111111111111111111");
                    log.info("[SAGA] Payment success for order {}. Proceeding to Inventory/Confirmation.", orderId);
                    confirmOrderPaid(orderId); // Bỏ comment và gọi hàm
                    BigDecimal totalAmount = response.amount();

                    sendCancelPaymentCommand(
                    response.orderId(),
                    totalAmount,
                    userId,
                    sagaId,
                    corrId);
                    break;
                case "FAILED":
                    System.out.println("2222222222222222222222222222222222222222222222222222222222222222222");
                    log.warn("[SAGA] Payment failed for order {}. Cancelling Order. Reason: {}", orderId,
                            response.reason());
                    cancelOrder(orderId);
                    break;
                case "REFUND_COMPLETED":
                    System.out.println("3333333333333333333333333333333333333333333333333333333333333333333333333");
                    log.info("[SAGA] Payment Refund Completed for order {}. Finalizing Order Cancellation.", orderId);
                    cancelOrder(orderId);
                    break;
                default:
                    log.warn("[SAGA] Unknown payment status {} for order {}", response.status(), orderId);
            }
            // ... (Giữ nguyên try-catch an toàn đã thêm)
        } catch (NumberFormatException e) {
            log.error("Lỗi chuyển đổi Order ID từ {} thành số. Bỏ qua tin nhắn.", response.orderId(), e);
            return;
        } catch (Exception e) {
            // log.error("Lỗi hệ thống khi xử lý phản hồi thanh toán cho order {}. Đang thử lại.", response.orderId(), e);
            return;
        }

        return;
    }

    public void sendCancelPaymentCommand(Long orderId,
            BigDecimal totalAmount,
            Long userId,
            String sagaId,
            String corrId) {

        // 🛑 Kiểm tra tính hợp lệ cơ bản
        if (orderId == null || userId == null)
            throw new IllegalArgumentException("orderId and userId are required for cancellation.");

        // Chuyển đổi ID sang String để dùng làm Kafka Key và Headers
        String orderIdStr = orderId.toString();
        String userIdStr = userId.toString();

        AuthorizePaymentCommandData commandData = AuthorizePaymentCommandData.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(totalAmount)
                .build();

        String jsonPayload = toJson(commandData);

        // 2. Gán Headers
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId);
        headers.put("correlationId", corrId);

        // 🛑 Thay đổi EventType: Đây là lệnh BỒI THƯỜNG
        headers.put("eventType", "CANCEL_PAYMENT");

        // Giữ nguyên replyTo để lắng nghe kết quả hủy
        headers.put("replyTo", "order.saga.reply");
        headers.put("userId", userIdStr);

        // 3. Gửi lệnh đến Topic chung của Payment Service
        String topic = "payment.command"; // Dùng topic chung đã sửa

        log.info("[SAGA->PAYMENT] Compensation Command: topic={} key={} eventType={} payload={}",
                topic, orderIdStr, headers.get("eventType"), jsonPayload);

        try {
            publisher.publish(topic, orderIdStr, jsonPayload, headers);
        } catch (RuntimeException ex) {
            log.error("[SAGA] publish CANCEL_PAYMENT failed, will keep retrying. orderId={} err={}",
                    orderIdStr, ex.toString(), ex);
        }
    }
}

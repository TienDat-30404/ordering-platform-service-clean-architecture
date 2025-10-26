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
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
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
        confirmOrderPaidUseCase.confirm(order);
    }

    private void cancelOrder(OrderId orderId) {
        Order order = orderRepositoryPort.findById(orderId);
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

        // 🛑 1. Tạo Payload cho Command AUTHORIZE_PAYMENT (Payload sử dụng giá trị
        // Long)
        // Map<String, Object> payload = new LinkedHashMap<>();
        // payload.put("amount", totalAmount);
        // payload.put("userId", userId); // ✅ Giữ là Long trong Payload nếu JSON
        // serializer hỗ trợ

        // SagaEnvelope env = SagaEnvelope.builder()
        // .eventType("AUTHORIZE_PAYMENT")
        // .orderId(orderIdStr) // 🛑 Gán String
        // .restaurantId(restaurantIdStr) // 🛑 Gán String
        // .payload(payload)
        // .timestamp(Instant.now().toString())
        // .build();

        AuthorizePaymentCommandData env = AuthorizePaymentCommandData.builder()
                .orderId(orderId)
                .userId(userId)
                .amount(totalAmount)
                .build();

        String json = toJson(env);

        // 🛑 2. Gán Headers (Yêu cầu String)
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
            publisher.publish("payment.authorize.command", orderIdStr, json, headers);
        } catch (RuntimeException ex) {
            log.error("[SAGA] publish AUTHORIZE_PAYMENT failed, will keep retrying via publisher. orderId={} err={}",
                    orderIdStr, ex.toString(), ex);
        }
    }

    @KafkaListener(topics = "orchestrator.payment.response", groupId = "order-service-group")
    @Transactional
    public void handlePaymentResponse(PaymentResponseData response) {
        try {
            log.info("[SAGA] Received Payment response for order {} Status: {}", response.orderId(), response.status());

            OrderId orderId = new OrderId(response.orderId()); // Giả sử orderId là Long

            switch (response.status()) {
                case "AUTHORIZED":
                    log.info("[SAGA] Payment success for order {}. Proceeding to Inventory/Confirmation.", orderId);
                    confirmOrderPaid(orderId); // Bỏ comment và gọi hàm
                    break;
                case "FAILED":
                    log.warn("[SAGA] Payment failed for order {}. Cancelling Order. Reason: {}", orderId,
                            response.reason());
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
            log.error("Lỗi hệ thống khi xử lý phản hồi thanh toán cho order {}. Đang thử lại.", response.orderId(), e);
            return;
        }

        // log.info("--------------------------------------------------------------------------------");
        // log.info("[TEST-RECEIVE] ĐÃ NHẬN PHẢN HỒI PAYMENT TỪ KAFKA!");
        // log.info("[TEST-RECEIVE] Order ID: {}", response.orderId());
        // log.info("[TEST-RECEIVE] Status: {}", response.status());
        // log.info("[TEST-RECEIVE] Amount: {}", response.amount());
        // log.info("[TEST-RECEIVE] Reason: {}", response.reason());
        // log.info("[TEST-RECEIVE] Dữ liệu PaymentResponseData nhận được: {}",
        // response.toString());
        // log.info("--------------------------------------------------------------------------------");

        // Sau khi log, chúng ta sẽ return ngay lập tức để bỏ qua logic nghiệp vụ
        // (switch/case)
        // và tránh lỗi do thiếu các hàm confirmOrderPaid/cancelOrder.
        return;
    }
}

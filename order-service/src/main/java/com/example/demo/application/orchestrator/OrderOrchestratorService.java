package com.example.demo.application.orchestrator;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.dto.saga.AuthorizePaymentCommandData;
import com.example.demo.application.ports.input.CanceledOrderUseCase;
import com.example.demo.application.ports.input.ConfirmOrderPaidUseCase;
import com.example.demo.application.ports.input.UpdateOrderStatusUseCase;
import com.example.common_dtos.enums.OrderStatus;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.ports.output.OrderPublisher.OrderEventPublisher;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.event.SagaEnvelope;
import com.example.demo.domain.valueobject.order.OrderId;
// import com.example.demo.domain.valueobject.order.OrderStatus;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.example.common_dtos.enums.SagaStatus;
import com.example.common_dtos.enums.Topics;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestratorService {

    private final ConfirmOrderPaidUseCase confirmOrderPaidUseCase;
    private final CanceledOrderUseCase canceledOrderUseCase;
    private final OrderRepositoryPort orderRepositoryPort;
    private final java.util.Map<String, java.util.List<java.util.Map<String,Object>>> pendingItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> pendingRestaurant = new java.util.concurrent.ConcurrentHashMap<>();
    private final OrderRepositoryPort orderRepository;
    private final RestaurantDataProviderPort restaurantData;
    private final UpdateOrderStatusUseCase updateOrderStatus;
    private final OrderEventPublisher publisher;
    private final ObjectMapper om = new ObjectMapper();

    // ===== Controller có thể truyền Long hoặc String =====
    public void startCreateOrderSagaFromCommand(String orderId,
                                                Long restaurantId,
                                                List<CreateOrderItemCommand> items) {
        if (restaurantId == null) throw new IllegalArgumentException("restaurantId is required");
        startCreateOrderSagaFromCommand(orderId, String.valueOf(restaurantId), items);
    }

    public void startCreateOrderSagaFromCommand(String orderId,
                                                String restaurantId,
                                                List<CreateOrderItemCommand> items) {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        if (restaurantId == null || restaurantId.isBlank()) throw new IllegalArgumentException("restaurantId is required");
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("items are required");

        // Gom item trùng productId, bảo đảm quantity >= 1
        List<Map<String, Object>> itemsPayload = items.stream()
                .collect(Collectors.groupingBy(
                        CreateOrderItemCommand::getProductId,
                        LinkedHashMap::new,
                        Collectors.summingInt(i -> Optional.ofNullable(i.getQuantity()).orElse(0))
                ))
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
    public void startCreateOrderSaga(String orderId, String restaurantId, List<Map<String, Object>> itemsPayload) {
        // validate input như cũ...
        pendingItems.put(orderId, itemsPayload);
        pendingRestaurant.put(orderId, restaurantId);
        String sagaId = UUID.randomUUID().toString();
        String corrId = UUID.randomUUID().toString();

        // ✅ TÍNH SỐ TIỀN TẠM TÍNH ĐỂ HOLD
        var amount = quoteTotal(restaurantId, itemsPayload);

        var env = SagaEnvelope.builder()
                .eventType("AUTHORIZE_PAYMENT")
                .orderId(orderId)
                .restaurantId(restaurantId)
                .payload(Map.of("amount", amount.toPlainString()))
                .timestamp(Instant.now().toString())
                .build();

        String json = toJson(env);

        Map<String,String> headers = Map.of(
                "sagaId", sagaId,
                "correlationId", corrId,
                "replyTo", Topics.ORDER_SAGA_REPLY,
                "eventType", "AUTHORIZE_PAYMENT"
        );

        log.info("[SAGA->PAYMENT] (FIRST) topic={} key={} amount={} headers={} payload={}",
                Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, amount, headers, json);

        try {
            publisher.publish(Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, json, headers);
        } catch (RuntimeException ex) {
            log.error("[SAGA] publish AUTHORIZE_PAYMENT failed. orderId={} err={}", orderId, ex.toString(), ex);
        }

        // 👉 Sau khi payment OK, ta mới gửi VALIDATE_MENU_ITEMS trong onReply(...)
    }

    // ===== Reply handler (giữ nguyên + log SagaStatus) =====
    public void onReply(ConsumerRecord<String, String> rec) throws Exception {
        String event = header(rec, "eventType");
        String sagaId = header(rec, "sagaId");
        var root = om.readTree(rec.value());
        String orderId = root.path("orderId").asText();

        SagaStatus sagaStatus = mapEventToSagaStatus(event);
        log.info("[SAGA<-REPLY] eventType={} sagaStatus={} sagaId={} orderId={} raw={}",
                event, sagaStatus, sagaId, orderId, rec.value());

        switch (event) {
            case "PAYMENT_AUTHORIZED" -> {
                updateOrderStatus.setStatus(orderId, OrderStatus.PAID); // hold thành công
                callRestaurantValidate(rec, orderId);                   //validate sau payment
            }
            case "PAYMENT_FAILED" -> {
                updateOrderStatus.setStatus(orderId, OrderStatus.CANCELLED);
                // cancelOrder(orderId, "payment failed");
            }
            case "RESTAURANT_ITEMS_VALIDATED" -> {
                updateOrderStatus.setStatus(orderId, OrderStatus.APPROVED);
                callRestaurantStartPreparation(rec, orderId);
            }
            case "RESTAURANT_ITEMS_INVALID" -> {
                //BỒI HOÀN HOLD
                callPaymentCancel(rec, orderId, "menu invalid");
                updateOrderStatus.setStatus(orderId, OrderStatus.CANCELLING);
            }
            case "PAYMENT_REFUNDED", "PAYMENT_CANCELED" -> {
                updateOrderStatus.setStatus(orderId, OrderStatus.CANCELLED);
                // cancelOrder(orderId, "payment void/refund after invalid menu");
            }
            case "RESTAURANT_PREPARING" -> {
                log.info("[SAGA] Order {} PREPARING", orderId);
                callRestaurantCompleteOrder(rec, orderId);
            }
            case "RESTAURANT_COMPLETED" -> {
                updateOrderStatus.setStatus(orderId, OrderStatus.COMPLETED);
                callRestaurantDeductStock(rec, orderId);
                confirmOrder(orderId);
            }

            default -> log.warn("[SAGA] Unknown eventType={} for orderId={}", event, orderId);
        }

    }

    private void callPaymentAuthorize(ConsumerRecord<String,String> rec,
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
                "replyTo", Topics.ORDER_SAGA_REPLY,
                "eventType", "AUTHORIZE_PAYMENT"
        );

        log.info("[SAGA->PAYMENT] topic={} key={} headers={} payload={}",
                Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, headers, json);

        publisher.publish(Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, json, headers);
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

    private void confirmOrder(String orderId) {
        log.info("[SAGA] Order {} confirmed", orderId);
    }

    private void cancelOrder(String orderId, String reason) {
        log.warn("[SAGA] Order {} cancelled: {}", orderId, reason);
    }

    // ===== helpers =====
    private String header(ConsumerRecord<?,?> rec, String key) {
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
            System.out.println("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" + totalAmount);
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
                "payment.command", orderIdStr, headers, json);

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

            System.out.println("//////////////////////////////////////" + headers);
            // 1. Trích xuất Metadata cần thiết từ Headers
            // String sagaId = (String) headers.get("sagaId");
            // String corrId = (String) headers.get("correlationId");
            // // UserId trong header là String, cần chuyển sang Long nếu DTO yêu cầu
            // Long userId = Long.valueOf((String) headers.get("userId"));

            OrderId orderId = new OrderId(response.orderId());
            switch (response.status()) {
                case "AUTHORIZED":
                    System.out.println("1111111111111111111111111111111111111111111111111111111111");
                    log.info("[SAGA] Payment success for order {}. Proceeding to Inventory/Confirmation.", orderId);
                    confirmOrderPaid(orderId); // Bỏ comment và gọi hàm

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
            log.error("Lỗi hệ thống khi xử lý phản hồi thanh toán cho order {}. Đang thử lại.", response.orderId(), e);
            return;
        }

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

    private SagaStatus mapEventToSagaStatus(String eventType) {
        if (eventType == null) return SagaStatus.UNKNOWN;
        return switch (eventType) {
            case "RESTAURANT_ITEMS_VALIDATED" -> SagaStatus.RESTAURANT_VALIDATION_OK;
            case "RESTAURANT_ITEMS_INVALID"   -> SagaStatus.RESTAURANT_VALIDATION_FAIL;
            case "PAYMENT_AUTHORIZED"         -> SagaStatus.PAYMENT_AUTHORIZED;
            case "PAYMENT_FAILED"             -> SagaStatus.PAYMENT_FAILED;
            default                           -> SagaStatus.UNKNOWN;
        };
    }
    private void callRestaurantStartPreparation(ConsumerRecord<String,String> rec, String orderId) {
        String sagaId = header(rec, "sagaId");

        Map<String, Object> env = Map.of(
                "eventType", "START_PREPARATION",
                "orderId", orderId,
                "payload", Map.of(),
                "timestamp", Instant.now().toString()
        );
        String json = toJson(env);

        Map<String, String> headers = Map.of(
                "sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId,
                "correlationId", UUID.randomUUID().toString(),
                "replyTo", Topics.ORDER_SAGA_REPLY,
                "eventType", "START_PREPARATION"
        );

        log.info("[SAGA->RESTAURANT] topic={} key={} headers={} payload={}",
                Topics.RESTAURANT_FULFILL_COMMAND, orderId, headers, json);

        publisher.publish(Topics.RESTAURANT_FULFILL_COMMAND, orderId, json, headers);
    }
    private void callRestaurantCompleteOrder(ConsumerRecord<String,String> rec, String orderId) {
        String sagaId = header(rec, "sagaId");

        Map<String, Object> env = Map.of(
                "eventType", "COMPLETE_ORDER",
                "orderId", orderId,
                "payload", Map.of(),
                "timestamp", Instant.now().toString()
        );
        String json = toJson(env);

        Map<String, String> headers = Map.of(
                "sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId,
                "correlationId", UUID.randomUUID().toString(),
                "replyTo", Topics.ORDER_SAGA_REPLY,
                "eventType", "COMPLETE_ORDER"
        );

        log.info("[SAGA->RESTAURANT] COMPLETE_ORDER topic={} key={} headers={} payload={}",
                Topics.RESTAURANT_FULFILL_COMMAND, orderId, headers, json);

        publisher.publish(Topics.RESTAURANT_FULFILL_COMMAND, orderId, json, headers);
    }

    private void callRestaurantValidate(org.apache.kafka.clients.consumer.ConsumerRecord<String,String> rec, String orderId) {
        String sagaId = header(rec, "sagaId");
        try {
            String restaurantId = pendingRestaurant.get(orderId);
            var itemsPayload = pendingItems.get(orderId);

            if (restaurantId == null) {
                var order = orderRepository.findById(new com.example.demo.domain.valueobject.order.OrderId(Long.valueOf(orderId)));
                restaurantId = String.valueOf(order.getRestaurantId().value());
            }
            if (itemsPayload == null || itemsPayload.isEmpty()) {
                itemsPayload = buildItemsPayloadFromOrder(orderId);
            }

            log.info("[SAGA] VALIDATE using restaurantId={} items={}", restaurantId, itemsPayload);

            var env = new java.util.LinkedHashMap<String,Object>();
            env.put("eventType", "VALIDATE_MENU_ITEMS");
            env.put("orderId", orderId);
            env.put("restaurantId", restaurantId);
            env.put("payload", java.util.Map.of(
                    "restaurantId", restaurantId,
                    "items", itemsPayload,
                    "checkStock", true
            ));
            env.put("timestamp", java.time.Instant.now().toString());

            String json = toJson(env);

            java.util.Map<String,String> headers = java.util.Map.of(
                    "sagaId", sagaId == null ? java.util.UUID.randomUUID().toString() : sagaId,
                    "correlationId", java.util.UUID.randomUUID().toString(),
                    "replyTo", com.example.common_dtos.enums.Topics.ORDER_SAGA_REPLY,
                    "eventType", "VALIDATE_MENU_ITEMS",
                    "restaurantId", restaurantId
            );

            log.info("[SAGA->RESTAURANT] VALIDATE_MENU_ITEMS topic={} key={} headers={} payload={}",
                    com.example.common_dtos.enums.Topics.RESTAURANT_VALIDATE_COMMAND, orderId, headers, json);

            publisher.publish(com.example.common_dtos.enums.Topics.RESTAURANT_VALIDATE_COMMAND, orderId, json, headers);
        } catch (Throwable ex) {
            log.error("[SAGA] callRestaurantValidate failed for orderId={} err={}", orderId, ex.toString(), ex);
        }
    }


    private void clearPending(String orderId) {
        pendingItems.remove(orderId);
        pendingRestaurant.remove(orderId);
    }

    private void callPaymentCancel(ConsumerRecord<String,String> rec, String orderId, String reason) {
        String sagaId = header(rec, "sagaId");
        var env = Map.of(
                "eventType", "CANCEL_AUTHORIZATION",
                "orderId", orderId,
                "payload", Map.of("reason", reason),
                "timestamp", Instant.now().toString()
        );
        String json = toJson(env);

        Map<String,String> headers = Map.of(
                "sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId,
                "correlationId", UUID.randomUUID().toString(),
                "replyTo", Topics.ORDER_SAGA_REPLY,
                "eventType", "CANCEL_AUTHORIZATION"
        );

        log.info("[SAGA->PAYMENT] CANCEL_AUTHORIZATION topic={} key={} headers={} payload={}",
                Topics.PAYMENT_CANCEL_COMMAND, orderId, headers, json);

        publisher.publish(Topics.PAYMENT_CANCEL_COMMAND, orderId, json, headers);
    }

    private java.math.BigDecimal quoteTotal(String restaurantId, java.util.List<java.util.Map<String,Object>> itemsPayload) {
        var ids = itemsPayload.stream()
                .map(m -> Long.valueOf(String.valueOf(m.get("productId"))))
                .toList();

        // ✅ gọi port đã implement
        java.util.List<ProductDetailData> details =
                restaurantData.getProducts(Long.valueOf(restaurantId), ids);

        var priceById = new java.util.HashMap<Long, java.math.BigDecimal>();
        if (details != null) {
            for (ProductDetailData d : details) {
                if (d != null && d.getId() != null && d.getPrice() != null) {
                    priceById.put(d.getId(), d.getPrice());
                }
            }
        }

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (var m : itemsPayload) {
            Long pid = Long.valueOf(String.valueOf(m.get("productId")));
            int qty  = Integer.parseInt(String.valueOf(m.get("quantity")));
            var price = priceById.getOrDefault(pid, java.math.BigDecimal.ZERO);
            total = total.add(price.multiply(java.math.BigDecimal.valueOf(qty)));
        }
        return total;
    }

    private List<Map<String, Object>> buildItemsPayloadFromOrder(String orderId) {
        var order = orderRepository.findById(
                new com.example.demo.domain.valueobject.order.OrderId(Long.valueOf(orderId))
        );
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("[SAGA] No items found for orderId={} when building payload", orderId);
            return java.util.List.of();
        }

        List<Map<String, Object>> items = new java.util.ArrayList<>();
        for (var oi : order.getItems()) {
            Long pid = null;
            try {
                // ưu tiên getProductId().value()
                try {
                    var method = oi.getClass().getMethod("getProductId");
                    var v = method.invoke(oi);
                    if (v != null) {
                        try {
                            var m2 = v.getClass().getMethod("value");
                            Object idVal = m2.invoke(v);
                            if (idVal != null) pid = Long.valueOf(String.valueOf(idVal));
                        } catch (NoSuchMethodException ignore) {
                            pid = Long.valueOf(String.valueOf(v));
                        }
                    }
                } catch (NoSuchMethodException ignore) {
                    // fallback: getProduct().getId()/getProductId() [.value()]
                    try {
                        var mProd = oi.getClass().getMethod("getProduct");
                        Object prod = mProd.invoke(oi);
                        if (prod != null) {
                            Object pidObj;
                            try {
                                pidObj = prod.getClass().getMethod("getProductId").invoke(prod);
                            } catch (NoSuchMethodException e2) {
                                pidObj = prod.getClass().getMethod("getId").invoke(prod);
                            }
                            if (pidObj != null) {
                                try {
                                    var mVal = pidObj.getClass().getMethod("value");
                                    Object idVal = mVal.invoke(pidObj);
                                    if (idVal != null) pid = Long.valueOf(String.valueOf(idVal));
                                } catch (NoSuchMethodException e3) {
                                    pid = Long.valueOf(String.valueOf(pidObj));
                                }
                            }
                        }
                    } catch (Exception ex2) {
                        log.debug("[SAGA] Cannot reflect productId on item: {}", ex2.toString());
                    }
                }

                Integer qty = 1;
                try {
                    var mQty = oi.getClass().getMethod("getQuantity");
                    Object q = mQty.invoke(oi);
                    if (q != null) qty = Integer.valueOf(String.valueOf(q));
                } catch (NoSuchMethodException e) {
                    log.debug("[SAGA] No getQuantity() on item, default=1");
                }

                if (pid != null) {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("productId", pid);
                    m.put("quantity", Math.max(1, qty));
                    items.add(m);
                } else {
                    log.warn("[SAGA] Skip item without productId in orderId={}", orderId);
                }
            } catch (Exception ex) {
                log.error("[SAGA] Error extracting item for orderId={} err={}", orderId, ex.toString(), ex);
            }
        }
        return items;
    }

    private void callRestaurantDeductStock(ConsumerRecord<String,String> rec, String orderId) {
        String sagaId = header(rec, "sagaId");
        try {
            // lấy items từ cache; thiếu thì dựng lại từ DB
            var itemsPayload = pendingItems.get(orderId);
            if (itemsPayload == null || itemsPayload.isEmpty()) {
                itemsPayload = buildItemsPayloadFromOrder(orderId);
            }

            var env = new LinkedHashMap<String, Object>();
            env.put("eventType", "DEDUCT_STOCK");
            env.put("orderId", orderId);
            env.put("payload", Map.of("items", itemsPayload));
            env.put("timestamp", Instant.now().toString());

            String json = toJson(env);

            Map<String, String> headers = Map.of(
                    "sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId,
                    "correlationId", UUID.randomUUID().toString(),
                    "replyTo", Topics.ORDER_SAGA_REPLY,
                    "eventType", "DEDUCT_STOCK"
            );

            log.info("[SAGA->RESTAURANT] DEDUCT_STOCK topic={} key={} headers={} payload={}",
                    Topics.RESTAURANT_FULFILL_COMMAND, orderId, headers, json);

            publisher.publish(Topics.RESTAURANT_FULFILL_COMMAND, orderId, json, headers);

            // sau khi deduct thì có thể clear cache tạm
            clearPending(orderId);
        } catch (Throwable ex) {
            log.error("[SAGA] callRestaurantDeductStock failed for orderId={} err={}", orderId, ex.toString(), ex);
        }
    }

}

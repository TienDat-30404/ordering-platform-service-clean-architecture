package com.example.demo.application.orchestrator;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.dto.saga.AuthorizePaymentCommandData;
import com.example.demo.application.ports.input.CanceledOrderUseCase;
import com.example.demo.application.ports.input.ConfirmOrderPaidUseCase;
import com.example.demo.application.ports.input.UpdateOrderStatusUseCase;
import com.example.demo.domain.valueobject.order.OrderStatus;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.ports.output.OrderPublisher.OrderEventPublisher;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.event.SagaEnvelope;
import com.example.demo.domain.valueobject.order.OrderId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import com.example.demo.domain.valueobject.saga.SagaStatus;
import com.example.common_dtos.enums.Topics;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestratorService {

    private final ConfirmOrderPaidUseCase confirmOrderPaidUseCase;
    private final CanceledOrderUseCase canceledOrderUseCase;
    private final OrderRepositoryPort orderRepositoryPort;
    private final java.util.Set<String> processedEvents = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    private final java.util.Map<String, java.util.List<java.util.Map<String,Object>>> pendingItems = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> pendingRestaurant = new java.util.concurrent.ConcurrentHashMap<>();
    private final OrderRepositoryPort orderRepository;
    private final RestaurantDataProviderPort restaurantData;
    private final UpdateOrderStatusUseCase updateOrderStatus;
    private final OrderEventPublisher publisher;
    private final ObjectMapper om = new ObjectMapper();

    // === NEW: track saga đang active cho từng orderId để lọc reply “dư âm” ===
    private final java.util.concurrent.ConcurrentHashMap<String, String> activeSagaByOrder = new java.util.concurrent.ConcurrentHashMap<>();

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

        // NEW: tao sagaId và ghi nhận “active”
        String sagaId = UUID.randomUUID().toString();
        activeSagaByOrder.put(orderId, sagaId);

        String corrId = UUID.randomUUID().toString();
        var amount = quoteTotal(restaurantId, itemsPayload);

        var env = SagaEnvelope.builder()
                .eventType("AUTHORIZE_PAYMENT")
                .orderId(orderId)
                .restaurantId(restaurantId)
                .payload(Map.of("amount", amount.toPlainString()))
                .timestamp(Instant.now().toString())
                .build();

        String json = toJson(env);

        // >>> THÊM userId/paymentId vào headers (nếu resolve được)
        String userIdHdr = resolveUserId(orderId);
        String paymentIdHdr = resolvePaymentId(orderId);

        Map<String,String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId);
        headers.put("correlationId", corrId);
        headers.put("replyTo", Topics.ORDER_SAGA_REPLY);
        headers.put("eventType", "AUTHORIZE_PAYMENT");
        if (userIdHdr != null)    headers.put("userId", userIdHdr);
        if (paymentIdHdr != null) headers.put("paymentId", paymentIdHdr);

        log.info("[SAGA->PAYMENT] (FIRST) topic={} key={} amount={} headers={} payload={}",
                Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, amount, headers, json);

        try {
            publisher.publish(Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, json, headers);
        } catch (RuntimeException ex) {
            log.error("[SAGA] publish AUTHORIZE_PAYMENT failed. orderId={} err={}", orderId, ex.toString(), ex);
        }

        //Sau khi payment OK, ta mới gửi VALIDATE_MENU_ITEMS trong onReply(...)
    }

    @KafkaListener(topics = Topics.ORDER_SAGA_REPLY, groupId = "order-service-group")
    public void onReply(ConsumerRecord<String, String> rec) {
        try {
            final String event = header(rec, "eventType");
            final String sagaId = header(rec, "sagaId");
            if (rec.value() == null || rec.value().isBlank()) {
                log.warn("[SAGA<-REPLY] empty payload, skip. headers={}", rec.headers());
                return;
            }

            var root = om.readTree(rec.value());
            final String orderId = root.path("orderId").asText(null);
            if (orderId == null || orderId.isBlank()) {
                log.warn("[SAGA<-REPLY] missing orderId in payload: {}", rec.value());
                return;
            }

            // === NEW FILTER LAYER 1: key phải khớp orderId ===
            String recordKey = rec.key();
            if (recordKey != null && !recordKey.equals(orderId)) {
                log.warn("[SAGA] Ignore reply: record.key={} != orderId={} (eventType={})", recordKey, orderId, event);
                return;
            }

            // === NEW FILTER LAYER 2: sagaId header phải khớp saga đang active ===
            String expected = activeSagaByOrder.get(orderId);
            if (expected == null) {
                log.warn("[SAGA] Ignore reply: no active saga for orderId={}, headerSaga={}, eventType={}", orderId, sagaId, event);
                return;
            }
            if (sagaId == null || !expected.equals(sagaId)) {
                log.warn("[SAGA] Ignore reply: mismatched sagaId for orderId={} expected={} actual={} (eventType={})",
                        orderId, expected, sagaId, event);
                return;
            }

            SagaStatus sagaStatus = mapEventToSagaStatus(event);
            log.info("[SAGA<-REPLY] eventType={} sagaStatus={} sagaId={} orderId={} raw={}",
                    event, sagaStatus, sagaId, orderId, rec.value());

            switch (event) {
                case "PAYMENT_AUTHORIZED" -> {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.PAID);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(PAID) failed but skip re-consume. orderId={} err={}",
                                orderId, ex.toString());
                    }
                    callRestaurantValidate(rec, orderId);
                }
                case "PAYMENT_FAILED" ->    {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.CANCELLED);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(CANCELLED) failed. orderId={} err={}", orderId, ex.toString());
                    }
                    cancelOrder(orderId, "payment failed");
                    // NEW: terminal → cleanup
                    activeSagaByOrder.remove(orderId);
                    clearPending(orderId);
                }
                case "RESTAURANT_ITEMS_VALIDATED" -> {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.APPROVED);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(APPROVED) failed. orderId={} err={}", orderId, ex.toString());
                    }
                    callRestaurantStartPreparation(rec, orderId);
                }
                case "RESTAURANT_ITEMS_INVALID" -> {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.CANCELLING);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(CANCELLING) failed. orderId={} err={}", orderId, ex.toString());
                    }
                    callPaymentCancel(rec, orderId, "menu invalid");
                }
                case "PAYMENT_REFUNDED", "PAYMENT_CANCELED" -> {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.CANCELLED);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(CANCELLED) failed. orderId={} err={}", orderId, ex.toString());
                    }
                    cancelOrder(orderId, "payment void/refund after invalid menu");
                    // NEW: terminal → cleanup
                    activeSagaByOrder.remove(orderId);
                    clearPending(orderId);
                }
                case "RESTAURANT_PREPARING" -> {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.PREPARING);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(PREPARING) failed. orderId={} err={}", orderId, ex.toString());
                    }
                    callRestaurantCompleteOrder(rec, orderId);
                }
                case "RESTAURANT_COMPLETED" -> {
                    try {
                        updateOrderStatus.setStatus(orderId, OrderStatus.COMPLETED);
                    } catch (Exception ex) {
                        log.warn("[SAGA] setStatus(COMPLETED) failed. orderId={} err={}", orderId, ex.toString());
                    }
                    callRestaurantDeductStock(rec, orderId);
                    confirmOrder(orderId);
                    // Chưa remove ở đây vì còn chờ DEDUCT_STOCK (nếu bạn muốn kết thúc tại đây thì bỏ comment dòng dưới)
                    // activeSagaByOrder.remove(orderId);
                }
                default -> {
                    log.warn("[SAGA] Unknown eventType={} for orderId={}, skip.", event, orderId);
                }
            }
        } catch (Exception e) {
            log.error("[SAGA] onReply crashed, skip re-consume. err={}", e.toString(), e);
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

        // >>> THÊM userId/paymentId vào headers
        String userIdHdr = resolveUserId(orderId);
        String paymentIdHdr = resolvePaymentId(orderId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId);
        headers.put("correlationId", UUID.randomUUID().toString());
        headers.put("replyTo", Topics.ORDER_SAGA_REPLY);
        headers.put("eventType", "AUTHORIZE_PAYMENT");
        if (userIdHdr != null)    headers.put("userId", userIdHdr);
        if (paymentIdHdr != null) headers.put("paymentId", paymentIdHdr);

        log.info("[SAGA->PAYMENT] topic={} key={} headers={} payload={}",
                Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, headers, json);

        publisher.publish(Topics.PAYMENT_AUTHORIZE_COMMAND, orderId, json, headers);
    }

    private void confirmOrderPaid(OrderId orderId) {
        Order order = orderRepositoryPort.findById(orderId);
        if(order == null) {
            log.warn("[IDEMPOTENCY] Order {} not found in DB during AUTHORIZED. Assuming it was deleted/finalized. Skipping.", orderId);
            return;
        }
        if(order.getStatus() == OrderStatus.PAID) {
            log.warn("[IDEMPOTENCY] Order {} is already Paid. Skipping redundant cancellation.", orderId);
            return;
        }
        confirmOrderPaidUseCase.confirm(order);
    }

    private void cancelOrder(OrderId orderId) {
        Order order = orderRepositoryPort.findById(orderId);
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

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId);
        headers.put("correlationId", UUID.randomUUID().toString());
        headers.put("replyTo", Topics.ORDER_SAGA_REPLY);
        headers.put("eventType", "START_PREPARATION");

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

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId);
        headers.put("correlationId", UUID.randomUUID().toString());
        headers.put("replyTo", Topics.ORDER_SAGA_REPLY);
        headers.put("eventType", "COMPLETE_ORDER");

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

            java.util.Map<String,String> headers = new LinkedHashMap<>();
            headers.put("sagaId", sagaId == null ? java.util.UUID.randomUUID().toString() : sagaId);
            headers.put("correlationId", java.util.UUID.randomUUID().toString());
            headers.put("replyTo", com.example.common_dtos.enums.Topics.ORDER_SAGA_REPLY);
            headers.put("eventType", "VALIDATE_MENU_ITEMS");
            headers.put("restaurantId", restaurantId);

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

        // >>> THÊM userId/paymentId vào headers
        String userIdHdr = resolveUserId(orderId);
        String paymentIdHdr = resolvePaymentId(orderId);

        Map<String,String> headers = new LinkedHashMap<>();
        headers.put("sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId);
        headers.put("correlationId", UUID.randomUUID().toString());
        headers.put("replyTo", Topics.ORDER_SAGA_REPLY);
        headers.put("eventType", "CANCEL_AUTHORIZATION");
        if (userIdHdr != null)    headers.put("userId", userIdHdr);
        if (paymentIdHdr != null) headers.put("paymentId", paymentIdHdr);

        log.info("[SAGA->PAYMENT] CANCEL_AUTHORIZATION topic={} key={} headers={} payload={}",
                Topics.PAYMENT_CANCEL_COMMAND, orderId, headers, json);

        publisher.publish(Topics.PAYMENT_CANCEL_COMMAND, orderId, json, headers);
    }

    private java.math.BigDecimal quoteTotal(String restaurantId, java.util.List<java.util.Map<String,Object>> itemsPayload) {
        var ids = itemsPayload.stream()
                .map(m -> Long.valueOf(String.valueOf(m.get("productId"))))
                .toList();

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

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId);
            headers.put("correlationId", UUID.randomUUID().toString());
            headers.put("replyTo", Topics.ORDER_SAGA_REPLY);
            headers.put("eventType", "DEDUCT_STOCK");

            log.info("[SAGA->RESTAURANT] DEDUCT_STOCK topic={} key={} headers={} payload={}",
                    Topics.RESTAURANT_FULFILL_COMMAND, orderId, headers, json);

            publisher.publish(Topics.RESTAURANT_FULFILL_COMMAND, orderId, json, headers);

            // sau khi deduct thì có thể clear cache tạm
            clearPending(orderId);
            // NEW: có thể coi như xong vòng đời
            activeSagaByOrder.remove(orderId);
        } catch (Throwable ex) {
            log.error("[SAGA] callRestaurantDeductStock failed for orderId={} err={}", orderId, ex.toString(), ex);
        }
    }

    // ===== resolve helpers (đã có sẵn) =====
    private String resolveUserId(String orderId) {
        try {
            var order = orderRepository.findById(
                    new com.example.demo.domain.valueobject.order.OrderId(Long.valueOf(orderId))
            );
            if (order == null) return null;

            // thử getUserId()
            try {
                var m = order.getClass().getMethod("getUserId");
                Object v = m.invoke(order);
                if (v != null) return String.valueOf(
                        v.getClass().getMethod("value").invoke(v)
                );
            } catch (NoSuchMethodException ignore) {}

            // thử getCustomerId()
            try {
                var m = order.getClass().getMethod("getCustomerId");
                Object v = m.invoke(order);
                if (v != null) return String.valueOf(
                        v.getClass().getMethod("value").invoke(v)
                );
            } catch (NoSuchMethodException ignore) {}

            // fallback: field userId / customerId
            try {
                var f = order.getClass().getDeclaredField("userId");
                f.setAccessible(true);
                Object v = f.get(order);
                if (v != null) return String.valueOf(
                        v.getClass().getMethod("value").invoke(v)
                );
            } catch (NoSuchFieldException ignore) {}
            try {
                var f = order.getClass().getDeclaredField("customerId");
                f.setAccessible(true);
                Object v = f.get(order);
                if (v != null) return String.valueOf(
                        v.getClass().getMethod("value").invoke(v)
                );
            } catch (NoSuchFieldException ignore) {}

        } catch (Throwable ex) {
            log.debug("[SAGA] resolveUserId failed for orderId={} err={}", orderId, ex.toString());
        }
        return null;
    }

    private String resolvePaymentId(String orderId) {
        try {
            var order = orderRepository.findById(
                    new com.example.demo.domain.valueobject.order.OrderId(Long.valueOf(orderId))
            );
            if (order == null) return null;

            // nếu Order có paymentId:
            try {
                var m = order.getClass().getMethod("getPaymentId");
                Object v = m.invoke(order);
                if (v != null) {
                    try {
                        Object idVal = v.getClass().getMethod("value").invoke(v);
                        return idVal != null ? String.valueOf(idVal) : String.valueOf(v);
                    } catch (NoSuchMethodException e) {
                        return String.valueOf(v);
                    }
                }
            } catch (NoSuchMethodException ignore) {}

            // hoặc có getPayment().getId()
            try {
                var mPay = order.getClass().getMethod("getPayment");
                Object pay = mPay.invoke(order);
                if (pay != null) {
                    Object pid;
                    try {
                        pid = pay.getClass().getMethod("getPaymentId").invoke(pay);
                    } catch (NoSuchMethodException e2) {
                        pid = pay.getClass().getMethod("getId").invoke(pay);
                    }
                    if (pid != null) {
                        try {
                            Object idVal = pid.getClass().getMethod("value").invoke(pid);
                            return idVal != null ? String.valueOf(idVal) : String.valueOf(pid);
                        } catch (NoSuchMethodException e3) {
                            return String.valueOf(pid);
                        }
                    }
                }
            } catch (NoSuchMethodException ignore) {}

        } catch (Throwable ex) {
            log.debug("[SAGA] resolvePaymentId failed for orderId={} err={}", orderId, ex.toString());
        }
        return null; // không có cũng OK, payment-service đã có find-or-create
    }
}

package com.example.demo.application.orchestrator;

import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.ports.output.OrderPublisher.OrderEventPublisher;
import com.example.demo.domain.event.SagaEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestratorService {

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
    public void startCreateOrderSaga(String orderId,
                                     String restaurantId,
                                     List<Map<String, Object>> itemsPayload) {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("orderId is required");
        if (restaurantId == null || restaurantId.isBlank()) throw new IllegalArgumentException("restaurantId is required");
        if (itemsPayload == null || itemsPayload.isEmpty()) throw new IllegalArgumentException("items are required");

        String sagaId = UUID.randomUUID().toString();
        String corrId = UUID.randomUUID().toString();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("restaurantId", restaurantId);           // ✅ PAYLOAD có restaurantId
        payload.put("items", itemsPayload);

        SagaEnvelope env = SagaEnvelope.builder()
                .eventType("VALIDATE_MENU_ITEMS")
                .orderId(orderId)
                .restaurantId(restaurantId)                   // ✅ ROOT có restaurantId (String)
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

        publisher.publish("restaurant.validate.command", orderId, json, headers);
    }

    // ===== Reply handler (giữ nguyên) =====
    public void onReply(ConsumerRecord<String, String> rec) throws Exception {
        String event = header(rec, "eventType");
        var root = om.readTree(rec.value());
        String orderId = root.path("orderId").asText();

        switch (event) {
            case "RESTAURANT_ITEMS_VALIDATED" -> callPaymentAuthorize(rec, orderId, root);
            case "RESTAURANT_ITEMS_INVALID"   -> cancelOrder(orderId, "menu invalid");
            case "PAYMENT_AUTHORIZED"         -> confirmOrder(orderId);
            case "PAYMENT_FAILED"             -> cancelOrder(orderId, "payment failed");
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
                "replyTo", "order.saga.reply",
                "eventType", "AUTHORIZE_PAYMENT"
        );

        log.info("[SAGA->PAYMENT] topic={} key={} headers={} payload={}",
                "payment.authorize.command", orderId, headers, json);

        publisher.publish("payment.authorize.command", orderId, json, headers);
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
        try { return om.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}

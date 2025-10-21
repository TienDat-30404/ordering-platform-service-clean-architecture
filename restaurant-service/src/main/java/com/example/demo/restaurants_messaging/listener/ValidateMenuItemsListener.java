package com.example.demo.restaurants_messaging.listener;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.ports.input.ValidateMenuItemUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidateMenuItemsListener {

    private static final String DEFAULT_REPLY_TOPIC = "order.saga.reply";

    private final KafkaTemplate<String, String> template;
    private final ValidateMenuItemUseCase validateMenuItemUseCase;
    private final ObjectMapper om = new ObjectMapper();

    /**
     * Hỗ trợ payload:
     * A) {"payload":{"items":[1,2,3]}}
     * B) {"payload":{"items":[{"productId":1,"quantity":2},{"productId":2,"quantity":1}]}}
     * C) {"payload":{"productIds":[1,2,3]}}
     * - restaurantId có thể ở: root.restaurantId (String), payload.restaurantId (String/Number), hoặc header "restaurantId"
     */
    @KafkaListener(topics = "restaurant.validate.command", groupId = "restaurant-service-group")
    public void onValidate(ConsumerRecord<String, String> rec) throws Exception {
        final String eventTypeHdr = header(rec, "eventType");
        final String sagaId = header(rec, "sagaId");
        final String replyToHdr = header(rec, "replyTo");
        final String restIdHdr = header(rec, "restaurantId");

        log.info("[RESTAURANT<-SAGA] key={} hdr.eventType={} hdr.sagaId={} hdr.replyTo={} hdr.restaurantId={} raw={}",
                rec.key(), eventTypeHdr, sagaId, replyToHdr, restIdHdr, rec.value());

        JsonNode root = om.readTree(rec.value());
        String orderId = root.path("orderId").asText(null);

        // payload có thể là object hoặc string JSON → resolve
        JsonNode payload = resolvePayloadNode(root.path("payload"));

        // Đọc restaurantId: root → payload → header
        Long restaurantId = toLongSafe(root.path("restaurantId").asText(null));
        if (restaurantId == null && payload != null && payload.hasNonNull("restaurantId")) {
            restaurantId = toLongSafe(payload.path("restaurantId").asText(null));
        }
        if (restaurantId == null) {
            restaurantId = toLongSafe(restIdHdr);
        }

        // Parse items linh hoạt
        ParseResult pr = parseItemsFlexible(payload);

        // replyTo fallback
        String replyTo = (replyToHdr == null || replyToHdr.isBlank()) ? DEFAULT_REPLY_TOPIC : replyToHdr;

        // Thiếu items → invalid
        if (pr.productIds.isEmpty()) {
            replyInvalid(replyTo, orderId, sagaId,
                    List.of(new ErrorProduct(null, "INVALID_REQUEST: missing items")));
            return;
        }

        // Gọi use case validate theo hợp đồng hiện tại (List<Long>)
        ItemValidationRequest req = new ItemValidationRequest();
        req.setRestaurantId(restaurantId);
        req.setMenuItemIds(new ArrayList<>(pr.productIds));

        List<ItemValidationResponse> results = validateMenuItemUseCase.validateItems(req);

        // Đánh giá kết quả
        Map<Long, ItemValidationResponse> byId = new HashMap<>();
        if (results != null) {
            for (ItemValidationResponse r : results) {
                if (r != null) byId.put(r.getMenuItemId(), r);
            }
        }

        List<ErrorProduct> errors = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        // id nào không có trong response → lỗi "not found"
        for (Long pid : pr.productIds) {
            if (!byId.containsKey(pid)) {
                errors.add(new ErrorProduct(pid, "Menu item not found in restaurant menu."));
            }
        }

        // cộng tổng các item hợp lệ
        if (results != null) {
            for (ItemValidationResponse r : results) {
                if (r == null) continue;
                Long pid = r.getMenuItemId();
                if (!Boolean.TRUE.equals(r.isValid())) {
                    errors.add(new ErrorProduct(pid, nvl(r.getReason(), "Invalid item")));
                    continue;
                }
                BigDecimal price = r.getPrice() == null ? BigDecimal.ZERO : r.getPrice();
                int quantity = pr.quantityByProductId.getOrDefault(pid, 1);
                total = total.add(price.multiply(BigDecimal.valueOf(quantity)));
            }
        }

        if (!errors.isEmpty()) {
            replyInvalid(replyTo, orderId, sagaId, errors);
            return;
        }

        // Reply VALID
        Map<String, Object> outPayload = Map.of(
                "eventType", "RESTAURANT_ITEMS_VALIDATED",
                "orderId", orderId,
                "payload", filterNulls(Map.of(
                        "total", total.toPlainString(),
                        "restaurantId", restaurantId
                )),
                "timestamp", Instant.now().toString()
        );

        String outJson = om.writeValueAsString(outPayload);
        log.info("[RESTAURANT->SAGA] VALID key={} sagaId={} toTopic={} payload={}", orderId, sagaId, replyTo, outJson);
        send(replyTo, orderId, sagaId, "RESTAURANT_ITEMS_VALIDATED", outJson);
    }

    // ---------------- helpers ----------------

    /** Nếu payload là text JSON → parse lại; nếu null/missing → {} */
    private JsonNode resolvePayloadNode(JsonNode rawPayload) throws Exception {
        if (rawPayload == null || rawPayload.isMissingNode() || rawPayload.isNull()) {
            return om.createObjectNode();
        }
        if (rawPayload.isTextual()) {
            String txt = rawPayload.asText();
            if (txt != null && !txt.isBlank() &&
                    (txt.trim().startsWith("{") || txt.trim().startsWith("["))) {
                return om.readTree(txt);
            }
            return om.createObjectNode();
        }
        return rawPayload;
    }

    /** Kết quả parse linh hoạt items ⇒ productIds (duy nhất, có thứ tự) + quantity map nếu có */
    private static final class ParseResult {
        final List<Long> productIds;
        final Map<Long, Integer> quantityByProductId;
        ParseResult(List<Long> productIds, Map<Long, Integer> quantityByProductId) {
            this.productIds = productIds;
            this.quantityByProductId = quantityByProductId;
        }
    }

    private ParseResult parseItemsFlexible(JsonNode payload) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        Map<Long, Integer> qtyMap = new LinkedHashMap<>();

        // Ưu tiên payload.items
        JsonNode itemsNode = payload.path("items");
        if (itemsNode.isArray() && itemsNode.size() > 0) {
            for (JsonNode n : itemsNode) {
                if (n.isNumber()) {
                    long pid = n.asLong();
                    ids.add(pid);
                    qtyMap.putIfAbsent(pid, 1);
                } else if (n.isObject()) {
                    if (n.hasNonNull("productId")) {
                        long pid = n.path("productId").asLong();
                        int q = Math.max(1, n.path("quantity").asInt(1));
                        ids.add(pid);
                        qtyMap.merge(pid, q, Integer::sum);
                    }
                }
            }
        }

        // Fallback: payload.productIds
        if (ids.isEmpty()) {
            JsonNode productIdsNode = payload.path("productIds");
            if (productIdsNode.isArray() && productIdsNode.size() > 0) {
                for (JsonNode n : productIdsNode) {
                    if (n.isNumber()) {
                        long pid = n.asLong();
                        ids.add(pid);
                        qtyMap.putIfAbsent(pid, 1);
                    }
                }
            }
        }
        return new ParseResult(new ArrayList<>(ids), qtyMap);
    }

    private void replyInvalid(String replyTo, String orderId, String sagaId, List<ErrorProduct> errors) throws Exception {
        Map<String, Object> outPayload = Map.of(
                "eventType", "RESTAURANT_ITEMS_INVALID",
                "orderId", orderId,
                "payload", Map.of(
                        "errors", errors,                         // [{productId, reason}]
                        "invalidIds", errors.stream()
                                .map(e -> e.productId)
                                .filter(Objects::nonNull)
                                .toList()
                ),
                "timestamp", Instant.now().toString()
        );
        String outJson = om.writeValueAsString(outPayload);
        log.info("[RESTAURANT->SAGA] INVALID key={} sagaId={} toTopic={} payload={}", orderId, sagaId, replyTo, outJson);
        send(replyTo, orderId, sagaId, "RESTAURANT_ITEMS_INVALID", outJson);
    }

    private void send(String topic, String key, String sagaId, String eventType, String payload) {
        ProducerRecord<String, String> out = new ProducerRecord<>(topic, key, payload);
        out.headers().add("sagaId", bytes(sagaId));
        out.headers().add("eventType", bytes(eventType));

        template.send(out).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[RESTAURANT->SAGA] SEND FAILED topic={} key={} sagaId={} eventType={} err={}",
                        topic, key, sagaId, eventType, ex.toString(), ex);
            } else {
                var md = result.getRecordMetadata();
                log.info("[RESTAURANT->SAGA] SENT OK topic={} partition={} offset={} key={} sagaId={} eventType={}",
                        md.topic(), md.partition(), md.offset(), key, sagaId, eventType);
            }
        });
    }

    private String header(ConsumerRecord<?,?> rec, String key) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private byte[] bytes(String s) {
        return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8);
    }

    private static String nvl(String s, String dft) { return (s == null || s.isBlank()) ? dft : s; }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> filterNulls(Map<String, Object> in) {
        Map<String, Object> out = new LinkedHashMap<>();
        in.forEach((k, v) -> { if (v != null) out.put(k, v); });
        return out;
    }

    // DTO nhẹ để trả lỗi (dùng productId)
    static final class ErrorProduct {
        public final Long productId;
        public final String reason;
        ErrorProduct(Long productId, String reason) {
            this.productId = productId;
            this.reason = reason;
        }
        public Long getProductId() { return productId; }
        public String getReason() { return reason; }
    }

    // ---- convert helper ----
    private Long toLongSafe(String s) {
        try {
            if (s == null || s.isBlank()) return null;
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

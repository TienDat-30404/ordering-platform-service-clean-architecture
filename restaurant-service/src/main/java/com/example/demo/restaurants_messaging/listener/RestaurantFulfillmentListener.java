package com.example.demo.restaurants_messaging.listener;

import com.example.common_dtos.enums.Topics;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestaurantFulfillmentListener {

    private final KafkaTemplate<String, String> template;
    private final ObjectMapper om = new ObjectMapper();

    @KafkaListener(topics = Topics.RESTAURANT_FULFILL_COMMAND, groupId = "restaurant-service-group")
    public void onFulfill(ConsumerRecord<String, String> rec) throws Exception {
        String eventType = header(rec, "eventType");
        String sagaId    = header(rec, "sagaId");
        String replyTo   = header(rec, "replyTo");
        if (replyTo == null || replyTo.isBlank()) replyTo = Topics.ORDER_SAGA_REPLY;

        JsonNode root = om.readTree(rec.value());
        String orderId = root.path("orderId").asText();

        log.info("[RESTAURANT FULFILL] recv key={} eventType={} sagaId={} payload={}",
                orderId, eventType, sagaId, rec.value());

        switch (eventType) {
            case "START_PREPARATION" -> {
                // cập nhật domain: PREPARING (nếu bạn có)
                reply(replyTo, orderId, sagaId, "RESTAURANT_PREPARING", Map.of(
                        "restaurantStatus", "PREPARING",
                        "note", "kitchen started"
                ));

            }
            case "COMPLETE_ORDER" -> {
                // bếp hoàn tất → gửi COMPLETED
                reply(replyTo, orderId, sagaId, "RESTAURANT_COMPLETED", Map.of(
                        "restaurantStatus", "COMPLETED",
                        "completedAt", Instant.now().toString()
                ));
            }
            default -> log.warn("[RESTAURANT FULFILL] Unknown eventType={} orderId={}", eventType, orderId);
        }
    }

    private void reply(String topic, String key, String sagaId, String eventType, Map<String, Object> payload) throws Exception {
        var envelope = Map.of(
                "eventType", eventType,
                "orderId", key,
                "payload", payload,
                "timestamp", Instant.now().toString()
        );
        String json = om.writeValueAsString(envelope);

        ProducerRecord<String, String> out = new ProducerRecord<>(topic, key, json);
        out.headers().add("sagaId", bytes(sagaId == null ? UUID.randomUUID().toString() : sagaId));
        out.headers().add("eventType", bytes(eventType));

        template.send(out).whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("[RESTAURANT->SAGA] SEND FAIL topic={} key={} eventType={} err={}",
                        topic, key, eventType, ex.toString(), ex);
            } else {
                log.info("[RESTAURANT->SAGA] SENT OK topic={} key={} eventType={}", topic, key, eventType);
            }
        });
    }

    private static byte[] bytes(String s) { return s == null ? new byte[0] : s.getBytes(StandardCharsets.UTF_8); }
    private static String header(ConsumerRecord<?,?> rec, String key) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}

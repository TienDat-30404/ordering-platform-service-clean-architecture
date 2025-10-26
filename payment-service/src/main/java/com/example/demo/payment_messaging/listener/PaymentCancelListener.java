package com.example.demo.payment_messaging.listener;

import com.example.common_dtos.enums.Topics;
import com.example.demo.payment_messaging.publisher.PaymentReplyPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentCancelListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCancelListener.class);

    private final PaymentReplyPublisher publisher;
    private final ObjectMapper om = new ObjectMapper();

    public PaymentCancelListener(PaymentReplyPublisher publisher) {
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.PAYMENT_CANCEL_COMMAND, groupId = "payment-service-group")
    public void onCancel(ConsumerRecord<String, String> rec) throws Exception {
        String sagaId = header(rec, "sagaId");
        String replyTo = header(rec, "replyTo");
        if (replyTo == null || replyTo.isBlank()) replyTo = Topics.ORDER_SAGA_REPLY;

        JsonNode root = om.readTree(rec.value());
        String orderId = root.path("orderId").asText();

        log.info("[PAYMENT] CANCEL_AUTHORIZATION orderId={} sagaId={} raw={}", orderId, sagaId, rec.value());

        // Demo: coi như hoàn/huỷ luôn
        String eventType = "PAYMENT_REFUNDED"; // hoặc "PAYMENT_CANCELED"
        var envelope = Map.of(
                "eventType", eventType,
                "orderId", orderId,
                "payload", Map.of("refundedAt", Instant.now().toString()),
                "timestamp", Instant.now().toString()
        );

        publisher.publish(
                replyTo,
                orderId,
                om.writeValueAsString(envelope),
                Map.of(
                        "sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId,
                        "correlationId", UUID.randomUUID().toString(),
                        "eventType", eventType
                )
        );
        log.info("[PAYMENT] Sent {} for orderId={} to {}", eventType, orderId, replyTo);
    }

    private static String header(ConsumerRecord<?,?> rec, String key) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}

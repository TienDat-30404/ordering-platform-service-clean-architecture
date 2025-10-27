package com.example.demo.restaurants_messaging.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestaurantEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    /** Overload tiện: không cần headers */
    public void publish(String topic, String key, String payload) {
        publish(topic, key, payload, Map.of());
    }

    /**
     * Phát event sang topic cụ thể (ví dụ: restaurant.validate.command)
     * @param topic   topic Kafka cần gửi
     * @param key     orderId hoặc correlationId
     * @param payload JSON payload (String)
     * @param headers Map chứa sagaId, replyTo, eventType, ...
     */
    public void publish(String topic, String key, String payload, Map<String, String> headers) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);

            if (headers != null && !headers.isEmpty()) {
                headers.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        record.headers().add(k, v.getBytes(StandardCharsets.UTF_8));
                    }
                });
            }

            kafkaTemplate.send(record).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("[KafkaPublisher] SEND FAILED topic={} key={} headers={} at={} err={}",
                            topic, key, headers, Instant.now(), ex.toString(), ex);
                } else {
                    var md = result.getRecordMetadata();
                    log.info("[KafkaPublisher] SENT OK topic={} partition={} offset={} key={} headers={} at={}",
                            md.topic(), md.partition(), md.offset(), key, headers, Instant.now());
                }
            });
        } catch (Exception e) {
            log.error("[KafkaPublisher] Failed to publish message: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka publish failed", e);
        }
    }

    /** (Tùy chọn) Gửi và đợi tối đa timeout để biết chắc đã gửi */
    public void publishAndWait(String topic, String key, String payload, Map<String, String> headers, long timeoutMs) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
            if (headers != null && !headers.isEmpty()) {
                headers.forEach((k, v) -> {
                    if (k != null && !k.isBlank() && v != null) {
                        record.headers().add(k, v.getBytes(StandardCharsets.UTF_8));
                    }
                });
            }
            var future = kafkaTemplate.send(record);
            var result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            var md = result.getRecordMetadata();
            log.info("[KafkaPublisher] SENT OK (wait) topic={} partition={} offset={} key={} headers={} at={}",
                    md.topic(), md.partition(), md.offset(), key, headers, Instant.now());
        } catch (Exception e) {
            log.error("[KafkaPublisher] SEND FAILED (wait) topic={} key={} headers={} err={}",
                    topic, key, headers, e.toString(), e);
            throw new RuntimeException("Kafka publish failed (wait)", e);
        }
    }
}

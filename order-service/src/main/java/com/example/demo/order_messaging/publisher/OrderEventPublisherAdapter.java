package com.example.demo.order_messaging.publisher;

import com.example.demo.application.ports.output.OrderPublisher.OrderEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

// ...
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisherAdapter implements OrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meter; // <-- inject Micrometer

    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_MS = 200;
    private static final double BACKOFF_MULTIPLIER = 1.8;
    private static final long SEND_TIMEOUT_MS = 5_000;
    private static final long MAX_DELAY_MS = 10_000;

    @Override
    public void publish(String topic, String key, String jsonPayload, Map<String, String> headers) {
        long delay = INITIAL_DELAY_MS;
        Throwable last = null;
        int attempt = 0;

        // metrics holders (tag theo topic & eventType nếu có)
        String eventType = headers != null ? headers.getOrDefault("eventType","UNKNOWN") : "UNKNOWN";
        Counter pubSuccess = Counter.builder("order_publish_success_total")
                .tag("topic", topic).tag("eventType", eventType).register(meter);
        Counter pubFailure = Counter.builder("order_publish_failure_total")
                .tag("topic", topic).tag("eventType", eventType).register(meter);
        DistributionSummary attemptsSummary = DistributionSummary.builder("order_publish_attempts")
                .tag("topic", topic).tag("eventType", eventType).register(meter);

        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try {
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, jsonPayload);
                if (headers != null) {
                    headers.forEach((k, v) -> {
                        if (v != null) record.headers().add(k, v.getBytes(StandardCharsets.UTF_8));
                    });
                }
                var result = kafkaTemplate.send(record).get(SEND_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
                var md = result.getRecordMetadata();
                log.info("[KafkaPublisher] SENT OK topic={} partition={} offset={} key={} (attempt {}/{})",
                        md.topic(), md.partition(), md.offset(), key, attempt, MAX_ATTEMPTS);

                pubSuccess.increment();
                attemptsSummary.record(attempt);
                return;
            } catch (Throwable ex) {
                last = ex;
                log.warn("[KafkaPublisher] SEND FAILED attempt {}/{} topic={} key={} err={}",
                        attempt, MAX_ATTEMPTS, topic, key, ex.toString());

                if (attempt == MAX_ATTEMPTS) break;
                try { Thread.sleep(delay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                delay = Math.min((long)(delay * BACKOFF_MULTIPLIER), MAX_DELAY_MS);
            }
        }

        pubFailure.increment();
        attemptsSummary.record(attempt);
        log.error("[KafkaPublisher] GIVE UP after {} attempts topic={} key={} lastErr={}",
                MAX_ATTEMPTS, topic, key, last != null ? last.toString() : "n/a", last);
        throw new RuntimeException("Kafka publish failed after retries", last);
    }
}

package com.example.demo.order_messaging.listener;

import com.example.demo.application.orchestrator.OrderOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaReplyListener {

    private final OrderOrchestratorService orchestrator;

    @KafkaListener(topics = "order.saga.reply", groupId = "order-service-group")
    public void onReply(ConsumerRecord<String, String> rec) {
        String eventType = header(rec, "eventType");
        String sagaId    = header(rec, "sagaId");

        log.info("[ORDER<-SAGA] key={} eventType={} sagaId={} payload={}",
                rec.key(), eventType, sagaId, rec.value());

        try {
            orchestrator.onReply(rec);
        } catch (Exception e) {
            log.error("[ORDER<-SAGA] handle failed key={} eventType={} err={}",
                    rec.key(), eventType, e.toString(), e);
        }
    }

    private String header(ConsumerRecord<?, ?> rec, String key) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }
}

package com.example.demo.order_messaging.listener;

import com.example.demo.application.orchestrator.OrderOrchestratorService;
import com.example.common_dtos.enums.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe các phản hồi từ Restaurant/Payment gửi về Orchestrator.
 * Ủy quyền xử lý cho OrderOrchestratorService.onReply(...)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderSagaReplyListener {

    private final OrderOrchestratorService orchestrator;

    @KafkaListener(topics = Topics.ORDER_SAGA_REPLY, groupId = "order-service-group")
    public void onReply(ConsumerRecord<String, String> record) throws Exception {
        // Log tối thiểu để trace
        log.debug("[ORDER<-SAGA_REPLY] key={} headers={} value={}",
                record.key(), record.headers(), record.value());

        // Ủy quyền cho orchestrator (đã có switch-case theo header eventType)
        orchestrator.onReply(record);
    }
}

package com.example.demo.infrastructure.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.common_dtos.dto.PaymentResponseData;


@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestratorResponsePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic RIÊNG TƯ cho Orchestrator lắng nghe
    private static final String ORCHESTRATOR_PAYMENT_RESPONSE_TOPIC = "orchestrator.payment.response";

    public void publishResponse(PaymentResponseData response) {
        log.info("[ORCH PUB] Publishing Payment response for order {} to Orchestrator. Status: {}",
                response.orderId(), response.status());

        try {
            kafkaTemplate.send(
                    ORCHESTRATOR_PAYMENT_RESPONSE_TOPIC,
                    response.orderId().toString(),
                    response); // Không cần xử lý bất đồng bộ phức tạp ở đây
        } catch (Exception e) {
            log.error("[ORCH PUB] Error sending PaymentResponse for order: {}", response.orderId(), e);
            // Trong môi trường sản xuất, lỗi này cần được xử lý nghiêm túc (Alert, DLQ)
        }
    }
}
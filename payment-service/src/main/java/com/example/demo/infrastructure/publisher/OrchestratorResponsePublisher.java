package com.example.demo.infrastructure.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import com.example.common_dtos.dto.PaymentResponseData;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrchestratorResponsePublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic RIÊNG TƯ cho Orchestrator lắng nghe
    private static final String ORCHESTRATOR_PAYMENT_RESPONSE_TOPIC = "orchestrator.payment.response";

    // public void publishResponse(PaymentResponseData response, Map<String, Object>
    // receivedHeaders) {
    // System.out.println("wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww" + receivedHeaders);
    // log.info("[ORCH PUB] Publishing Payment response for order {} to
    // Orchestrator. Status: {}",
    // response.orderId(), response.status());

    // try {
    // kafkaTemplate.send(
    // ORCHESTRATOR_PAYMENT_RESPONSE_TOPIC,
    // response.orderId().toString(),
    // response); // Không cần xử lý bất đồng bộ phức tạp ở đây
    // } catch (Exception e) {
    // log.error("[ORCH PUB] Error sending PaymentResponse for order: {}",
    // response.orderId(), e);
    // // Trong môi trường sản xuất, lỗi này cần được xử lý nghiêm túc (Alert, DLQ)
    // }
    // }

    public void publishResponse(PaymentResponseData response, Map<String, Object> receivedHeaders) {
        log.info("[ORCH PUB] Publishing Payment response for order {} to Orchestrator. Status: {}",
                response.orderId(), response.status());

        System.out.println("rêcvierHearrrrrrrrrrrrrrrrrrrrrrrrrrrr" + receivedHeaders);
        Map<String, Object> headersToSend = receivedHeaders.entrySet().stream()
                // Loại bỏ các Header Kafka nội bộ không cần thiết
                .filter(e -> !e.getKey().startsWith("kafka_") && e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue() instanceof byte[] ? new String((byte[]) e.getValue()) : e.getValue()));

        String replyToTopic = (String) headersToSend.getOrDefault("replyTo", ORCHESTRATOR_PAYMENT_RESPONSE_TOPIC);
        System.out.println("replyToTOpiccccccccccccccccccccccccc" + replyToTopic);
        headersToSend.put("eventType", response.status());
        headersToSend.put("source", "payment-service");
        System.out.println("kafkaHeadersTopiccccccccccccccccccccccccc" + KafkaHeaders.TOPIC);
        // --- 2. TẠO VÀ GỬI MESSAGE VỚI HEADERS ---
        try {
            Message<PaymentResponseData> message = MessageBuilder.withPayload(response)
                    .copyHeaders(headersToSend) // Gắn tất cả headers đã cập nhật
                    .setHeader(KafkaHeaders.TOPIC, replyToTopic) // Đặt Topic phản hồi
                    .setHeader(KafkaHeaders.KEY, response.orderId().toString()) // Dùng orderId làm Key
                    .build();
            System.out.println("messsssssssssss2222222ssssage" + message);
            kafkaTemplate.send(message);

        } catch (Exception e) {
            log.error("[ORCH PUB] Error sending PaymentResponse for order: {}", response.orderId(), e);
        }
    }

}

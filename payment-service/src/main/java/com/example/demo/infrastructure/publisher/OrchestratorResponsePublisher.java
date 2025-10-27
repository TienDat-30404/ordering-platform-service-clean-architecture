package com.example.demo.infrastructure.publisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

        // 1. TẠO MAP CHỈ CHỨA CÁC HEADER SAGA CẦN THIẾT
        Map<String, String> sagaHeaders = new HashMap<>();

        // Lấy Topic phản hồi (ReplyTo)
        String replyToTopic = ORCHESTRATOR_PAYMENT_RESPONSE_TOPIC; // Mặc định

        // Khôi phục các Saga Headers cốt lõi từ Headers đã nhận (Chuyển byte[] thành
        // String)
        for (Map.Entry<String, Object> entry : receivedHeaders.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Chỉ quan tâm đến các Headers được Service khởi tạo (Orchestrator) gửi đến
            if (key.equals("sagaId") || key.equals("correlationId") || key.equals("replyTo")) {
                if (value instanceof byte[]) {
                    String strValue = new String((byte[]) value, StandardCharsets.UTF_8);
                    sagaHeaders.put(key, strValue);
                    if (key.equals("replyTo")) {
                        replyToTopic = strValue;
                    }
                } else if (value instanceof String) {
                    sagaHeaders.put(key, (String) value);
                    if (key.equals("replyTo")) {
                        replyToTopic = (String) value;
                    }
                }
            }
        }

        // 2. CẬP NHẬT CÁC HEADER PHẢN HỒI
        // Thêm trạng thái Saga mới (quan trọng nhất)
        sagaHeaders.put("eventType", response.status());
        // Thêm nguồn gốc của phản hồi
        sagaHeaders.put("source", "payment-service");

        // --- 3. GỬI MESSAGE ---
        try {
            Message<PaymentResponseData> message = MessageBuilder.withPayload(response)
                    .copyHeaders(sagaHeaders) // Gắn chỉ các headers Saga đã chuẩn hóa
                    .setHeader(KafkaHeaders.TOPIC, replyToTopic) // Đặt Topic phản hồi
                    .setHeader(KafkaHeaders.KEY, response.orderId().toString()) // Dùng orderId làm Key
                    .build();
            System.out.println("messssssssssssssssssssage"+ message);
            kafkaTemplate.send(message);
            log.info("[ORCH PUB] Response sent to topic: {} with SagaId: {}", response.status(), sagaHeaders.get("sagaId"));

        } catch (Exception e) {
            log.error("[ORCH PUB] Error sending PaymentResponse for order: {}", response.orderId(), e);
        }
    }

}

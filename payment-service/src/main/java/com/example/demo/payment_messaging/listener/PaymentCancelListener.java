package com.example.demo.payment_messaging.listener;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.common_dtos.enums.Topics;
import com.example.demo.application.dto.command.RefundPaymentCommand;
import com.example.demo.application.usecases.PaymentApplicationService;
import com.example.demo.payment_messaging.publisher.PaymentReplyPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentCancelListener {

    private final PaymentApplicationService paymentApplicationService; // <-- use case mới
    private final PaymentReplyPublisher publisher;
    private final ObjectMapper om = new ObjectMapper();

    public PaymentCancelListener(PaymentApplicationService paymentApplicationService,
                                 PaymentReplyPublisher publisher) {
        this.paymentApplicationService = paymentApplicationService;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.PAYMENT_CANCEL_COMMAND, groupId = "payment-service-group")
    public void onCancel(ConsumerRecord<String, String> rec) throws Exception {
        String sagaId  = header(rec, "sagaId");
        String replyTo = header(rec, "replyTo");
        if (replyTo == null || replyTo.isBlank()) replyTo = Topics.ORDER_SAGA_REPLY;

        String hdrPaymentId = header(rec, "paymentId");
        String hdrUserId    = header(rec, "userId");

        JsonNode root   = om.readTree(rec.value());
        String orderIdStr = root.path("orderId").asText();
        String reason   = root.path("payload").path("reason").asText("compensation");
        String amountStr= root.path("payload").path("amount").asText("0");

        Long orderId   = safeLong(orderIdStr);
        Long paymentId = safeLong(hdrPaymentId);
        BigDecimal amount = new BigDecimal(amountStr);

        // Gọi use case refund
        PaymentResponseData resp = paymentApplicationService.refundPayment(
                new RefundPaymentCommand(orderId, paymentId, amount, reason)
        );

        String eventType = "PAYMENT_REFUNDED"; // theo flow bồi hoàn
        var envelope = Map.of(
                "eventType", eventType,
                "orderId", String.valueOf(resp.orderId()),
                "payload", Map.of("refundedAt", Instant.now().toString()),
                "timestamp", Instant.now().toString()
        );

        publisher.publish(
                replyTo,
                String.valueOf(resp.orderId()),
                om.writeValueAsString(envelope),
                Map.of(
                        "sagaId", sagaId == null ? UUID.randomUUID().toString() : sagaId,
                        "correlationId", UUID.randomUUID().toString(),
                        "eventType", eventType
                )
        );
    }

    private static String header(ConsumerRecord<?,?> rec, String key) {
        Header h = rec.headers().lastHeader(key);
        return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
    }

    private static Long safeLong(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.valueOf(s); } catch (Exception e) { return null; }
    }
}

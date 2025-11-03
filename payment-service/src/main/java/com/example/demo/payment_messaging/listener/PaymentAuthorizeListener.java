package com.example.demo.payment_messaging.listener;

import com.example.common_dtos.dto.PaymentResponseData;
import com.example.common_dtos.enums.Topics;
import com.example.demo.application.dto.command.AuthorizePaymentCommand;
import com.example.demo.application.usecases.PaymentApplicationService;
import com.example.demo.payment_messaging.publisher.PaymentReplyPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentAuthorizeListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentAuthorizeListener.class);

    private final PaymentApplicationService paymentApplicationService; // <-- dùng use case mới
    private final PaymentReplyPublisher publisher;
    private final ObjectMapper om = new ObjectMapper();

    public PaymentAuthorizeListener(PaymentApplicationService paymentApplicationService,
                                    PaymentReplyPublisher publisher) {
        this.paymentApplicationService = paymentApplicationService;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.PAYMENT_AUTHORIZE_COMMAND, groupId = "payment-service-group")
    public void onAuthorize(ConsumerRecord<String, String> rec) throws Exception {
        String sagaId  = header(rec, "sagaId");
        log.info("22222222222222222222222222222222222", "22trs");
        String replyTo = header(rec, "replyTo");
        if (replyTo == null || replyTo.isBlank()) replyTo = Topics.ORDER_SAGA_REPLY;

        // Header optional
        String hdrPaymentId = header(rec, "paymentId");
        String hdrUserId    = header(rec, "userId");

        JsonNode root = om.readTree(rec.value());
        String orderIdStr = root.path("orderId").asText();
        String amountStr  = root.path("payload").path("amount").asText("0");

        Long orderId  = safeLong(orderIdStr);
        Long userId   = safeLong(hdrUserId);         // có thể null
        Long paymentId= safeLong(hdrPaymentId);      // có thể null
        BigDecimal amount = new BigDecimal(amountStr);

        log.info("[PAYMENT] AUTHORIZE orderId={}, amount={}, sagaId={}, paymentId={}, userId={}, raw={}",
                orderId, amount, sagaId, paymentId, userId, rec.value());

        // Gọi use case mới
        PaymentResponseData resp = paymentApplicationService.authorizePayment(
                new AuthorizePaymentCommand(orderId, userId, amount, paymentId)
        );

        String eventType = switch (resp.status()) {
            case "AUTHORIZED" -> "PAYMENT_AUTHORIZED";
            case "FAILED"     -> "PAYMENT_FAILED";
            default           -> "PAYMENT_FAILED";
        };

        Map<String, Object> payload = switch (eventType) {
            case "PAYMENT_AUTHORIZED" -> Map.of(
                    "orderId", String.valueOf(resp.orderId()),
                    "transactionId", resp.transactionId(),
                    "approvedAt", Instant.now().toString()
            );
            default -> Map.of(
                    "orderId", String.valueOf(resp.orderId()),
                    "reason", resp.reason()
            );
        };

        var envelope = Map.of(
                "eventType", eventType,
                "orderId", String.valueOf(resp.orderId()),
                "payload", payload,
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

        log.info("[PAYMENT] Sent {} for orderId={} to {}", eventType, resp.orderId(), replyTo);
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

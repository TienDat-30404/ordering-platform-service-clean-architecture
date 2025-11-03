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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentCancelListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentCancelListener.class);

    private final PaymentApplicationService paymentApplicationService;
    private final PaymentReplyPublisher publisher;
    private final MeterRegistry meter;
    private final ObjectMapper om = new ObjectMapper();

    public PaymentCancelListener(PaymentApplicationService paymentApplicationService,
                                 PaymentReplyPublisher publisher,
                                 MeterRegistry meter) {
        this.paymentApplicationService = paymentApplicationService;
        this.publisher = publisher;
        this.meter = meter;
    }

    @KafkaListener(topics = Topics.PAYMENT_CANCEL_COMMAND, groupId = "payment-service-group")
    public void onCancel(ConsumerRecord<String, String> rec) throws Exception {
        // --- Metrics: consumed ---
        Counter.builder("payment_cancel_messages_consumed_total")
                .tag("topic", Topics.PAYMENT_CANCEL_COMMAND)
                .register(meter)
                .increment();

        try {
            String sagaId  = header(rec, "sagaId");
            String replyTo = header(rec, "replyTo");
            if (replyTo == null || replyTo.isBlank()) replyTo = Topics.ORDER_SAGA_REPLY;

            String hdrPaymentId = header(rec, "paymentId");
            String hdrUserId    = header(rec, "userId"); // hiện chưa dùng, vẫn đọc để có thể gắn tag nếu cần

            JsonNode root     = om.readTree(rec.value());
            String orderIdStr = root.path("orderId").asText();
            String reason     = root.path("payload").path("reason").asText("compensation");
            String amountStr  = root.path("payload").path("amount").asText("0");

            Long orderId    = safeLong(orderIdStr);
            Long paymentId  = safeLong(hdrPaymentId);
            BigDecimal amount = new BigDecimal(amountStr);

            // --- Metrics: refund amount distribution ---
            DistributionSummary.builder("payment_refund_amount")
                    .baseUnit("VND") // đổi theo currency của bạn nếu khác
                    .register(meter)
                    .record(amount.doubleValue());

            log.info("[PAYMENT] CANCEL/REFUND orderId={}, paymentId={}, amount={}, reason={}, sagaId={}, raw={}",
                    orderId, paymentId, amount, reason, sagaId, rec.value());

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

            // --- Metrics: reply status ---
            Counter.builder("payment_cancel_reply_total")
                    .tag("status", resp.status()) // thường là REFUND_COMPLETED
                    .register(meter)
                    .increment();

            log.info("[PAYMENT] Sent {} for orderId={} to {}", eventType, resp.orderId(), replyTo);
        } catch (Exception e) {
            // --- Metrics: error path ---
            Counter.builder("payment_cancel_errors_total")
                    .register(meter)
                    .increment();
            throw e; // để error handler Kafka xử lý tiếp
        }
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

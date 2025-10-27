// package com.example.demo.payment_messaging.listener;

// import com.example.common_dtos.enums.Topics;
// import com.example.demo.payment.service.PaymentService;
// import com.example.demo.payment_messaging.publisher.PaymentReplyPublisher;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.apache.kafka.clients.consumer.ConsumerRecord;
// import org.apache.kafka.common.header.Header;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.kafka.annotation.KafkaListener;
// import org.springframework.stereotype.Component;

// // import java.math.BigDecimal;
// // import java.nio.charset.StandardCharsets;
// // import java.time.Instant;
// // import java.util.Map;
// // import java.util.UUID;

// // @Component
// // @RequiredArgsConstructor
// // @Slf4j
// // public class PaymentAuthorizeListener {

// private static final Logger log=LoggerFactory.getLogger(PaymentAuthorizeListener.class);

// private final PaymentService paymentService;private final PaymentReplyPublisher publisher;private final ObjectMapper om=new ObjectMapper();

// public PaymentAuthorizeListener(PaymentService paymentService,PaymentReplyPublisher publisher){this.paymentService=paymentService;this.publisher=publisher;}

// @KafkaListener(topics=Topics.PAYMENT_AUTHORIZE_COMMAND,groupId="payment-service-group")public void onAuthorize(ConsumerRecord<String,String>rec)throws Exception{String sagaId=header(rec,"sagaId");String replyTo=header(rec,"replyTo");if(replyTo==null||replyTo.isBlank())replyTo=Topics.ORDER_SAGA_REPLY;

// JsonNode root = om.readTree(rec.value());
// String orderId = root.path("orderId").asText();
// // payload.amount có thể là string/number
// String amountStr = root.path("payload").path("amount").asText("0");
// BigDecimal amount = new BigDecimal(amountStr);

// log.info("[PAYMENT] AUTHORIZE orderId={}, amount={}, sagaId={}", orderId,
// amount, sagaId);

// var result = paymentService.authorize(orderId, amount);

// String eventType;
// Map<String, Object> payload;
// if (result.approved()) {
// eventType = "PAYMENT_AUTHORIZED";
// payload = Map.of(
// "orderId", orderId,
// "transactionId", result.txId(),
// "approvedAt", Instant.now().toString()
// );
// } else {
// eventType = "PAYMENT_FAILED";
// payload = Map.of(
// "orderId", orderId,
// "reason", result.reason()
// );
// }

// var envelope=Map.of("eventType",eventType,"orderId",orderId,"payload",payload,"timestamp",Instant.now().toString());

// publisher.publish(replyTo,orderId,om.writeValueAsString(envelope),Map.of("sagaId",sagaId==null?UUID.randomUUID().toString():sagaId,"correlationId",UUID.randomUUID().toString(),"eventType",eventType));

// log.info("[PAYMENT] Sent {} for orderId={} to {}", eventType, orderId,
// replyTo);
// }

// private String header(ConsumerRecord<?,?> rec, String key) {
// Header h = rec.headers().lastHeader(key);
// return h == null ? null : new String(h.value(), StandardCharsets.UTF_8);
// }

// private String toJson(Object o) {
// try { return om.writeValueAsString(o); } catch (Exception e) { throw new
// RuntimeException(e); }
// }
// }

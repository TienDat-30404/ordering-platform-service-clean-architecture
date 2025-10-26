// package com.example.demo.payment_messaging.publisher;

// import lombok.RequiredArgsConstructor;
// import org.apache.kafka.clients.producer.ProducerRecord;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.stereotype.Component;

// import java.nio.charset.StandardCharsets;
// import java.util.Map;

// @Component
// @RequiredArgsConstructor
// public class KafkaPaymentReplyPublisher implements PaymentReplyPublisher {

//     private final KafkaTemplate<String, String> kafkaTemplate;

//     @Override
//     public void publish(String topic, String key, String payload, Map<String, String> headers) {
//         ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);
//         if (headers != null) {
//             headers.forEach((k, v) -> {
//                 if (v != null) record.headers().add(k, v.getBytes(StandardCharsets.UTF_8));
//             });
//         }
//         kafkaTemplate.send(record);
//     }
// }
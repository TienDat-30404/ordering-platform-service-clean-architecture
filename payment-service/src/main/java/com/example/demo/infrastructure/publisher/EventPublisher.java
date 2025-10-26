//package com.example.demo.infrastructure.publisher;
//
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Component;
//
//import com.example.demo.config.KafkaConfig;
//import com.example.demo.domain.event.PaymentAuthorizedEvent;
//import com.example.demo.domain.event.PaymentFailedEvent;
//import com.example.demo.domain.event.PaymentResponseData;
//import com.example.demo.domain.event.RefundCompletedEvent;
//
//
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class EventPublisher {
//
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    public void publishPaymentAuthorized(PaymentAuthorizedEvent event) {
//        log.info("Publishing PaymentAuthorized event for order: {}", event.getOrderId());
//        kafkaTemplate.send(KafkaConfig.PAYMENT_AUTHORIZED_TOPIC, event.getOrderId().toString(), event);
//    }
//
//    public void publishPaymentFailed(PaymentFailedEvent event) {
//        log.info("Publishing PaymentFailed event for order: {}", event.getOrderId());
//        kafkaTemplate.send(KafkaConfig.PAYMENT_FAILED_TOPIC, event.getOrderId().toString(), event);
//    }
//
//    public void publishRefundCompleted(RefundCompletedEvent event) {
//        log.info("Publishing RefundCompleted event for order: {}", event.getOrderId());
//        kafkaTemplate.send(KafkaConfig.REFUND_COMPLETED_TOPIC, event.getOrderId().toString(), event);
//    }
//}
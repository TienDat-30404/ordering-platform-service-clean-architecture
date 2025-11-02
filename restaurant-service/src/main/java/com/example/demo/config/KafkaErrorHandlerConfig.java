package com.example.demo.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
        // 1) DLQ recoverer: gửi sang <topic>.DLT cùng partition hiện tại
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                // mapping mặc định: ghi vào <topic>.DLT cùng partition
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );

        // 2) Backoff: retry 3 lần, mỗi lần cách nhau 1s (tuỳ bạn chỉnh)
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // 3) (Tùy chọn) đánh dấu 1 số lỗi là non-retryable -> đi thẳng DLQ
        // handler.addNotRetryableExceptions(IllegalArgumentException.class);

        return handler;
    }
}
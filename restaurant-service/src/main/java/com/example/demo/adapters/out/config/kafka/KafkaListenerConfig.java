package com.example.demo.adapters.out.config.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Listener config chuẩn cho Kafka 3.x:
 * - MANUAL ack (chủ động commit)
 * - Retry ngắn + DLT: <topic>.DLT
 * - Dùng DefaultErrorHandler (thay thế SeekToCurrentErrorHandler cũ)
 */
@Configuration
public class KafkaListenerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            KafkaTemplate<String, String> kafkaTemplate
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(consumerFactory);

        // MANUAL ack để chủ động commit sau khi xử lý xong
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // DLT: gửi bản ghi lỗi sang <topic>.DLT cùng partition
        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );

        // Retry 3 lần, mỗi lần cách nhau 1s rồi mới đẩy DLT
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        factory.setCommonErrorHandler(errorHandler);

        // (Tuỳ chọn) tăng concurrency nếu cần:
        // factory.setConcurrency(Runtime.getRuntime().availableProcessors());

        // (Tuỳ chọn) filter bỏ message rỗng:
        // factory.setRecordFilterStrategy(rec -> rec.value() == null || rec.value().isBlank());

        return factory;
    }
}

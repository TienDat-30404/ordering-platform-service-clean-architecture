package com.example.demo;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
// Kích hoạt profile 'test' để load application-test.properties
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = BaseIntegrationTest.Initializer.class)
public abstract class BaseIntegrationTest {

    // 1. Khởi động Kafka Container
    @Container
    public static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    // 2. Khởi động PostgreSQL Container
    @Container
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine")
    );

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            kafka.start();
            postgres.start();

            // Ghi đè các thuộc tính cấu hình của Spring
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    applicationContext,
                    // Cấu hình Kafka
                    "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers(),

                    // Cấu hình Datasource
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),

                    // JPA: Tự động tạo schema trong DB test
                    "spring.jpa.hibernate.ddl-auto=create"
            );
        }
    }
}
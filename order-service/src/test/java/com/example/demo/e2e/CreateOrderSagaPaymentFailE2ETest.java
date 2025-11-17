package com.example.demo.e2e;

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;
import com.example.demo.adapters.out.persistence.repository.OrderJpaRepository;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.orchestrator.OrderOrchestratorService;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeader;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import testsupport.KafkaJson;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(CreateOrderSagaPaymentFailE2ETest.StubPortsConfig.class)
class CreateOrderSagaPaymentFailE2ETest {

    @Autowired OrderOrchestratorService orchestrator;
    @Autowired KafkaTemplate<String,String> kafkaTemplate;
    @Autowired OrderJpaRepository orderRepo;
    @Autowired JdbcTemplate jdbc;

    private static final Long RESTAURANT_ID = 1L; // restaurant không quan trọng ở nhánh fail payment
    private static final String BOOTSTRAP = "localhost:29092";

    private static final List<String> TOPICS = List.of(
            "payment.authorize.command",
            "order.saga.reply"
    );

    // Random group-id cho @KafkaListener của app để không dính “dư âm”
    @DynamicPropertySource
    static void kafkaListenerProps(DynamicPropertyRegistry r) {
        String gid = "order-svc-e2e-" + UUID.randomUUID();
        r.add("spring.kafka.consumer.group-id", () -> gid);
        r.add("spring.kafka.listener.client-id", () -> gid);
    }

    @BeforeEach
    void purgeTopics() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", BOOTSTRAP))) {
            Map<String, TopicDescription> desc = admin.describeTopics(TOPICS).allTopicNames().get();
            List<TopicPartition> tps = desc.values().stream()
                    .flatMap(td -> td.partitions().stream().map(p -> new TopicPartition(td.name(), p.partition())))
                    .collect(Collectors.toList());

            var ends = admin.listOffsets(
                    tps.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()))
            ).all().get();

            Map<TopicPartition, RecordsToDelete> del = new HashMap<>();
            for (var e : ends.entrySet()) {
                del.put(e.getKey(), RecordsToDelete.beforeOffset(e.getValue().offset()));
            }
            admin.deleteRecords(del).all().get();
        }
    }

    @Test
    void paymentFailed_shouldCancelImmediately_andTrackAllStatuses() throws Exception {
        long orderId = seedOrderRow(RESTAURANT_ID);

        // item bất kỳ; nhánh này fail ở payment nên restaurant không dùng đến
        var it = new CreateOrderItemCommand();
        it.setProductId(100L);
        it.setQuantity(1);

        orchestrator.startCreateOrderSagaFromCommand(
                String.valueOf(orderId), String.valueOf(RESTAURANT_ID), List.of(it)
        );

        // Lấy đúng sagaId đang active (tránh mismatch header)
        String sagaId = awaitSagaIdFromService(String.valueOf(orderId), 10);
        assertThat(sagaId).as("sagaId must be captured from orchestrator.activeSagaByOrder").isNotBlank();

        // --- Gửi reply PAYMENT_FAILED (đúng header sagaId + key = orderId) ---
        sendReply("PAYMENT_FAILED", sagaId, String.valueOf(orderId), Map.of(
                "reason", "CARD_DECLINED",
                "code",   "DECLINED"
        ));

        // --- Track toàn bộ status biến thiên cho tới khi terminal (vòng lặp thủ công, không Awaitility) ---
        List<String> timeline = trackStatusesUntilTerminal(orderId, 45 /*sec*/, 30 /*ms*/);

        // In timeline cho dễ đọc
        System.out.println("===== ORDER STATUS TIMELINE (PAYMENT_FAIL no-miss) =====");
        timeline.forEach(s -> System.out.println(" - " + s));
        System.out.println("========================================================");

        // Kỳ vọng subsequence: PENDING -> CANCELLED (fail payment huỷ ngay)
        assertThat(timeline)
                .as("[Status subsequence must be PENDING -> CANCELLED]")
                .containsSubsequence("PENDING", "CANCELLED");

        // Không được ghé qua happy-path states
        assertThat(timeline)
                .as("Must not touch happy-path states when payment fails")
                .doesNotContain("PAID", "APPROVED", "PREPARING", "COMPLETED");

        // Trạng thái cuối cùng phải là CANCELLED
        assertThat(timeline.get(timeline.size()-1)).isEqualTo("CANCELLED");
    }

    // ================= helpers =================

    private long seedOrderRow(Long restaurantId){
        var e = new OrderJpaEntity();
        e.setUserId(1L);
        e.setRestaurantId(restaurantId);
        e.setStatus("PENDING");
        e.setAmount(BigDecimal.ZERO);
        e.setFinalPrice(BigDecimal.ZERO);
        e.setDiscountAmount(BigDecimal.ZERO);
        e.setHasBeenRated(false);
        e.setCreatedAt(Instant.now());
        orderRepo.saveAndFlush(e);
        return e.getId();
    }

    private void sendReply(String eventType, String sagaId, String orderId, Map<String,Object> payload){
        var env = Map.of(
                "eventType", eventType,
                "orderId", orderId,
                "payload", payload,
                "timestamp", Instant.now().toString()
        );
        ProducerRecord<String,String> rec =
                new ProducerRecord<>("order.saga.reply", orderId, KafkaJson.toJson(env));
        rec.headers().add(new RecordHeader("sagaId", sagaId.getBytes(StandardCharsets.UTF_8)));
        rec.headers().add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(rec);
    }

    /** Lấy sagaId đang active từ orchestrator (reflection) */
    @SuppressWarnings("unchecked")
    private String awaitSagaIdFromService(String orderId, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds*1000L;
        Field f = OrderOrchestratorService.class.getDeclaredField("activeSagaByOrder");
        f.setAccessible(true);
        while (System.currentTimeMillis() < end) {
            var map = (Map<String,String>) f.get(orchestrator);
            String v = map.get(orderId);
            if (v != null && !v.isBlank()) return v;
            Thread.sleep(50);
        }
        return null;
    }

    /**
     * Poll DB theo chu kỳ intervalMs, ghi nhận mọi thay đổi status (không bỏ lỡ)
     * cho đến khi gặp terminal (CANCELLED/COMPLETED) hoặc hết timeoutSec.
     */
    private List<String> trackStatusesUntilTerminal(long orderId, int timeoutSec, int intervalMs) {
        final Set<String> TERMINALS = Set.of("CANCELLED", "COMPLETED");
        final long deadline = System.currentTimeMillis() + timeoutSec * 1000L;
        final List<String> timeline = new ArrayList<>();

        String last = null;
        while (System.currentTimeMillis() < deadline) {
            String now = orderRepo.findById(orderId)
                    .map(OrderJpaEntity::getStatus)
                    .orElse("UNKNOWN");

            if (!Objects.equals(now, last)) {
                timeline.add(now);
                last = now;
            }
            if (TERMINALS.contains(now)) {
                return timeline;
            }
            try { Thread.sleep(Math.max(5, intervalMs)); } catch (InterruptedException ignored) {}
        }

        // Hết timeout mà chưa terminal → fail rõ ràng
        System.out.println("[TEST][WARN] Timeout waiting for terminal status. Timeline so far:");
        timeline.forEach(s -> System.out.println(" - " + s));
        assertThat(false)
                .as("Did not reach a terminal status within " + timeoutSec + "s; last=" + last)
                .isTrue();
        return timeline; // unreachable, để compiler happy
    }

    // Stub restaurant (giữ context load ổn định; nhánh này không chạm tới restaurant)
    @TestConfiguration
    static class StubPortsConfig {
        @Bean @Primary
        RestaurantDataProviderPort restaurantDataStub(){
            return new RestaurantDataProviderPort(){
                @Override public List<ProductDetailData> getProducts(Long r, List<Long> ids){
                    return ids.stream().map(id -> new ProductDetailData(id,"Item "+id,new BigDecimal("10000"))).toList();
                }
                @Override public Map<Long,ProductDetailData> getProductDetailsByIds(List<Long> ids){
                    Map<Long,ProductDetailData> m = new LinkedHashMap<>();
                    ids.forEach(id -> m.put(id, new ProductDetailData(id,"Item "+id,new BigDecimal("10000"))));
                    return m;
                }
                @Override public List<ItemValidationResponse> validateOrderCreation(Long r, List<Long> ids){ return List.of(); }
            };
        }
    }
}

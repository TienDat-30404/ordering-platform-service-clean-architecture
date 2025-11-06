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
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Import(CreateOrderSagaInsufficientStockE2ETest.StubPortsConfig.class)
class CreateOrderSagaInsufficientStockE2ETest {

    @Autowired OrderOrchestratorService orchestrator;
    @Autowired KafkaTemplate<String,String> kafkaTemplate;
    @Autowired OrderJpaRepository orderRepo;
    @Autowired JdbcTemplate jdbc;

    private Long ORDER_ID;
    private static final Long RESTAURANT_ID = 1L;
    private static final String BOOTSTRAP = "localhost:29092";

    private static final List<String> TOPICS = List.of(
            "payment.authorize.command",
            "restaurant.validate.command",
            "payment.cancel.command",
            "order.saga.reply"
    );

    // Randomize group-id cho toàn bộ @KafkaListener của app khi chạy test
    @DynamicPropertySource
    static void kafkaListenerProps(DynamicPropertyRegistry r) {
        String gid = "order-svc-e2e-" + UUID.randomUUID();
        r.add("spring.kafka.consumer.group-id", () -> gid);
        r.add("spring.kafka.listener.client-id", () -> gid);
    }

    @BeforeEach
    void setup() throws Exception {
        purgeTopics(); // dọn offset để không dính dư âm run trước
    }

    @AfterEach
    void tearDown() {
        // nothing
    }

    @Test
    void invalidStock_shouldRefundAndCancel_andTrackAllStatuses() throws Exception {
        // 0) Seed order (PENDING)
        ORDER_ID = seedOrderRow(RESTAURANT_ID);

        // 1) Bật tracker ngay từ PENDING (để không bỏ lỡ gì)
        StatusTracker tracker = new StatusTracker(ORDER_ID, orderRepo);
        tracker.start();

        // 2) Kick off saga với item cố tình thiếu stock
        var it = new CreateOrderItemCommand();
        it.setProductId(1L);
        it.setQuantity(999);

        orchestrator.startCreateOrderSagaFromCommand(
                String.valueOf(ORDER_ID),
                String.valueOf(RESTAURANT_ID),
                List.of(it)
        );

        // 3) Lấy đúng sagaId từ orchestrator (tránh race Kafka)
        String sagaId = awaitSagaIdFromService(String.valueOf(ORDER_ID), 10);
        assertThat(sagaId)
                .as("[sagaId from orchestrator.activeSagaByOrder]")
                .isNotBlank();

        // 4) Giả lập PAYMENT_AUTHORIZED
        sendReply("PAYMENT_AUTHORIZED", sagaId, ORDER_ID.toString(), Map.of(
                "transactionId", UUID.randomUUID().toString(),
                "approvedAt", Instant.now().toString()
        ));

        // 5) Giả lập validate thất bại (thiếu stock)
        sendReply("RESTAURANT_ITEMS_INVALID", sagaId, ORDER_ID.toString(), Map.of(
                "errors", List.of(Map.of("productId", 999, "reason", "NOT_ENOUGH_STOCK"))
        ));

        // 6) Giả lập refund xong
        sendReply("PAYMENT_REFUNDED", sagaId, ORDER_ID.toString(), Map.of(
                "refundedAt", Instant.now().toString()
        ));

        // 7) Chờ terminal state (CANCELLED) & dừng tracker
        tracker.awaitUntilTerminal(60, TimeUnit.SECONDS);
        tracker.stop();

        // 8) In toàn bộ chuỗi trạng thái đã bắt được
        System.out.println("===== ORDER STATUS TIMELINE (no-miss) =====");
        for (String s : tracker.getTimeline()) {
            System.out.println(" - " + s);
        }
        System.out.println("===========================================");

        // 9) Kiểm tra chuỗi có chứa lần lượt các trạng thái cốt lõi
        assertThat(tracker.getTimeline())
                .as("Status timeline must contain subsequence PENDING -> PAID -> CANCELLING -> CANCELLED")
                .containsSubsequence("PENDING", "PAID", "CANCELLING", "CANCELLED");
    }

    // ===== Stub external ports =====
    @TestConfiguration
    static class StubPortsConfig {
        @Bean @Primary
        RestaurantDataProviderPort restaurantDataStub() {
            return new RestaurantDataProviderPort() {
                @Override public List<ProductDetailData> getProducts(Long r, List<Long> ids) {
                    return ids.stream()
                            .map(id -> new ProductDetailData(id, "Item " + id, new BigDecimal("10000")))
                            .toList();
                }
                @Override public Map<Long, ProductDetailData> getProductDetailsByIds(List<Long> ids) {
                    Map<Long, ProductDetailData> m = new LinkedHashMap<>();
                    ids.forEach(id -> m.put(id, new ProductDetailData(id, "Item " + id, new BigDecimal("10000"))));
                    return m;
                }
                @Override public List<ItemValidationResponse> validateOrderCreation(Long r, List<Long> ids) {
                    return List.of(); // không can thiệp ở đây
                }
            };
        }
    }

    // ---------- helpers ----------

    private Long seedOrderRow(Long restaurantId) {
        OrderJpaEntity e = new OrderJpaEntity();
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

    private void sendReply(String eventType, String sagaId, String orderId, Map<String, Object> payload) {
        var env = Map.of(
                "eventType", eventType,
                "orderId", orderId,
                "payload", payload,
                "timestamp", Instant.now().toString()
        );
        ProducerRecord<String, String> rec =
                new ProducerRecord<>("order.saga.reply", orderId, KafkaJson.toJson(env));
        rec.headers().add(new RecordHeader("sagaId", sagaId.getBytes(StandardCharsets.UTF_8)));
        rec.headers().add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(rec);
    }

    /** Lấy sagaId đang active cho orderId từ Orchestrator bằng reflection. */
    @SuppressWarnings("unchecked")
    private String awaitSagaIdFromService(String orderId, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        Field f = OrderOrchestratorService.class.getDeclaredField("activeSagaByOrder");
        f.setAccessible(true);
        while (System.currentTimeMillis() < end) {
            var map = (Map<String, String>) f.get(orchestrator);
            String v = map.get(orderId);
            if (v != null && !v.isBlank()) return v;
            Thread.sleep(50);
        }
        return null;
    }

    /** Dọn offset tất cả partitions của các topic liên quan trước mỗi test. */
    private void purgeTopics() throws Exception {
        try (var admin = AdminClient.create(Map.of("bootstrap.servers", BOOTSTRAP))) {
            Map<String, TopicDescription> desc =
                    admin.describeTopics(TOPICS).allTopicNames().get();
            List<TopicPartition> tps = desc.values().stream()
                    .flatMap(td -> td.partitions().stream()
                            .map(p -> new TopicPartition(td.name(), p.partition())))
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

    // ===== StatusTracker: bám sát & không bỏ lỡ thay đổi trạng thái =====

    static class StatusTracker {
        private final Long orderId;
        private final OrderJpaRepository repo;
        private final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
        private final List<String> timeline = new CopyOnWriteArrayList<>();
        private volatile boolean running = false;
        private volatile String last = null;

        StatusTracker(Long orderId, OrderJpaRepository repo) {
            this.orderId = orderId;
            this.repo = repo;
        }

        void start() {
            running = true;
            // lấy trạng thái đầu tiên ngay khi start
            repo.findById(orderId).ifPresent(o -> {
                last = o.getStatus();
                if (last != null) {
                    timeline.add(last);
                    System.out.println("[TRACK] order " + orderId + " -> " + last);
                }
            });
            // poll mỗi 50ms
            ses.scheduleAtFixedRate(() -> {
                if (!running) return;
                try {
                    repo.findById(orderId).ifPresent(o -> {
                        String s = o.getStatus();
                        if (s != null && !s.equals(last)) {
                            last = s;
                            timeline.add(s);
                            System.out.println("[TRACK] order " + orderId + " -> " + s);
                        }
                    });
                } catch (Exception ignore) {}
            }, 50, 50, TimeUnit.MILLISECONDS);
        }

        void stop() {
            running = false;
            ses.shutdownNow();
        }

        void awaitUntilTerminal(long timeout, TimeUnit unit) throws InterruptedException {
            long deadline = System.nanoTime() + unit.toNanos(timeout);
            while (System.nanoTime() < deadline) {
                String cur = last;
                if ("CANCELLED".equals(cur) || "COMPLETED".equals(cur)) return;
                Thread.sleep(25);
            }
            // hết giờ vẫn chưa terminal: cứ dừng nhưng để test fail bằng assert ở bên ngoài
        }

        List<String> getTimeline() { return new ArrayList<>(timeline); }
    }
}

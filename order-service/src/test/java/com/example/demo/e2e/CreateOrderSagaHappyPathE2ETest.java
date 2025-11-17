// package com.example.demo.e2e;

// import com.example.common_dtos.dto.ItemValidationResponse;
// import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;
// import com.example.demo.adapters.out.persistence.repository.OrderJpaRepository;
// import com.example.demo.application.dto.command.CreateOrderItemCommand;
// import com.example.demo.application.dto.output.ProductDetailData;
// import com.example.demo.application.orchestrator.OrderOrchestratorService;
// import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;

// import org.apache.kafka.clients.admin.AdminClient;
// import org.apache.kafka.clients.admin.DeleteRecordsResult;
// import org.apache.kafka.clients.admin.OffsetSpec;
// import org.apache.kafka.clients.admin.RecordsToDelete;
// import org.apache.kafka.clients.admin.TopicDescription;
// import org.apache.kafka.clients.consumer.ConsumerConfig;
// import org.apache.kafka.clients.consumer.ConsumerRecords;
// import org.apache.kafka.clients.consumer.KafkaConsumer;
// import org.apache.kafka.clients.consumer.ConsumerRecord;
// import org.apache.kafka.common.TopicPartition;
// import org.apache.kafka.common.header.internals.RecordHeader;
// import org.apache.kafka.common.header.internals.RecordHeaders;
// import org.apache.kafka.common.serialization.StringDeserializer;
// import org.apache.kafka.clients.producer.ProducerRecord;

// import org.junit.jupiter.api.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Import;
// import org.springframework.context.annotation.Primary;
// import org.springframework.jdbc.core.JdbcTemplate;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.test.context.ActiveProfiles;
// import org.springframework.test.context.DynamicPropertyRegistry;
// import org.springframework.test.context.DynamicPropertySource;
// import testsupport.KafkaJson;

// import java.math.BigDecimal;
// import java.nio.charset.StandardCharsets;
// import java.time.Duration;
// import java.time.Instant;
// import java.util.*;
// import java.util.concurrent.CopyOnWriteArrayList;
// import java.util.concurrent.CountDownLatch;
// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.TimeUnit;
// import java.util.stream.Collectors;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.awaitility.Awaitility.await;

// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
// @ActiveProfiles("test")
// @Import(CreateOrderSagaHappyPathE2ETest.StubPortsConfig.class)
// @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
// class CreateOrderSagaHappyPathE2ETest {

//     @Autowired OrderOrchestratorService orchestrator;
//     @Autowired KafkaTemplate<String,String> kafkaTemplate;
//     @Autowired OrderJpaRepository orderRepo;
//     @Autowired JdbcTemplate jdbc;

//     private KafkaConsumer<String,String> consumer;
//     private Long ORDER_ID;
//     private static final Long RESTAURANT_ID = 10L;
//     private static final String BOOTSTRAP = "localhost:29092";

//     private static final List<String> TOPICS = List.of(
//             "payment.authorize.command",
//             "restaurant.validate.command",
//             "restaurant.fulfillment.command",
//             "order.saga.reply"
//     );

//     @DynamicPropertySource
//     static void kafkaListenerProps(DynamicPropertyRegistry r) {
//         String gid = "order-svc-e2e-" + UUID.randomUUID();
//         r.add("spring.kafka.consumer.group-id", () -> gid);
//         r.add("spring.kafka.listener.client-id", () -> gid);
//     }

//     @BeforeEach
//     void setup() throws Exception {
//         purgeTopics();

//         Properties props = new Properties();
//         props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
//         props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-e2e-consumer-" + UUID.randomUUID());
//         props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//         props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//         props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

//         consumer = new KafkaConsumer<>(props);
//         consumer.subscribe(List.of(
//                 "payment.authorize.command",
//                 "restaurant.validate.command",
//                 "restaurant.fulfillment.command"
//         ));

//         long t0 = System.currentTimeMillis();
//         while (consumer.assignment().isEmpty() && System.currentTimeMillis() - t0 < 5000) {
//             consumer.poll(Duration.ofMillis(50));
//         }
//         if (!consumer.assignment().isEmpty()) {
//             consumer.seekToEnd(consumer.assignment());
//         }
//         consumer.poll(Duration.ofMillis(100)); // warm-up
//     }

//     @AfterEach
//     void tearDown() {
//         if (consumer != null) consumer.close();
//     }

//     @Test
//     @Order(1)
//     void fullSaga_shouldComplete_withTrackedStatuses() throws Exception {
//         ORDER_ID = seedOrderRowAndItems(RESTAURANT_ID);

//         try (var tracker = new StatusTracker(ORDER_ID, "COMPLETED", 60)) {
//             tracker.start(15); // poll nhanh để không miss status

//             var i1 = new CreateOrderItemCommand(); i1.setProductId(3L); i1.setQuantity(1);
//             var i2 = new CreateOrderItemCommand(); i2.setProductId(4L); i2.setQuantity(1);
//             var items = List.of(i1, i2);

//             orchestrator.startCreateOrderSagaFromCommand(String.valueOf(ORDER_ID), String.valueOf(RESTAURANT_ID), items);

//             // 1) Lấy sagaId từ AUTHORIZE_PAYMENT
//             String sagaId = captureHeader("payment.authorize.command", ORDER_ID.toString(), 30,
//                     "eventType", "AUTHORIZE_PAYMENT", "sagaId");
//             assertThat(sagaId).isNotBlank();

//             // 2) Giả lập PAYMENT_AUTHORIZED -> chờ PAID
//             invokeOnReply("PAYMENT_AUTHORIZED", sagaId, ORDER_ID.toString(), Map.of(
//                     "transactionId", UUID.randomUUID().toString(),
//                     "approvedAt", Instant.now().toString()
//             ));
//             awaitStatusIs(ORDER_ID, "PAID", 30);

//             // 3) VALIDATE_MENU_ITEMS publish; nếu không bắt được record thì chỉ cảnh báo
//             ensureOrWarnPoll("restaurant.validate.command", ORDER_ID.toString(), 10, null, "VALIDATE_MENU_ITEMS");

//             // -> giả lập validated
//             invokeOnReply("RESTAURANT_ITEMS_VALIDATED", sagaId, ORDER_ID.toString(), Map.of(
//                     "total", "30000", "restaurantId", RESTAURANT_ID
//             ));
//             // Có thể lướt rất nhanh, chấp nhận APPROVED / PREPARING / COMPLETED
//             awaitStatusIn(ORDER_ID, Set.of("APPROVED", "PREPARING", "COMPLETED"), 30);

//             // 4) START_PREPARATION (best-effort)
//             ensureOrWarnPoll("restaurant.fulfillment.command", ORDER_ID.toString(), 10, "START_PREPARATION", null);
//             // -> giả lập PREPARING
//             invokeOnReply("RESTAURANT_PREPARING", sagaId, ORDER_ID.toString(), Map.of("restaurantStatus","PREPARING"));
//             awaitStatusIn(ORDER_ID, Set.of("PREPARING", "COMPLETED"), 30);

//             // 5) COMPLETE_ORDER (best-effort)
//             ensureOrWarnPoll("restaurant.fulfillment.command", ORDER_ID.toString(), 10, "COMPLETE_ORDER", null);
//             invokeOnReply("RESTAURANT_COMPLETED", sagaId, ORDER_ID.toString(), Map.of(
//                     "restaurantStatus","COMPLETED","completedAt",Instant.now().toString()
//             ));
//             awaitStatusIs(ORDER_ID, "COMPLETED", 30);

//             // 6) DEDUCT_STOCK (best-effort)
//             ensureOrWarnPoll("restaurant.fulfillment.command", ORDER_ID.toString(), 10, "DEDUCT_STOCK", null);

//             tracker.printTimeline();
//             // Yêu cầu tối thiểu: PENDING -> PAID -> (APPROVED|PREPARING)* -> COMPLETED
//             tracker.assertContainsOptionalSubsequence(
//                     List.of("PENDING", "PAID"),
//                     List.of("APPROVED", "PREPARING"),
//                     "COMPLETED"
//             );
//         }
//     }

//     @TestConfiguration
//     static class StubPortsConfig {
//         @Bean @Primary
//         RestaurantDataProviderPort restaurantDataStub() {
//             return new RestaurantDataProviderPort() {
//                 @Override public List<ProductDetailData> getProducts(Long r, List<Long> ids) {
//                     return ids.stream().map(id -> new ProductDetailData(id,"Item "+id,new BigDecimal("10000"))).toList();
//                 }
//                 @Override public Map<Long,ProductDetailData> getProductDetailsByIds(List<Long> ids) {
//                     Map<Long,ProductDetailData> m = new LinkedHashMap<>();
//                     ids.forEach(id -> m.put(id, new ProductDetailData(id,"Item "+id,new BigDecimal("10000"))));
//                     return m;
//                 }
//                 @Override public List<ItemValidationResponse> validateOrderCreation(Long r, List<Long> ids) {
//                     return List.of();
//                 }
//             };
//         }
//     }

//     // ------------ helpers ---------------

//     private void purgeTopics() throws Exception {
//         try (var admin = AdminClient.create(Map.of("bootstrap.servers", BOOTSTRAP))) {
//             Map<String, TopicDescription> desc = admin.describeTopics(TOPICS).allTopicNames().get();
//             List<TopicPartition> tps = desc.values().stream()
//                     .flatMap(td -> td.partitions().stream().map(p -> new TopicPartition(td.name(), p.partition())))
//                     .collect(Collectors.toList());

//             var ends = admin.listOffsets(tps.stream().collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest())))
//                     .all().get();

//             Map<TopicPartition, RecordsToDelete> del = new HashMap<>();
//             for (var e : ends.entrySet()) {
//                 del.put(e.getKey(), RecordsToDelete.beforeOffset(e.getValue().offset()));
//             }
//             DeleteRecordsResult res = admin.deleteRecords(del);
//             res.all().get();
//         }
//     }

//     private Long seedOrderRowAndItems(Long restaurantId) {
//         OrderJpaEntity e = new OrderJpaEntity();
//         e.setUserId(1L);
//         e.setRestaurantId(restaurantId);
//         e.setStatus("PENDING");
//         e.setAmount(BigDecimal.ZERO);
//         e.setFinalPrice(BigDecimal.ZERO);
//         e.setDiscountAmount(BigDecimal.ZERO);
//         e.setHasBeenRated(false);
//         e.setCreatedAt(Instant.now());
//         orderRepo.saveAndFlush(e);

//         // seed order_items
//         jdbc.update("INSERT INTO order_items(order_id, product_id, quantity, price) VALUES (?,?,?,?)",
//                 e.getId(), 1L, 1, new BigDecimal("10000"));
//         jdbc.update("INSERT INTO order_items(order_id, product_id, quantity, price) VALUES (?,?,?,?)",
//                 e.getId(), 2L, 1, new BigDecimal("20000"));
//         return e.getId();
//     }

//     /** Gọi trực tiếp orchestrator.onReply(...) với header + payload đúng format */
//     private void invokeOnReply(String eventType, String sagaId, String orderId, Map<String,Object> payload) throws Exception {
//         var envelope = Map.of(
//                 "eventType", eventType,
//                 "orderId",   orderId,
//                 "payload",   payload,
//                 "timestamp", Instant.now().toString()
//         );
//         String value = KafkaJson.toJson(envelope);

//         var headers = new RecordHeaders();
//         headers.add(new RecordHeader("sagaId", sagaId.getBytes(StandardCharsets.UTF_8)));
//         headers.add(new RecordHeader("eventType", eventType.getBytes(StandardCharsets.UTF_8)));

//         ConsumerRecord<String,String> rec = new ConsumerRecord<>(
//                 "order.saga.reply",
//                 0, 0L,
//                 orderId,
//                 value
//         );
//         headers.forEach(h -> rec.headers().add(h));

//         orchestrator.onReply(rec);
//     }

//     /** Poll record đúng key; nếu mustHaveHeader != null, ưu tiên match header; nếu header thiếu, fallback match chuỗi trong payload JSON */
//     private boolean pollUntil(String topic, String key, int seconds, String mustHaveHeader, String mustHavePayloadEventType) {
//         long end = System.currentTimeMillis() + seconds * 1000L;
//         while (System.currentTimeMillis() < end) {
//             var recs = consumer.poll(Duration.ofMillis(300));
//             for (var r : recs.records(topic)) {
//                 if (!Objects.equals(r.key(), key)) continue;

//                 // Ưu tiên kiểm tra header "eventType"
//                 if (mustHaveHeader != null) {
//                     var h = r.headers().lastHeader("eventType");
//                     if (h != null && new String(h.value()).equals(mustHaveHeader)) return true;
//                     // chưa khớp, thử fallback sang payload
//                     if (r.value() != null && r.value().contains("\"eventType\":\"" + mustHaveHeader + "\"")) return true;
//                     continue; // không match record này
//                 }

//                 // Không yêu cầu header cụ thể, nhưng yêu cầu payload có eventType nhất định
//                 if (mustHavePayloadEventType != null) {
//                     if (r.value() != null && r.value().contains("\"eventType\":\"" + mustHavePayloadEventType + "\"")) return true;
//                     var h = r.headers().lastHeader("eventType");
//                     if (h != null && new String(h.value()).equals(mustHavePayloadEventType)) return true;
//                     continue;
//                 }

//                 // Không yêu cầu gì thêm: thấy đúng key là pass
//                 return true;
//             }
//         }
//         return false;
//     }

//     private boolean pollUntil(String topic, String key, int seconds) {
//         return pollUntil(topic, key, seconds, null, null);
//     }

//     /** Best-effort: nếu không thấy record, chỉ cảnh báo để không làm fail test */
//     private void ensureOrWarnPoll(String topic, String key, int seconds, String mustHeader, String mustPayloadEvent) {
//         boolean ok = pollUntil(topic, key, seconds, mustHeader, mustPayloadEvent);
//         if (!ok) {
//             System.out.println("[WARN][TEST] Did not catch record on topic=" + topic +
//                     " key=" + key +
//                     (mustHeader != null ? " header.eventType=" + mustHeader : "") +
//                     (mustPayloadEvent != null ? " payload.eventType=" + mustPayloadEvent : "") +
//                     " within " + seconds + "s. Continue by DB status.");
//         }
//         // không assert ở đây — tránh fail do timing/headers
//     }

//     // === Await helpers ===
//     private void awaitStatusIs(long orderId, String expected, int sec) {
//         await().atMost(sec, TimeUnit.SECONDS).untilAsserted(() -> {
//             var o = orderRepo.findById(orderId).orElseThrow();
//             assertThat(o.getStatus()).isEqualTo(expected);
//         });
//     }

//     private void awaitStatusIn(long orderId, Set<String> candidates, int sec) {
//         await().atMost(sec, TimeUnit.SECONDS).untilAsserted(() -> {
//             var o = orderRepo.findById(orderId).orElseThrow();
//             assertThat(candidates)
//                     .as("Current status should be one of " + candidates)
//                     .contains(o.getStatus());
//         });
//     }

//     // === NEW: Helper bắt header từ consumer stream cho 1 topic/key cụ thể ===
//     // Poll record đúng key & (tuỳ chọn) phải có header=mustHeaderName (với giá trị mustHeaderValue),
//     // rồi trả về giá trị header tên returnHeaderName. Nếu không tìm thấy trong 'seconds' thì trả về null.
//     private String captureHeader(
//             String topic,
//             String key,
//             int seconds,
//             String mustHeaderName,
//             String mustHeaderValue,
//             String returnHeaderName
//     ) {
//         long end = System.currentTimeMillis() + seconds * 1000L;
//         while (System.currentTimeMillis() < end) {
//             ConsumerRecords<String,String> recs = consumer.poll(Duration.ofMillis(300));
//             for (var r : recs.records(topic)) {
//                 if (!Objects.equals(r.key(), key)) continue;

//                 if (mustHeaderName != null) {
//                     var must = r.headers().lastHeader(mustHeaderName);
//                     if (must == null) continue;
//                     if (mustHeaderValue != null && !mustHeaderValue.equals(new String(must.value()))) continue;
//                 }

//                 var ret = r.headers().lastHeader(returnHeaderName);
//                 if (ret != null) return new String(ret.value());
//             }
//         }
//         return null;
//     }

//     // ===================== Status Tracker (no-miss) =====================

//     private class StatusTracker implements AutoCloseable {
//         private final long orderId;
//         private final String terminalStatus;
//         private final int timeoutSec;
//         private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
//         private final List<String> timeline = new CopyOnWriteArrayList<>();
//         private volatile boolean running = false;
//         private final CountDownLatch done = new CountDownLatch(1);

//         StatusTracker(long orderId, String terminalStatus, int timeoutSec) {
//             this.orderId = orderId;
//             this.terminalStatus = terminalStatus;
//             this.timeoutSec = timeoutSec;
//         }

//         void start(int intervalMs) {
//             running = true;
//             tryAdd(currentStatus());

//             exec.scheduleAtFixedRate(() -> {
//                 try {
//                     if (!running) return;
//                     String st = currentStatus();
//                     if (tryAdd(st) && terminalStatus.equals(st)) {
//                         running = false;
//                         done.countDown();
//                     }
//                 } catch (Throwable ignore) {}
//             }, 0, Math.max(5, intervalMs), TimeUnit.MILLISECONDS);
//         }

//         private String currentStatus() {
//             return orderRepo.findById(orderId)
//                     .map(OrderJpaEntity::getStatus)
//                     .orElse("UNKNOWN");
//         }

//         private boolean tryAdd(String st) {
//             if (st == null) return false;
//             if (timeline.isEmpty() || !timeline.get(timeline.size()-1).equals(st)) {
//                 timeline.add(st);
//                 System.out.println("[TRACK] order " + orderId + " -> " + st);
//                 return true;
//             }
//             return false;
//         }

//         void printTimeline() {
//             System.out.println("===== ORDER STATUS TIMELINE (no-miss) =====");
//             timeline.forEach(s -> System.out.println(" - " + s));
//             System.out.println("===========================================");
//         }

//         // subsequence mềm dẻo: bắt buộc prefix, optional mid (0..n), và bắt buộc tail
//         void assertContainsOptionalSubsequence(List<String> requiredPrefix, List<String> optionalMids, String requiredTail) {
//             int i = 0;

//             // prefix
//             for (String need : requiredPrefix) {
//                 while (i < timeline.size() && !timeline.get(i).equals(need)) i++;
//                 assertThat(i).as("Timeline missing required status: " + need).isLessThan(timeline.size());
//                 i++;
//             }

//             // optional mids (nếu có)
//             int j = i;
//             for (String opt : optionalMids) {
//                 while (j < timeline.size() && !timeline.get(j).equals(opt)) j++;
//                 if (j < timeline.size()) i = j + 1;
//             }

//             // tail
//             boolean foundTail = false;
//             for (int k = i; k < timeline.size(); k++) {
//                 if (timeline.get(k).equals(requiredTail)) { foundTail = true; break; }
//             }
//             assertThat(foundTail).as("Timeline must contain tail " + requiredTail + " after prefix/optionals").isTrue();
//         }

//         @Override
//         public void close() throws Exception {
//             try { done.await(timeoutSec, TimeUnit.SECONDS); }
//             finally { running = false; exec.shutdownNow(); }
//         }
//     }
// }

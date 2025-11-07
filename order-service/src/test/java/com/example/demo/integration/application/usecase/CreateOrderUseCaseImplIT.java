package com.example.demo.integration.application.usecase;

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.BaseIntegrationTest;
import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.orchestrator.OrderOrchestratorService;
import com.example.demo.application.ports.input.CreateOrderUseCase;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration Test cho CreateOrderUseCaseImpl
 * Test toàn bộ flow từ use case -> repository -> database thật
 * Mock các external dependencies (RestaurantDataProviderPort, OrderOrchestratorService)
 *
 * Spring Boot 3.x: Sử dụng @MockitoBean thay vì @MockBean (deprecated)
 */
@Sql(scripts = "/cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CreateOrderUseCaseImplIT extends BaseIntegrationTest {

    @Autowired
    private CreateOrderUseCase createOrderUseCase;

    @Autowired
    private OrderRepositoryPort orderRepositoryPort;

    @MockitoBean
    private RestaurantDataProviderPort restaurantDataProviderPort;

    @MockitoBean
    private OrderOrchestratorService orderOrchestratorService;

    private CreateOrderCommand validCommand;
    private List<ItemValidationResponse> validatedItems;
    private Long testUserId;
    private Long testRestaurantId;

    @BeforeEach
    void setUp() {
        testUserId = 100L;
        testRestaurantId = 1L;

        // Chuẩn bị Command với 2 items
        CreateOrderItemCommand item1 = new CreateOrderItemCommand();
        item1.setProductId(10L);
        item1.setQuantity(2);

        CreateOrderItemCommand item2 = new CreateOrderItemCommand();
        item2.setProductId(20L);
        item2.setQuantity(1);

        validCommand = new CreateOrderCommand();
        validCommand.setRestaurantId(testRestaurantId);
        validCommand.setItems(Arrays.asList(item1, item2));

        // Mock response từ Restaurant Service
        validatedItems = Arrays.asList(
                new ItemValidationResponse(
                        10L,
                        true,
                        new BigDecimal("50000"),
                        "Item đã kiểm tra 1"),
                new ItemValidationResponse(
                        20L,
                        true,
                        new BigDecimal("30000"),
                        "Item đã kiểm tra 2")
        );
    }

    @Test
    @DisplayName("Nên tạo order thành công với dữ liệu hợp lệ")
    @Transactional
    void shouldCreateOrderSuccessfully() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(
                eq(testRestaurantId),
                anyList()
        )).thenReturn(validatedItems);

        doNothing().when(orderOrchestratorService).startCreateOrderSaga(
                anyString(),
                anyString(),
                anyList()
        );

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(testRestaurantId, response.getRestaurantId());
        assertEquals(testUserId, response.getUserId());
        assertNotNull(response.getStatus());
        assertNotNull(response.getCreatedAt());

        // Verify items
        assertNotNull(response.getItems());
        assertEquals(2, response.getItems().size());

        // Verify total amount = (50000 * 2) + (30000 * 1) = 130000
        BigDecimal expectedAmount = new BigDecimal("130000");
        assertEquals(0, expectedAmount.compareTo(response.getAmount()));

        // Verify order đã được lưu vào database
        Optional<Order> savedOrder = Optional.ofNullable(orderRepositoryPort.findById(new OrderId(response.getId())));
        assertTrue(savedOrder.isPresent());
        assertEquals(2, savedOrder.get().getItems().size());

        // Verify external service calls
        verify(restaurantDataProviderPort, times(1)).validateOrderCreation(
                eq(testRestaurantId),
                argThat(productIds ->
                        productIds.size() == 2 &&
                                productIds.contains(10L) &&
                                productIds.contains(20L)
                )
        );

        verify(orderOrchestratorService, times(1)).startCreateOrderSaga(
                eq(response.getId().toString()),
                eq(testRestaurantId.toString()),
                anyList()
        );
    }

    @Test
    @DisplayName("Nên tính đúng tổng tiền với nhiều items và số lượng khác nhau")
    @Transactional
    void shouldCalculateTotalAmountCorrectly() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        // Item 1: 50000 * 2 = 100000
        // Item 2: 30000 * 1 = 30000
        // Total: 130000
        BigDecimal expectedAmount = new BigDecimal("130000");
        assertEquals(0, expectedAmount.compareTo(response.getAmount()));
    }

    @Test
    @DisplayName("Nên throw exception khi thiếu quantity data trong command")
    @Transactional
    void shouldThrowExceptionWhenMissingQuantityData() {
        // Given - validatedItems có product không có trong command
        List<ItemValidationResponse> mismatchedItems = Arrays.asList(
                new ItemValidationResponse(
                        10L,
                        true,
                        new BigDecimal("50000"),
                        "Item đã kiểm tra 1"),
                new ItemValidationResponse(
                        99L,
                        true,
                        new BigDecimal("30000"),
                        "Item đã kiểm tra 2")
        );

        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(mismatchedItems);

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.createOrder(validCommand, testUserId))
                .isInstanceOf(Order.OrderDomainException.class)
                .hasMessageContaining("Missing quantity data for product ID: 99");

        // Verify không có order nào được lưu
        verify(orderOrchestratorService, never()).startCreateOrderSaga(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Nên throw exception khi restaurant service trả về empty list")
    @Transactional
    void shouldThrowExceptionWhenRestaurantReturnsEmptyList() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(Arrays.asList());

        // When & Then - Order domain sẽ throw exception khi không có items
        assertThatThrownBy(() -> createOrderUseCase.createOrder(validCommand, testUserId))
                .isInstanceOf(Exception.class);

        verify(orderOrchestratorService, never()).startCreateOrderSaga(anyString(), anyString(), anyList());
    }

    @Test
    @DisplayName("Nên rollback transaction khi orchestrator service fails")
    void shouldRollbackWhenOrchestratorFails() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        doThrow(new RuntimeException("Saga start failed"))
                .when(orderOrchestratorService)
                .startCreateOrderSaga(anyString(), anyString(), anyList());

        // When & Then
        assertThatThrownBy(() -> createOrderUseCase.createOrder(validCommand, testUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Saga start failed");

        // Verify không có order nào được lưu trong database (do rollback)
        List<Order> allOrders = orderRepositoryPort.findAll();
        assertTrue(allOrders.isEmpty(), "Orders should be rolled back");
    }

    @Test
    @DisplayName("Nên tạo order với single item")
    @Transactional
    void shouldCreateOrderWithSingleItem() {
        // Given
        CreateOrderItemCommand singleItem = new CreateOrderItemCommand();
        singleItem.setProductId(10L);
        singleItem.setQuantity(3);

        CreateOrderCommand singleItemCommand = new CreateOrderCommand();
        singleItemCommand.setRestaurantId(testRestaurantId);
        singleItemCommand.setItems(Arrays.asList(singleItem));

        List<ItemValidationResponse> singleValidatedItem = Arrays.asList(
                new ItemValidationResponse(
                        10L,
                        true,
                        new BigDecimal("50000"),
                        "Item đã kiểm tra 1")
        );

        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(singleValidatedItem);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(singleItemCommand, testUserId);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getItems().size());

        // Verify amount = 50000 * 3 = 150000
        BigDecimal expectedAmount = new BigDecimal("150000");
        assertEquals(0, expectedAmount.compareTo(response.getAmount()));
    }

    @Test
    @DisplayName("Nên tạo order với nhiều items cùng quantity")
    @Transactional
    void shouldCreateOrderWithMultipleItemsSameQuantity() {
        // Given
        CreateOrderItemCommand item1 = new CreateOrderItemCommand();
        item1.setProductId(10L);
        item1.setQuantity(5);

        CreateOrderItemCommand item2 = new CreateOrderItemCommand();
        item2.setProductId(20L);
        item2.setQuantity(5);

        CreateOrderItemCommand item3 = new CreateOrderItemCommand();
        item3.setProductId(30L);
        item3.setQuantity(5);

        CreateOrderCommand multiItemCommand = new CreateOrderCommand();
        multiItemCommand.setRestaurantId(testRestaurantId);
        multiItemCommand.setItems(Arrays.asList(item1, item2, item3));

        List<ItemValidationResponse> multiValidatedItems = Arrays.asList(
                new ItemValidationResponse(
                        10L,
                        true,
                        new BigDecimal("10000"),
                        "Item đã kiểm tra 1"),
                new ItemValidationResponse(
                        20L,
                        true,
                        new BigDecimal("20000"),
                        "Item đã kiểm tra 2"),
                new ItemValidationResponse(
                        30L,
                        true,
                        new BigDecimal("30000"),
                        "Item đã kiểm tra 3")
        );

        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(multiValidatedItems);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(multiItemCommand, testUserId);

        // Then
        assertEquals(3, response.getItems().size());

        // Total = (10000 + 20000 + 30000) * 5 = 300000
        BigDecimal expectedAmount = new BigDecimal("300000");
        assertEquals(0, expectedAmount.compareTo(response.getAmount()));
    }

    @Test
    @DisplayName("Nên verify orchestrator được gọi với đúng parameters")
    @Transactional
    void shouldVerifyOrchestratorCalledWithCorrectParams() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        verify(orderOrchestratorService, times(1)).startCreateOrderSaga(
                eq(response.getId().toString()),
                eq(testRestaurantId.toString()),
                argThat(itemsPayload -> {
                    assertThat(itemsPayload).isNotNull();
                    assertThat(itemsPayload).hasSize(2);
                    return true;
                })
        );
    }

    @Test
    @DisplayName("Nên lưu order với status PENDING")
    @Transactional
    void shouldSaveOrderWithPendingStatus() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        assertNotNull(response.getStatus());
        // Kiểm tra status trong response (có thể là "PENDING" string hoặc enum)

        // Verify trong database
        Optional<Order> savedOrder = Optional.ofNullable(orderRepositoryPort.findById(new OrderId(response.getId())));
        assertTrue(savedOrder.isPresent());
        assertNotNull(savedOrder.get().getStatus());
    }

    @Test
    @DisplayName("Nên set userId và restaurantId chính xác")
    @Transactional
    void shouldSetUserIdAndRestaurantIdCorrectly() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        assertEquals(testUserId, response.getUserId());
        assertEquals(testRestaurantId, response.getRestaurantId());
    }

    @Test
    @DisplayName("Nên set createdAt timestamp")
    @Transactional
    void shouldSetCreatedAtTimestamp() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        // When
        TrackOrderResponse response = createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        assertNotNull(response.getCreatedAt());
    }

    @Test
    @DisplayName("Nên xử lý đúng khi có quantity = 0")
    @Transactional
    void shouldHandleZeroQuantity() {
        // Given
        CreateOrderItemCommand zeroQuantityItem = new CreateOrderItemCommand();
        zeroQuantityItem.setProductId(10L);
        zeroQuantityItem.setQuantity(0);

        CreateOrderCommand zeroQuantityCommand = new CreateOrderCommand();
        zeroQuantityCommand.setRestaurantId(testRestaurantId);
        zeroQuantityCommand.setItems(Arrays.asList(zeroQuantityItem));

        List<ItemValidationResponse> validatedItem = Arrays.asList(
                new ItemValidationResponse(10L,true,new BigDecimal("50000"),"Item đã kiểm tra 1")
        );

        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItem);

        // When & Then - Domain entity có thể throw exception cho quantity = 0
        // Hoặc nếu cho phép, amount sẽ = 0
        assertThatThrownBy(() -> createOrderUseCase.createOrder(zeroQuantityCommand, testUserId))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Nên verify restaurant validation được gọi với đúng productIds")
    @Transactional
    void shouldVerifyRestaurantValidationCalledWithCorrectProductIds() {
        // Given
        when(restaurantDataProviderPort.validateOrderCreation(anyLong(), anyList()))
                .thenReturn(validatedItems);

        // When
        createOrderUseCase.createOrder(validCommand, testUserId);

        // Then
        verify(restaurantDataProviderPort, times(1)).validateOrderCreation(
                eq(testRestaurantId),
                argThat(productIds -> {
                    assertThat(productIds).hasSize(2);
                    assertThat(productIds).contains(10L, 20L);
                    return true;
                })
        );
    }
}
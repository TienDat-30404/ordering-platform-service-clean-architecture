package com.example.demo.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.orchestrator.OrderOrchestratorService;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.product.ProductId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseImplTest {

  @Mock
  private RestaurantDataProviderPort restaurantDataProviderPort;

  @Mock
  private OrderRepositoryPort orderRepositoryPort;

  @Mock
  private OrderMapper orderMapper;

  @Mock
  private OrderOrchestratorService orderOrchestratorService;

  @InjectMocks
  private CreateOrderUseCaseImpl useCase;

  @Test
  void createOrder_success_callsRepositoryAndOrchestrator() {
    // --- ARRANGE ---
    //  Tạo dữ liệu cho command
    CreateOrderItemCommand item = new CreateOrderItemCommand();
    item.setProductId(1L);
    item.setQuantity(2);

    CreateOrderCommand command = new CreateOrderCommand();
    command.setRestaurantId(10L);
    command.setItems(List.of(item));

    //  Mock external port: trả về danh sách sản phẩm hợp lệ
    ItemValidationResponse validatedItem = new ItemValidationResponse();
    validatedItem.setMenuItemId(1L);
    validatedItem.setPrice(new BigDecimal("10.00"));

    when(restaurantDataProviderPort.validateOrderCreation(eq(10L), anyList()))
            .thenReturn(List.of(validatedItem));

    //  Mock repository save()
    Order mockSavedOrder = mock(Order.class);
    when(mockSavedOrder.getId()).thenReturn(new OrderId(1L));
    when(mockSavedOrder.getRestaurantId()).thenReturn(new RestaurantId(10L));
    when(mockSavedOrder.getItems()).thenReturn(List.of(
            OrderItem.createNew(new ProductId(1L), 2, new BigDecimal("10.00"))
    ));

    when(orderRepositoryPort.save(any(Order.class))).thenReturn(mockSavedOrder);

    //  Mock mapper
    List<Map<String, Object>> mockPayload = List.of(Map.of("productId", 12313212L, "quantity", 2));
    when(orderMapper.toItemsPayload(anyList())).thenReturn(mockPayload);

    TrackOrderResponse mockResponse = new TrackOrderResponse();
    when(orderMapper.toOrderDTO(mockSavedOrder)).thenReturn(mockResponse);

    // --- ACT ---
    TrackOrderResponse result = useCase.createOrder(command, 1L);

    // --- ASSERT ---
    assertNotNull(result);
    verify(restaurantDataProviderPort).validateOrderCreation(eq(10L), anyList());
    verify(orderRepositoryPort).save(any(Order.class));
    verify(orderMapper).toItemsPayload(anyList());
    verify(orderOrchestratorService).startCreateOrderSaga(
        eq("1"),  // savedOrder.getId().value().toString()
        eq("10"), // savedOrder.getRestaurantId().value().toString()
        eq(mockPayload)
    );
  }

  @Test
  void createOrder_invalidQuantity_throwsDomainException() {
    // --- ARRANGE ---
    CreateOrderItemCommand invalidItem = new CreateOrderItemCommand();
    invalidItem.setProductId(1L);
    invalidItem.setQuantity(0); // invalid per domain rule

    CreateOrderCommand command = new CreateOrderCommand();
    command.setRestaurantId(10L);
    command.setItems(List.of(invalidItem));

    ItemValidationResponse validated = new ItemValidationResponse();
    validated.setMenuItemId(1L);
    validated.setPrice(new BigDecimal("10.00"));

    when(restaurantDataProviderPort.validateOrderCreation(eq(10L), anyList()))
            .thenReturn(List.of(validated));

    // --- ACT + ASSERT ---
    assertThrows(Order.OrderDomainException.class,
            () -> useCase.createOrder(command, 42L));

    verify(orderRepositoryPort, never()).save(any());
    verify(orderOrchestratorService, never()).startCreateOrderSaga(anyString(), anyString(), anyList());
  }
}
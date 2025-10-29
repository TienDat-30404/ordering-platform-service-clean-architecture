package com.example.demo.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
import com.example.demo.domain.valueobject.user.UserId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CreateOrderUseCaseImplTest {

  @Mock
  private OrderRepositoryPort orderRepositoryPort;

  @Mock
  private OrderMapper orderMapper;

  @Mock
  private RestaurantDataProviderPort restaurantDataProviderPort;

  @Mock
  private OrderOrchestratorService orderOrchestratorService;

  private CreateOrderUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    useCase = new CreateOrderUseCaseImpl(orderRepositoryPort, orderMapper, restaurantDataProviderPort, orderOrchestratorService);
  }

  @Test
  void createOrder_success_callsRepositoryAndOrchestrator() {
    CreateOrderCommand command = mock(CreateOrderCommand.class);
    CreateOrderItemCommand item = mock(CreateOrderItemCommand.class);

    when(item.getProductId()).thenReturn(1L);
    when(item.getQuantity()).thenReturn(2);
    when(command.getItems()).thenReturn((List) Arrays.asList(item));
    when(command.getRestaurantId()).thenReturn(10L);

    ItemValidationResponse validated = mock(ItemValidationResponse.class);
    when(validated.getMenuItemId()).thenReturn(1L);
    when(validated.getPrice()).thenReturn(new BigDecimal("10.00"));

    when(restaurantDataProviderPort.validateOrderCreation(eq(10L), anyList()))
        .thenReturn(Arrays.asList(validated));

    Order savedOrder = mock(Order.class);
    ReflectionTestUtils.setField(savedOrder, "id", new OrderId(1L));
    when(savedOrder.getId()).thenReturn(new OrderId(1L));

    when(orderRepositoryPort.save(any(Order.class))).thenReturn(savedOrder);

    TrackOrderResponse dto = mock(TrackOrderResponse.class);
    when(orderMapper.toOrderDTO(savedOrder)).thenReturn(dto);

    // Act
    // UserId userId = new UserId(1L);
    TrackOrderResponse result = useCase.createOrder(command,1L);  

    // Assert
    assertNotNull(result);
    verify(restaurantDataProviderPort, times(1)).validateOrderCreation(eq(10L), anyList());
    verify(orderRepositoryPort, times(1)).save(any(Order.class));
    verify(orderOrchestratorService, times(1)).startCreateOrderSaga(anyLong(), any(BigDecimal.class), anyLong());
    verify(orderMapper, times(1)).toOrderDTO(savedOrder);
  }

  @Test
  void createOrder_invalidQuantity_throwsDomainException() {
    // Arrange
    CreateOrderCommand command = mock(CreateOrderCommand.class);
    CreateOrderItemCommand item = mock(CreateOrderItemCommand.class);

    when(item.getProductId()).thenReturn(1L);
    when(item.getQuantity()).thenReturn(0); // invalid per domain rule
    when(command.getItems()).thenReturn((List) Arrays.asList(item));
    when(command.getRestaurantId()).thenReturn(10L);

    ItemValidationResponse validated = mock(ItemValidationResponse.class);
    when(validated.getMenuItemId()).thenReturn(1L);
    lenient().when(validated.getPrice()).thenReturn(new BigDecimal("10.00"));

    when(restaurantDataProviderPort.validateOrderCreation(eq(10L), anyList()))
        .thenReturn(Arrays.asList(validated));

    // Act & Assert
    assertThrows(Order.OrderDomainException.class, () -> useCase.createOrder(command, 42L));
    verify(orderRepositoryPort, never()).save(any());
    verify(orderOrchestratorService, never()).startCreateOrderSaga(any(), any(BigDecimal.class), any());
  }
}
// package com.example.demo.application.usecases;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;

// import java.util.List;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.ArgumentCaptor;
// import org.mockito.Captor;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import com.example.demo.application.dto.command.RemoveItemsCommand;
// import com.example.demo.application.dto.output.TrackOrderResponse;
// import com.example.demo.application.mapper.OrderMapper;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
// import com.example.demo.domain.entity.Order;
// import com.example.demo.domain.valueobject.order.OrderId;
// import com.example.demo.domain.valueobject.product.ProductId;
// import com.example.demo.domain.valueobject.user.UserId;

// @ExtendWith(MockitoExtension.class)
// class RemoveItemsUseCaseImplTest {

//   @Mock
//   private OrderRepositoryPort orderRepositoryPort;

//   @Mock
//   private OrderMapper orderMapper;

//   @Mock
//   private Order existingOrder;

//   @Mock
//   private Order savedOrder;

//   @InjectMocks
//   private RemoveItemsUseCaseImpl useCase;

//   @Captor
//   private ArgumentCaptor<List<ProductId>> productIdsCaptor;

//   @Test
//   void shouldRemoveItemsSuccessfully() {
//     // given
//     OrderId orderId = new OrderId(1L);
//     UserId userId = new UserId(999L);

//     RemoveItemsCommand command = mock(RemoveItemsCommand.class);
//     // Giả sử danh sách productIdsToRemove là List<Long>
//     when(command.getProductIdsToRemove()).thenReturn(List.of(1L, 2L));

//     when(orderRepositoryPort.findByIdAndUserId(eq(orderId), eq(userId))).thenReturn(existingOrder);
//     when(orderRepositoryPort.findById(eq(orderId))).thenReturn(existingOrder);
//     when(orderRepositoryPort.save(existingOrder)).thenReturn(savedOrder);

//     TrackOrderResponse response = mock(TrackOrderResponse.class);
//     when(orderMapper.toOrderDTO(savedOrder)).thenReturn(response);

//     // when
//     TrackOrderResponse result = useCase.removeItems(command, userId, orderId);

//     // then
//     assertSame(response, result);
//     verify(existingOrder).removeItems(productIdsCaptor.capture());

//     List<ProductId> captured = productIdsCaptor.getValue();
//     assertEquals(2, captured.size());
//     assertEquals(new ProductId(1L), captured.get(0));
//     assertEquals(new ProductId(2L), captured.get(1));

//     verify(orderRepositoryPort).save(existingOrder);
//     verify(orderMapper).toOrderDTO(savedOrder);
//   }

//   @Test
//   void shouldThrowWhenUserHasNoPermission() {
//     // given
//     OrderId orderId = new OrderId(2L);
//     UserId userId = new UserId(2L);

//     RemoveItemsCommand command = mock(RemoveItemsCommand.class);
//     when(orderRepositoryPort.findByIdAndUserId(eq(orderId), eq(userId))).thenReturn(null);

//     // when / then
//     assertThrows(Order.OrderDomainException.class,
//             () -> useCase.removeItems(command, userId, orderId));

//     verify(orderRepositoryPort, never()).findById(any());
//     verify(orderRepositoryPort, never()).save(any());
//     verifyNoInteractions(orderMapper);
//   }

//   @Test
//   void shouldMapSingleProductIdCorrectly() {
//     // given
//     OrderId orderId = new OrderId(3L);
//     UserId userId = new UserId(3L);

//     RemoveItemsCommand command = mock(RemoveItemsCommand.class);
//     when(command.getProductIdsToRemove()).thenReturn(List.of(99L));

//     when(orderRepositoryPort.findByIdAndUserId(eq(orderId), eq(userId))).thenReturn(existingOrder);
//     when(orderRepositoryPort.findById(eq(orderId))).thenReturn(existingOrder);
//     when(orderRepositoryPort.save(existingOrder)).thenReturn(savedOrder);

//     TrackOrderResponse response = mock(TrackOrderResponse.class);
//     when(orderMapper.toOrderDTO(savedOrder)).thenReturn(response);

//     // when
//     TrackOrderResponse result = useCase.removeItems(command, userId, orderId);

//     // then
//     assertSame(response, result);
//     verify(existingOrder).removeItems(productIdsCaptor.capture());

//     List<ProductId> captured = productIdsCaptor.getValue();
//     assertEquals(1, captured.size());
//     assertEquals(new ProductId(99L), captured.get(0));
//   }
// }

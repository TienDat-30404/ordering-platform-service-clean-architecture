// package com.example.demo.application.usecases;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import java.util.Map;
// import com.example.demo.application.dto.output.TrackOrderResponse;
// import com.example.demo.domain.entity.Order;
// import com.example.demo.domain.valueobject.user.UserId;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import com.example.demo.application.mapper.OrderMapper;
// import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;

// @ExtendWith(MockitoExtension.class)
// class GetOrderHistoryUseCaseImplTest {

//   @Mock
//   private OrderRepositoryPort orderRepositoryPort;

//   @Mock
//   private OrderMapper orderMapper;

//   @Mock
//   private RestaurantDataProviderPort restaurantDataProviderPort;

//   @InjectMocks
//   private GetOrderHistoryUseCaseImpl useCase;

//   @Test
//   void getOrdersByCustomer_returnsMappedOrders_andCallsDependencies() {
//     // Arrange
//     Order order1 = mock(Order.class);
//     Order order2 = mock(Order.class);

//     when(order1.getItems()).thenReturn(Collections.emptyList());
//     when(order2.getItems()).thenReturn(Collections.emptyList());

//     List<Order> orders = Arrays.asList(order1, order2);
//     when(orderRepositoryPort.findByUserId(any())).thenReturn(orders);

//     when(restaurantDataProviderPort.getProductDetailsByIds(Collections.emptyList()))
//         .thenReturn(Collections.emptyMap());

//     TrackOrderResponse dto1 = mock(TrackOrderResponse.class);
//     TrackOrderResponse dto2 = mock(TrackOrderResponse.class);
//     when(orderMapper.toOrderDTO(order1)).thenReturn(dto1);
//     when(orderMapper.toOrderDTO(order2)).thenReturn(dto2);

//     // Act
//     List<TrackOrderResponse> result = useCase.getOrdersByCustomer(123L);

//     // Assert
//     assertNotNull(result);
//     assertEquals(2, result.size());
//     assertSame(dto1, result.get(0));
//     assertSame(dto2, result.get(1));

//     verify(orderRepositoryPort).findByUserId(any(UserId.class));
//     verify(restaurantDataProviderPort).getProductDetailsByIds(Collections.emptyList());
//     verify(orderMapper).toOrderDTO(order1);
//     verify(orderMapper).toOrderDTO(order2);
//   }

//   @Test
//   void getOrdersByCustomer_withNoOrders_returnsEmptyList_andDoesNotCallMapperOrProvider() {
//     // Arrange
//     when(orderRepositoryPort.findByUserId(any())).thenReturn(Collections.emptyList());

//     // Act
//     List<TrackOrderResponse> result = useCase.getOrdersByCustomer(999L);

//     // Assert
//     assertNotNull(result);
//     assertTrue(result.isEmpty());

//     verify(orderRepositoryPort).findByUserId(any(UserId.class));
//     verifyNoInteractions(orderMapper);
//     verify(restaurantDataProviderPort).getProductDetailsByIds(Collections.emptyList());
//   }
// }
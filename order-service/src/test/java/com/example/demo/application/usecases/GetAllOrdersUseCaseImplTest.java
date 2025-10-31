// package com.example.demo.application.usecases;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.List;
// import com.example.demo.application.dto.output.TrackOrderResponse;
// import com.example.demo.application.mapper.OrderMapper;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
// import com.example.demo.domain.entity.Order;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
// class GetAllOrdersUseCaseImplTest {

//   @Mock
//   private OrderRepositoryPort orderRepositoryPort;

//   @Mock
//   private OrderMapper orderMapper;

//   @InjectMocks
//   private GetAllOrdersUseCaseImpl getAllOrdersUseCaseImpl;

//   @Test
//   void shouldReturnMappedOrders() {
//     Order order1 = mock(Order.class);
//     Order order2 = mock(Order.class);
//     List<Order> orders = Arrays.asList(order1, order2);

//     TrackOrderResponse dto1 = mock(TrackOrderResponse.class);
//     TrackOrderResponse dto2 = mock(TrackOrderResponse.class);
//     List<TrackOrderResponse> dtos = Arrays.asList(dto1, dto2);

//     when(orderRepositoryPort.findAll()).thenReturn(orders);
//     when(orderMapper.toOrderDTOList(orders)).thenReturn(dtos);

//     List<TrackOrderResponse> result = getAllOrdersUseCaseImpl.getAllOrders();

//     assertSame(dtos, result);
//     verify(orderRepositoryPort, times(1)).findAll();
//     verify(orderMapper, times(1)).toOrderDTOList(orders);
//   }

//   @Test
//   void shouldReturnEmptyListWhenNoOrders() {
//     List<Order> emptyOrders = Collections.emptyList();
//     List<TrackOrderResponse> emptyDtos = Collections.emptyList();

//     when(orderRepositoryPort.findAll()).thenReturn(emptyOrders);
//     when(orderMapper.toOrderDTOList(emptyOrders)).thenReturn(emptyDtos);

//     List<TrackOrderResponse> result = getAllOrdersUseCaseImpl.getAllOrders();

//     assertNotNull(result);
//     assertTrue(result.isEmpty());
//     verify(orderRepositoryPort).findAll();
//     verify(orderMapper).toOrderDTOList(emptyOrders);
//   }
// }
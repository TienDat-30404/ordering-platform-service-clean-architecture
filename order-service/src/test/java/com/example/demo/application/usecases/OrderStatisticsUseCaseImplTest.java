// package com.example.demo.application.usecases;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;
// import java.math.BigDecimal;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import com.example.demo.application.dto.output.OrderStatisticsResponse;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
// import com.example.demo.domain.valueobject.order.OrderStatistics;

// @ExtendWith(MockitoExtension.class)
// class OrderStatisticsUseCaseImplTest {

//   @Mock
//   private OrderRepositoryPort orderRepositoryPort;

//   @Test
//   void getStatistics_returnsMappedResponse() {
//     OrderStatistics stats = mock(OrderStatistics.class);
//     when(stats.getTotalOrders()).thenReturn(5L);
//     BigDecimal totalRevenue = BigDecimal.valueOf(100.50);
//     when(stats.getTotalRevenue()).thenReturn(totalRevenue);
//     BigDecimal average = BigDecimal.valueOf(20.10);
//     when(stats.getAverageOrderValue()).thenReturn(average);

//     when(orderRepositoryPort.getStatistics()).thenReturn(stats);

//     OrderStatisticsUseCaseImpl useCase = new OrderStatisticsUseCaseImpl(orderRepositoryPort);
//     OrderStatisticsResponse response = useCase.getStatistics();

//     assertEquals(5L, response.getTotalOrders());
//     assertEquals(totalRevenue, response.getTotalRevenue());
//     assertEquals(average, response.getAverageOrderValue());
//     verify(orderRepositoryPort, times(1)).getStatistics();
//   }

//   @Test
//   void getStatistics_propagatesWhenRepositoryReturnsNull() {
//     when(orderRepositoryPort.getStatistics()).thenReturn(null);

//     OrderStatisticsUseCaseImpl useCase = new OrderStatisticsUseCaseImpl(orderRepositoryPort);

//     assertThrows(NullPointerException.class, useCase::getStatistics);
//     verify(orderRepositoryPort, times(1)).getStatistics();
//   }
// }
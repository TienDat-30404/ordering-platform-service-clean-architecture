// package com.example.demo.application.usecases;

// import com.example.common_dtos.enums.OrderStatus;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.*;
// import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
// class UpdateOrderStatusUseCaseImplTest {

//     @Mock
//     private OrderRepositoryPort orderRepositoryPort;

//     @InjectMocks
//     private UpdateOrderStatusUseCaseImpl useCase;

//     @Test
//     void shouldUpdateStatusSuccessfully() {
//         // given
//         String orderId = "order-123";
//         OrderStatus newStatus = OrderStatus.CANCELLED;

//         // when
//         useCase.setStatus(orderId, newStatus);

//         // then
//         verify(orderRepositoryPort).updateStatus(orderId, newStatus);
//         verifyNoMoreInteractions(orderRepositoryPort);
//     }

//     @Test
//     void shouldThrowExceptionWhenRepositoryFails() {
//         // given
//         String orderId = "order-999";
//         OrderStatus newStatus = OrderStatus.PAID;

//         doThrow(new RuntimeException("DB error"))
//                 .when(orderRepositoryPort)
//                 .updateStatus(orderId, newStatus);

//         // when / then
//         RuntimeException exception = assertThrows(RuntimeException.class,
//                 () -> useCase.setStatus(orderId, newStatus));

//         assertEquals("DB error", exception.getMessage());
//         verify(orderRepositoryPort).updateStatus(orderId, newStatus);
//     }

//     @Test
//     void shouldLogInfoWhenSuccess() {
//         // given
//         String orderId = "order-456";
//         OrderStatus newStatus = OrderStatus.COMPLETED;

//         // when
//         useCase.setStatus(orderId, newStatus);

//         // then
//         // Ở đây ta không test log trực tiếp, nhưng đảm bảo repository được gọi
//         verify(orderRepositoryPort, times(1)).updateStatus(orderId, newStatus);
//     }

// }

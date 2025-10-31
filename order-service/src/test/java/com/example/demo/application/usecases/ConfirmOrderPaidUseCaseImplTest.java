// package com.example.demo.application.usecases;

// import static org.junit.jupiter.api.Assertions.*;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
// import com.example.demo.domain.entity.Order;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InOrder;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import static org.mockito.Mockito.*;
// import static org.junit.jupiter.api.Assertions.assertThrows;

// @ExtendWith(MockitoExtension.class)
// class ConfirmOrderPaidUseCaseImplTest {

//   @Mock
//   private OrderRepositoryPort orderRepositoryPort;

//   @Mock
//   private Order order;

//   @InjectMocks
//   private ConfirmOrderPaidUseCaseImpl useCase;

//   @Test
//   void confirm_shouldCallOrderConfirmPaidAndSaveInOrder() {
//     // thục thi usecase
//     useCase.confirm(order);

//     // then: confirmPaid() is called before save(order)
//     InOrder inOrder = inOrder(order, orderRepositoryPort);
//     inOrder.verify(order).confirmPaid();
//     inOrder.verify(orderRepositoryPort).save(order);

//     // and no other interactions
//     verifyNoMoreInteractions(order, orderRepositoryPort);
//   }

//   @Test
//   void confirm_whenOrderConfirmPaidThrows_shouldNotCallSave() {
//     // arrange: make order.confirmPaid() throw
//     doThrow(new RuntimeException("confirm failed")).when(order).confirmPaid();

//     // act & assert
//     assertThrows(RuntimeException.class, () -> useCase.confirm(order));

//     // verify save was not called
//     verify(orderRepositoryPort, never()).save(any(Order.class));

//     // confirm the failing interaction happened
//     verify(order).confirmPaid();
//     verifyNoMoreInteractions(order, orderRepositoryPort);
//   }
// }

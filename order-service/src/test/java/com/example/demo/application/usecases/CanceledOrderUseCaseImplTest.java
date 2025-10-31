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

// @ExtendWith(MockitoExtension.class)
// class CanceledOrderUseCaseImplTest {

//   // Mô phỏng (mock) repository để kiểm soát hành vi lưu order
//   @Mock
//   OrderRepositoryPort repository;

//   // Mô phỏng (mock) đối tượng Order để kiểm tra việc gọi phương thức canceled()
//   @Mock
//   Order order;

//   // Inject các mock vào implementation của use case để kiểm tra tương tác giữa use case và các dependency
//   @InjectMocks
//   CanceledOrderUseCaseImpl useCase;

//   @Test
//   void canceled_should_call_order_canceled_and_save() {
//     // Gọi phương thức canceled của useCase với order mock
//     useCase.canceled(order);

//     // Kiểm tra rằng order.canceled() đã được gọi
//     verify(order).canceled();

//     // Kiểm tra rằng repository.save(order) đã được gọi để lưu trạng thái sau khi hủy
//     verify(repository).save(order);

//     // Đảm bảo không có tương tác khác ngoài những gì đã kiểm tra
//     verifyNoMoreInteractions(order, repository);
//   }

//   @Test
//   void canceled_should_invoke_cancel_before_save() {
//     // Gọi phương thức canceled của useCase
//     useCase.canceled(order);

//     // Tạo InOrder để kiểm tra thứ tự gọi giữa order và repository
//     InOrder inOrder = inOrder(order, repository);

//     // Xác nhận rằng order.canceled() được gọi trước
//     inOrder.verify(order).canceled();

//     // Xác nhận rằng repository.save(order) được gọi sau khi order đã bị hủy
//     inOrder.verify(repository).save(order);
//   }
// }
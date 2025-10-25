// package com.example.demo.application.usecases;

// import com.example.demo.application.dto.response.TrackOrderResponse;
// import com.example.demo.application.mapper.OrderMapper;
// import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
// import com.example.demo.domain.entity.Order;
// import com.example.demo.domain.entity.OrderItem; // Cần import OrderItem để tạo Order
// import com.example.demo.domain.valueobject.order.OrderId;
// import com.example.common_dtos.enums.OrderStatus;
// import com.example.demo.domain.valueobject.user.UserId;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.math.BigDecimal;
// import java.time.Instant;
// import java.util.Collections;
// import java.util.List;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.anyList;
// import static org.mockito.Mockito.*;

// // Kích hoạt Mockito cho JUnit 5
// @ExtendWith(MockitoExtension.class)
// class OrderServiceTest {

//     // Giả định GetAllOrdersUseCaseImpl là lớp thực thi của service
//     @InjectMocks
//     GetAllOrdersUseCaseImpl orderService;

//     // Giả lập (Mock) Interface Repository
//     @Mock
//     OrderRepositoryPort orderRepository;

//     // Giả lập (Mock) Interface Mapper
//     @Mock
//     OrderMapper orderMapper;

//     @Test
//     void whenGetAllOrders_shouldReturnMappedDTOs() {
//         // 1. ARRANGE (Sắp xếp/Chuẩn bị): Tạo dữ liệu giả lập

//         // 1.1 Tạo đối tượng Order Entity (Sử dụng constructor tái tạo từ DB)
//         OrderId orderId = new OrderId(1L);
//         UserId userId = new UserId(2L);
//         BigDecimal amount = new BigDecimal("1000.00");
//         OrderStatus status = OrderStatus.PENDING;
//         Instant createdAt = Instant.now();
//         List<OrderItem> emptyItems = Collections.emptyList();
//         boolean hasBeenRated = false;
        
//         Order mockOrder = new Order(
//                 orderId, 
//                 userId, 
//                 amount, 
//                 status, 
//                 createdAt, 
//                 emptyItems, 
//                 hasBeenRated
//         ); 
        
//         // 1.2 Tạo đối tượng DTO giả lập
//         TrackOrderResponse mockDTO = new TrackOrderResponse();
//         mockDTO.setId(1L);
//         // Lưu ý: userId trong DTO phải khớp với expected (2L)
//         mockDTO.setUserId(2L); 
//         // mockDTO.setId(2L); // Dòng này bị dư và sai logic, đã xóa
//         // mockDTO.setUserId(4L); // Dòng này bị dư và sai logic, đã xóa

//         List<Order> entityList = List.of(mockOrder);
//         List<TrackOrderResponse> dtoList = List.of(mockDTO);

//         // 2. MOCKING (Giả lập hành vi): Định nghĩa hành vi của các Ports
        
//         // Khi Use Case gọi orderRepository.findAll(), trả về entityList
//         when(orderRepository.findAll()).thenReturn(entityList);
        
//         // Khi Use Case gọi orderMapper.toDTOList(entityList), trả về dtoList
//         // Lệnh anyList() được dùng để chấp nhận mọi List<Order>
//         when(orderMapper.toOrderDTOList(anyList())).thenReturn(dtoList);
        
//         // 3. ACT (Thực thi): Gọi phương thức cần kiểm thử
//         List<TrackOrderResponse> result = orderService.getAllOrders();

//         // 4. ASSERT (Xác minh): Kiểm tra kết quả và xác minh các Mock đã được gọi
        
//         // Xác minh kết quả trả về đúng số lượng và dữ liệu
//         assertNotNull(result, "Kết quả trả về không được là null.");
//         assertFalse(result.isEmpty(), "Kết quả trả về không được là rỗng.");
//         assertEquals(1, result.size(), "Số lượng DTO trả về phải là 1.");
//         assertEquals(2L, result.get(0).getUserId(), "UserId trong DTO phải khớp với dữ liệu giả lập.");

//         // Xác minh rằng các Ports đã được gọi chính xác và chỉ một lần
//         verify(orderRepository, times(1)).findAll();
//         verify(orderMapper, times(1)).toOrderDTOList(entityList); 
//     }
// }
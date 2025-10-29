package com.example.demo.application.usecases;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.dto.command.AddItemsCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.RestaurantId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for AddItemsToOrderUseCaseImpl.
 * - Uses Mockito to mock ports/mapper and domain objects.
 * - Creates a small TestNewItem stub to satisfy runtime calls to getProductId/getQuantity.
 */
@ExtendWith(MockitoExtension.class)
class AddItemsToOrderUseCaseImplTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private RestaurantDataProviderPort restaurantDataProviderPort;

    @InjectMocks
    private AddItemsToOrderUseCaseImpl useCase;

    private final Long userId = 999L;
    private final Long orderId = 1L;

    @BeforeEach
    void setup() {
        // InjectMocks + MockitoExtension handles initialization
    }

    // Simple stub that matches the shape expected by the use case at runtime:
    // it must have getProductId() and getQuantity() methods.
    static class TestNewItem {
        private final Long productId;
        private final Integer quantity;

        TestNewItem(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }
    }

@Test
void addItems_successfulAddsAndReturnsDto() {
        // Mô tả test: kiểm tra kịch bản thành công khi thêm item vào đơn hàng
        AddItemsCommand command = org.mockito.Mockito.mock(AddItemsCommand.class);
        // Use the real CreateOrderItemCommand type to avoid ClassCastException at runtime
        com.example.demo.application.dto.command.CreateOrderItemCommand newItem =
                org.mockito.Mockito.mock(com.example.demo.application.dto.command.CreateOrderItemCommand.class);
        when(newItem.getProductId()).thenReturn(101L);
        when(newItem.getQuantity()).thenReturn(2);

        when(command.getOrderId()).thenReturn(orderId);
        when(command.getNewItems()).thenReturn((List) List.of(newItem));

        // Giả lập kiểm tra quyền: có order tồn tại thuộc user
        Order existingOrder = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.findByIdAndUserId(any(), any())).thenReturn(existingOrder);

        // Lấy thông tin order thực tế để đọc restaurantId
        Order order = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.findById(any())).thenReturn(order);

        // Giả lập restaurantId trả về từ order
        RestaurantId restId = org.mockito.Mockito.mock(RestaurantId.class);
        when(order.getRestaurantId()).thenReturn(restId);
        when(restId.value()).thenReturn(55L);

        // Giả lập response từ service bên ngoài trả về thông tin menu item và giá
        ItemValidationResponse ivr = org.mockito.Mockito.mock(ItemValidationResponse.class);
        when(ivr.getMenuItemId()).thenReturn(101L);
        when(ivr.getPrice()).thenReturn(java.math.BigDecimal.valueOf(10.5));

        // Khi validateOrderCreation được gọi với restaurantId 55, trả về danh sách gồm ivr ở trên
        when(restaurantDataProviderPort.validateOrderCreation(eq(55L), anyList()))
                .thenReturn((List) List.of(ivr));

        // Giả lập lưu order và mapping sang DTO
        Order savedOrder = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.save(order)).thenReturn(savedOrder);

        TrackOrderResponse dto = org.mockito.Mockito.mock(TrackOrderResponse.class);
        when(orderMapper.toOrderDTO(savedOrder)).thenReturn(dto);

        // Thực thi use case
        TrackOrderResponse result = useCase.addItems(command, userId);

        // Kiểm tra kết quả: DTO trả về đúng đối tượng đã map
        assertSame(dto, result);
        // Xác nhận rằng order.addItem(...) đã được gọi với danh sách item mới
        verify(order).addItem(anyList());
        // Xác nhận rằng order được lưu và mapper được sử dụng
        verify(orderRepositoryPort).save(order);
        verify(orderMapper).toOrderDTO(savedOrder);
}

@Test
void addItems_whenUserHasNoPermission_throwsDomainException() {
        // Chuẩn bị command và mô phỏng rằng user không có quyền truy cập order
        AddItemsCommand command = org.mockito.Mockito.mock(AddItemsCommand.class);
        when(command.getOrderId()).thenReturn(orderId);
        // findByIdAndUserId trả về null => không tìm thấy order thuộc về user => không có quyền
        when(orderRepositoryPort.findByIdAndUserId(any(), any())).thenReturn(null);

        // Act & Assert: gọi use case sẽ ném OrderDomainException do thiếu quyền
        assertThrows(Order.OrderDomainException.class, () -> useCase.addItems(command, userId));
}

@Test
void addItems_whenVerifiedItemMissingInCommand_throwsDomainException() {
        // Tạo command chứa item có productId 300
        AddItemsCommand command = org.mockito.Mockito.mock(AddItemsCommand.class);
        // Use the real CreateOrderItemCommand type so there's no ClassCastException
        com.example.demo.application.dto.command.CreateOrderItemCommand cmdItem =
                org.mockito.Mockito.mock(com.example.demo.application.dto.command.CreateOrderItemCommand.class);

        when(cmdItem.getProductId()).thenReturn(300L);
        when(cmdItem.getQuantity()).thenReturn(1);

        when(command.getOrderId()).thenReturn(orderId);
        when(command.getNewItems()).thenReturn((List) List.of(cmdItem));

        // Giả lập kiểm tra quyền: order tồn tại nhưng thuộc user
        Order existingOrder = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.findByIdAndUserId(any(), any())).thenReturn(existingOrder);

        // Lấy order thực tế để đọc restaurantId
        Order order = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.findById(any())).thenReturn(order);

        // Giả lập restaurantId của order
        RestaurantId restId = org.mockito.Mockito.mock(RestaurantId.class);
        when(order.getRestaurantId()).thenReturn(restId);
        when(restId.value()).thenReturn(55L);

        // External service trả về danh sách verified items chỉ chứa menuItemId = 400
        ItemValidationResponse ivr = org.mockito.Mockito.mock(ItemValidationResponse.class);
        when(ivr.getMenuItemId()).thenReturn(400L); // không có trong command -> thiếu quantity tương ứng
        lenient().when(ivr.getPrice()).thenReturn(java.math.BigDecimal.valueOf(5.0));

        // Khi gọi validateOrderCreation với restaurantId 55, trả về danh sách chỉ có ivr
        when(restaurantDataProviderPort.validateOrderCreation(eq(55L), anyList()))
                .thenReturn((List) List.of(ivr));

        // Act & Assert: vì verified item không khớp với item trong command nên domain sẽ ném ngoại lệ
        assertThrows(Order.OrderDomainException.class, () -> useCase.addItems(command, userId));
}

@Test
void addItems_whenQuantityInvalidZero_throwsDomainException() {
        // Tạo command chứa item với quantity không hợp lệ (0)
        AddItemsCommand command = org.mockito.Mockito.mock(AddItemsCommand.class);
        // Use CreateOrderItemCommand mock to avoid ClassCastException
        com.example.demo.application.dto.command.CreateOrderItemCommand cmdItem =
                org.mockito.Mockito.mock(com.example.demo.application.dto.command.CreateOrderItemCommand.class);
        when(cmdItem.getProductId()).thenReturn(101L);
        when(cmdItem.getQuantity()).thenReturn(0); // số lượng bằng 0 là không hợp lệ

        when(command.getOrderId()).thenReturn(orderId);
        when(command.getNewItems()).thenReturn((List) List.of(cmdItem));

        // Giả lập kiểm tra quyền: order tồn tại cho user
        Order existingOrder = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.findByIdAndUserId(any(), any())).thenReturn(existingOrder);

        // Lấy order để truy xuất restaurantId
        Order order = org.mockito.Mockito.mock(Order.class);
        when(orderRepositoryPort.findById(any())).thenReturn(order);

        // Giả lập restaurantId và giá của item từ dịch vụ external
        RestaurantId restId = org.mockito.Mockito.mock(RestaurantId.class);
        when(order.getRestaurantId()).thenReturn(restId);
        when(restId.value()).thenReturn(55L);

        ItemValidationResponse ivr = org.mockito.Mockito.mock(ItemValidationResponse.class);
        when(ivr.getMenuItemId()).thenReturn(101L);
        lenient().when(ivr.getPrice()).thenReturn(java.math.BigDecimal.valueOf(7.0));

        // External service trả về thông tin item hợp lệ (về giá và id)
        when(restaurantDataProviderPort.validateOrderCreation(eq(55L), anyList()))
                .thenReturn((List) List.of(ivr));

        // Act & Assert: vì số lượng trong command là 0 (không hợp lệ) nên domain sẽ ném ngoại lệ
        assertThrows(Order.OrderDomainException.class, () -> useCase.addItems(command, userId));
}
}
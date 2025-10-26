package com.example.demo.application.usecases;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.dto.command.AddItemsCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.ports.input.AddItemsUseCase;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort; // <-- ĐÃ THÊM
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.product.ProductId;
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddItemsToOrderUseCaseImpl implements AddItemsUseCase {
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderMapper orderMapper;
    private final RestaurantDataProviderPort restaurantDataProviderPort; // <-- Inject Port

    @Transactional
    public TrackOrderResponse addItems(AddItemsCommand command, Long userId) {
        
        // 0. Kiểm tra user có quyền với Order này không
        Order existingOrder = orderRepositoryPort.findByIdAndUserId(
            new OrderId(command.getOrderId()), 
            new UserId(userId)
        );
        if(existingOrder == null) {
            throw new Order.OrderDomainException("User does not have permission to modify this order.");
        }

        // 1. Tải Order Aggregate Root
        Order order = orderRepositoryPort.findById(new OrderId(command.getOrderId()));
        
        // 2. Lấy danh sách ID sản phẩm mới cần xác minh
        List<Long> newProductIds = command.getNewItems().stream()
                .map(item -> item.getProductId())
                .collect(Collectors.toList());
        
        // 3. Lấy giá tương ứng mỗi sản phẩm
        List<ItemValidationResponse> verifiedData = restaurantDataProviderPort.validateOrderCreation(
            order.getRestaurantId().value(), 
            newProductIds
        );
        
        // 4. Ánh xạ Quantity từ Command sang Map để tra cứu nhanh (trả về số lượng tương ứng mỗi sản phẩm)
        Map<Long, Integer> commandQuantityMap = command.getNewItems().stream()
            .collect(Collectors.toMap(
                cmdItem -> cmdItem.getProductId(),
                cmdItem -> cmdItem.getQuantity()
            ));

        // 5. TẠO OrderItem Entity HỢP LỆ bằng cách gộp Price (từ External) và Quantity (từ Command)
        List<OrderItem> newItems = verifiedData.stream()
            .map(verifiedItem -> { 
                Long currentProductId = verifiedItem.getMenuItemId();
                Integer quantity = commandQuantityMap.get(currentProductId);
                
                if (quantity == null || quantity <= 0) {
                    throw new Order.OrderDomainException("Missing or invalid quantity for product ID: " + currentProductId);
                }
                
                return OrderItem.createNew(
                    new ProductId(currentProductId), 
                    quantity, 
                    verifiedItem.getPrice() 
                ); 
            }).collect(Collectors.toList());
        
        order.addItem(newItems);
        
        Order savedOrder = orderRepositoryPort.save(order);
        return orderMapper.toOrderDTO(savedOrder);
    }
}
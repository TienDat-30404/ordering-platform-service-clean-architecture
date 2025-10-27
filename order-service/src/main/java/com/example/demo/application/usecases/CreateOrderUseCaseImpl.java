package com.example.demo.application.usecases;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map; // Cần import Map
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.orchestrator.OrderOrchestratorService;
import com.example.demo.application.ports.input.CreateOrderUseCase;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.product.ProductId; // Cần import ProductId
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
        private final OrderRepositoryPort orderRepositoryPort;
        private final OrderMapper orderMapper;
        private final RestaurantDataProviderPort restaurantDataProviderPort;
        private final OrderOrchestratorService orderOrchestratorService;

        @Transactional
        public TrackOrderResponse createOrder(CreateOrderCommand command, Long userId) {

                // 1. lấy tất cả productIds của đơn hàng cần tạo
                List<Long> productIds = command.getItems().stream()
                                .map(item -> item.getProductId())
                                .collect(Collectors.toList());

                // 2. GỌI EXTERNAL PORT: Lấy danh sách VerifiedProductData (có giá chính thức)
                List<ItemValidationResponse> verifiedData = restaurantDataProviderPort.validateOrderCreation(
                                command.getRestaurantId(),
                                productIds);

                Map<Long, Integer> commandQuantityMap = command.getItems().stream()
                                .collect(Collectors.toMap(
                                                cmdItem -> cmdItem.getProductId(), // Key: Long productId
                                                cmdItem -> cmdItem.getQuantity() // Value: int quantity
                                ));

                // 3. Xây dựng Final Order Items: Kết hợp Price từ VerifiedData và Quantity từ
                // Command Map
                List<OrderItem> finalOrderItems = verifiedData.stream()
                                .map(verifiedItem -> {
                                        Long currentProductId = verifiedItem.getMenuItemId();

                                        // Tra cứu Quantity từ Map
                                        Integer quantity = commandQuantityMap.get(currentProductId);

                                        // Kiểm tra sự tồn tại và tính hợp lệ của quantity (Domain Rule: quantity > 0)
                                        if (quantity == null || quantity <= 0) {
                                                throw new Order.OrderDomainException(
                                                                "Missing or invalid quantity for product ID: "
                                                                                + currentProductId);
                                        }

                                        // TẠO OrderItem Domain Entity HỢP LỆ
                                        return OrderItem.createNew(
                                                        new ProductId(currentProductId),
                                                        quantity,
                                                        verifiedItem.getPrice());
                                }).collect(Collectors.toList());
                // 4. Tạo Order Aggregate Root
                Order order = new Order(
                                new UserId(userId),
                                finalOrderItems,
                                new RestaurantId(command.getRestaurantId()));

                // 5. Lưu Order Aggregate
                // Order savedOrder = orderRepositoryPort.save(order);

                // List<Map<String, Object>> itemsPayload =
                // orderMapper.toItemsPayload(savedOrder.getItems());
                // orderOrchestratorService.startCreateOrderSaga(
                // savedOrder.getId().value().toString(),
                // savedOrder.getRestaurantId().value().toString(),
                // itemsPayload
                // );

                Order savedOrder = orderRepositoryPort.save(order);

                BigDecimal totalAmountToPay = new BigDecimal("1000000.00");

                orderOrchestratorService.startCreateOrderSaga(
                                savedOrder.getId().value(),
                                totalAmountToPay,
                                savedOrder.getUserId().value());

                // 6. Trả về Response
                TrackOrderResponse response = orderMapper.toOrderDTO(savedOrder);
                return response;
        }
}

// @Service
// @RequiredArgsConstructor
// public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
// private final OrderRepositoryPort orderRepositoryPort;
// private final OrderMapper orderMapper;
// // Bỏ restaurantDataProviderPort vì ta không gọi đồng bộ nữa.
// private final OrderOrchestratorService orderOrchestratorService; // Thêm
// Orchestrator

// @Transactional
// public TrackOrderResponse createOrder(CreateOrderCommand command, Long
// userId) {

// Order initialOrder = new Order(
// new UserId(userId),
// orderMapper.toOrderItems(command.getItems()), // Giả định có mapper chuyển
// đổi
// new RestaurantId(command.getRestaurantId()));

// Order savedOrder = orderRepositoryPort.save(initialOrder);
// List<Map<String, Object>> itemsPayload =
// orderMapper.toItemsPayload(savedOrder.getItems());

// orderOrchestratorService.startCreateOrderSaga(
// savedOrder.getId().toString(),
// savedOrder.getRestaurantId().toString(),
// itemsPayload
// );

// // 4. Trả về Response
// TrackOrderResponse response = orderMapper.toOrderDTO(savedOrder);
// return response;
// }
// }
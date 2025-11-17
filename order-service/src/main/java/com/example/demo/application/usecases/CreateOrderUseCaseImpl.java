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
import com.example.demo.application.ports.output.monitoring.OrderMonitoringPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.product.ProductId; // Cần import ProductId
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
// @RequiredArgsConstructor
public class CreateOrderUseCaseImpl implements CreateOrderUseCase {
        private final OrderRepositoryPort orderRepositoryPort;
        private final OrderMapper orderMapper;
        private final RestaurantDataProviderPort restaurantDataProviderPort;
        private final OrderOrchestratorService orderOrchestratorService;
        private final OrderMonitoringPort orderMonitoringPort;

        public CreateOrderUseCaseImpl(
                        OrderRepositoryPort orderRepositoryPort,
                        OrderMapper orderMapper,
                        RestaurantDataProviderPort restaurantDataProviderPort,
                        OrderOrchestratorService orderOrchestratorService,
                        OrderMonitoringPort orderMonitoringPort) {
                this.orderRepositoryPort = orderRepositoryPort;
                this.orderMapper = orderMapper;
                this.restaurantDataProviderPort = restaurantDataProviderPort;
                this.orderOrchestratorService = orderOrchestratorService;
                this.orderMonitoringPort = orderMonitoringPort;
        }

        @Transactional
        public TrackOrderResponse createOrder(CreateOrderCommand command, Long userId) {
                try {

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

                        List<OrderItem> finalOrderItems = verifiedData.stream()
                                        .map(verifiedItem -> {
                                                Long currentProductId = verifiedItem.getMenuItemId();

                                                // Tra cứu Quantity từ Map
                                                Integer quantity = commandQuantityMap.get(currentProductId);
                                                if (quantity == null) {
                                                        throw new Order.OrderDomainException(
                                                                        "Missing quantity data for product ID: "
                                                                                        + currentProductId
                                                                                        + " in the command.");
                                                }
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

                        Order savedOrder = orderRepositoryPort.save(order);
                        List<Map<String, Object>> itemsPayload = orderMapper.toItemsPayload(savedOrder.getItems());
                        orderOrchestratorService.startCreateOrderSaga(
                                        savedOrder.getId().value().toString(),
                                        savedOrder.getRestaurantId().value().toString(),
                                        itemsPayload);

                        TrackOrderResponse response = orderMapper.toOrderDTO(savedOrder);
                        return response;
                } catch (Order.OrderDomainException e) {
                        orderMonitoringPort.recordOrderCreation("FAILED");
                        throw new RuntimeException("System error  during OrderDomainException.", e);
                } catch (Exception e) {
                        orderMonitoringPort.recordOrderCreation("FAILED");
                        throw new RuntimeException("System error during Exception.", e);
                }
        }
}

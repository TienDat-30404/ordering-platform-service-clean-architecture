package com.example.demo.adapters.in.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.application.dto.command.AddItemsCommand;
import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.dto.output.OrderItemResponse;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.product.ProductId;
import com.example.demo.domain.valueobject.user.UserId;

@Component
public class OrderMapperImpl implements OrderMapper {

    // DTO (Create Order) -> domain entity ( order )
    // public Order toOrderEntity(CreateOrderCommand command) {
    // UserId userId = new UserId(command.getUserId());
    // List<OrderItem> orderItems = command.getItems().stream()
    // .map(this::toOrderItemEntity)
    // .collect(Collectors.toList());
    // RestaurantId restaurantId = new RestaurantId(command.getRestaurantId());
    // return new Order(
    // userId,
    // orderItems,
    // restaurantId
    // );
    // }

    // DTO (Create Order Item ) -> domain entity ( Order Item )
    @Override
    public List<OrderItem> toOrderItems(List<CreateOrderItemCommand> orderItems) {
        return orderItems.stream()
                .map(this::toOrderItem) // Dùng phương thức helper bên dưới
                .collect(Collectors.toList());
    }

    private OrderItem toOrderItem(CreateOrderItemCommand itemCommand) {
        ProductId productId = new ProductId(itemCommand.getProductId());
        OrderItem item = OrderItem.createNew(productId, itemCommand.getQuantity(), BigDecimal.ZERO);
        return item;
    }

    // DTO (Order Item) -> DTO (OrderItemResponse) - phương thức bổ sung (ko phải
    // lớp triển khai)
    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProductId().value(),
                "bt2hjr",
                item.getQuantity(),
                item.getPrice(),
                item.getTotalPrice());
    }

    // Domain Entity (Order) -> DTO (TrackOrderResponse)
    public TrackOrderResponse toOrderDTO(Order order) {
        TrackOrderResponse response = new TrackOrderResponse();
        response.setId(order.getId().value());
        response.setUserId(order.getUserId().value());
        response.setRestaurantId(order.getRestaurantId().value());
        response.setAmount(order.getAmount());
        response.setStatus(order.getStatus().getName());
        response.setCreatedAt(LocalDateTime.ofInstant(order.getCreatedAt(), ZoneId.systemDefault()));
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(items);
        return response;
    }

    // Domain Entity ( Order ) -> DTO(List<TrackOrderResponse)
    public List<TrackOrderResponse> toOrderDTOList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream()
                .map(this::toOrderDTO) // Gọi phương thức ánh xạ đơn lẻ đã có
                .collect(Collectors.toList());
    }

    public TrackOrderResponse toProductDataOfOrderDTO(Order order, Map<Long, ProductDetailData> productDetailsMap) {
        TrackOrderResponse response = new TrackOrderResponse();
        // ... set các trường Order ...

        List<OrderItemResponse> items = order.getItems().stream()
                // Gọi phương thức ánh xạ riêng cho Item, truyền kèm Map
                .map(item -> toOrderItemResponse(item, productDetailsMap))
                .collect(Collectors.toList());

        response.setItems(items);
        return response;
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item, Map<Long, ProductDetailData> productDetailsMap) {
        ProductDetailData detail = productDetailsMap.get(item.getProductId().value());

        return new OrderItemResponse(
                item.getProductId().value(),
                detail != null ? detail.getName() : "Unknown Product",
                item.getQuantity(),
                item.getPrice(),
                item.getTotalPrice());
    }

    public List<Map<String, Object>> toItemsPayload(List<OrderItem> orderItems) {
    return orderItems.stream()
        .map(item -> {
            Map<String, Object> itemMap = new java.util.HashMap<>();
            itemMap.put("productId", item.getProductId().value());
            itemMap.put("quantity", item.getQuantity());
            return itemMap;
        })
        .collect(Collectors.toList());
}

}

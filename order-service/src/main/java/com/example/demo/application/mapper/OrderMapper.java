package com.example.demo.application.mapper;

import java.util.List;
import java.util.Map;

import com.example.demo.application.dto.command.CreateOrderItemCommand;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;

public interface OrderMapper {
    // public Order toOrderEntity(CreateOrderCommand command);
    public TrackOrderResponse toOrderDTO(Order order);

    public List<TrackOrderResponse> toOrderDTOList(List<Order> orders);

    // List<OrderItem> toListOrderItemEntity(AddItemsCommand command);
    public List<OrderItem> toOrderItems(List<CreateOrderItemCommand> orderItems);

    TrackOrderResponse toProductDataOfOrderDTO(Order order, Map<Long, ProductDetailData> productDetailsMap);

    List<Map<String, Object>> toItemsPayload(List<OrderItem> orderItems);
}

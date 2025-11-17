package com.example.demo.adapters.out.persistence.mapper;

import com.example.demo.adapters.out.persistence.entity.OrderItemJpaEntity;
import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderItem;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderItemId;
import com.example.demo.domain.valueobject.order.OrderStatus;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.product.ProductId;
import com.example.demo.domain.valueobject.user.UserId;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class OrderPersistenceMapper {

    // Domain Entity ( Order ) -> JpaEntity ( Order )
    public OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        if (order.getId() != null) {
            entity.setId(order.getId().value());
        }
        entity.setUserId(order.getUserId().value());
        entity.setAmount(order.getAmount());
        entity.setStatus(order.getStatus().name());
        entity.setCreatedAt(order.getCreatedAt());
        entity.setHasBeenRated(order.getHasBeenRated());
        entity.setRestaurantId(order.getRestaurantId().value());
        entity.setFinalPrice(order.getFinalPrice());
        List<OrderItemJpaEntity> jpaItems = order.getItems().stream()
                .map(this::toOrderItemJpaEntity) // (Phương thức này map item field)
                .collect(Collectors.toList());

        for (OrderItemJpaEntity item : jpaItems) {
            item.setOrder(entity);
        }
        entity.setItems(jpaItems);
        if(order.getVoucher() != null) {
            entity.setVoucherCode(order.getVoucher().getCode());
            entity.setDiscountAmount(order.getVoucher().getDiscountAmount());
        }
        entity.setFinalPrice(order.getFinalPrice());
    
        return entity;
    }

    // Domain Entity (Order Item ) => Jpa Eentity (Order Item )
    private OrderItemJpaEntity toOrderItemJpaEntity(OrderItem orderItem) {
        OrderItemJpaEntity itemJpaEntity = new OrderItemJpaEntity();
        if (orderItem.getId() != null) {
            itemJpaEntity.setId(orderItem.getId().value());
        }
        itemJpaEntity.setProductId(orderItem.getProductId().value());
        itemJpaEntity.setQuantity(orderItem.getQuantity());
        itemJpaEntity.setPrice(orderItem.getPrice());

        return itemJpaEntity;
    }

    // Jpa Entity ( Order Item ) -> Domain Entity ( Order Item )
    private OrderItem toOrderItemDomainEntity(OrderItemJpaEntity jpaItem) {
        OrderItemId itemId = null;
        if (jpaItem.getId() != null) {
            itemId = new OrderItemId(jpaItem.getId());
        }
        ProductId productId = new ProductId(jpaItem.getProductId());

        int quantity = jpaItem.getQuantity();
        BigDecimal price = jpaItem.getPrice();

        return new OrderItem(
                itemId,
                productId,
                quantity,
                price);
    }

    // JpaEntity (Order) -> Domain Entity ( Order )
    public Order toDomainEntity(OrderJpaEntity jpaEntity) {
        OrderId orderId = new OrderId(jpaEntity.getId());
        UserId userId = new UserId(jpaEntity.getUserId());
        BigDecimal amount = jpaEntity.getAmount();
        BigDecimal finalPrice = jpaEntity.getFinalPrice();
        OrderStatus status = OrderStatus.valueOf(jpaEntity.getStatus());
        Instant createdAt = jpaEntity.getCreatedAt();
        List<OrderItem> items = jpaEntity.getItems().stream()
                .map(this::toOrderItemDomainEntity)
                .collect(Collectors.toList());
        Boolean hasBeenRated = jpaEntity.getHasBeenRated();
        RestaurantId restaurantId = new RestaurantId(jpaEntity.getRestaurantId());
        return new Order(
                orderId,
                userId,
                amount,
                finalPrice,
                status,
                createdAt,
                items,
                hasBeenRated,
                restaurantId
                );
    }
}
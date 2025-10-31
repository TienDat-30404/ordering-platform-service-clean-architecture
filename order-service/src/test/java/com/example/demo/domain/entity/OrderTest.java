package com.example.demo.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common_dtos.enums.OrderStatus;
import com.example.demo.domain.valueobject.order.RestaurantId;
import com.example.demo.domain.valueobject.product.ProductId;
import com.example.demo.domain.valueobject.user.UserId;

class OrderTest {

    @Test
    @DisplayName("validateUserIdExists() - Should throw exception when userId is null")
    void validateUserIdExists_shouldThrow_whenUserIdIsNull() {
        // given
        List<OrderItem> items = List.of(
                new OrderItem(null, new ProductId(1L), 2, BigDecimal.valueOf(10))
        );

        // when + then
        assertThrows(Order.OrderDomainException.class, () -> {
            new Order(null, items, new RestaurantId(1L));
        });
    }

    @Test
    @DisplayName("validateItemsExist() - Should throw exception when items list is null or empty")
    void validateItemsExist_shouldThrow_whenItemsEmpty() {
        // when + then
        assertThrows(Order.OrderDomainException.class, () -> {
            new Order(new UserId(1L), List.of(), new RestaurantId(1L));
        });
    }

    @Test
    @DisplayName("calculateTotalPrice() - Should correctly calculate total amount from items")
    void calculateTotalPrice_shouldSumItemPricesCorrectly() {
        // given
        List<OrderItem> items = List.of(
                new OrderItem(null, new ProductId(1L), 2, BigDecimal.valueOf(10)),  // 20
                new OrderItem(null, new ProductId(2L), 3, BigDecimal.valueOf(5))    // 15
        );

        // when
        Order order = new Order(new UserId(1L), items, new RestaurantId(1L));

        // then
        assertEquals(BigDecimal.valueOf(35), order.getAmount());
    }

    @Test
    @DisplayName("validateOrder() - Should call internal checks and compute amount")
    void validateOrder_shouldPassWithValidData() {
        // given
        List<OrderItem> items = List.of(
                new OrderItem(null, new ProductId(1L), 2, BigDecimal.valueOf(12.5)),
                new OrderItem(null, new ProductId(2L), 1, BigDecimal.valueOf(5.0))
        );

        // when
        Order order = new Order(new UserId(999L), items, new RestaurantId(10L));

        // then
        assertNotNull(order.getUserId());
        assertEquals(2, order.getItems().size());
        assertEquals(BigDecimal.valueOf(30.0), order.getAmount());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }
}

package com.example.demo.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.demo.domain.valueobject.product.ProductId;

class OrderItemTest {

    @Test
    @DisplayName("createNew() - should create valid OrderItem when quantity > 0")
    void createNew_shouldCreateItem_whenQuantityValid() {
        // given
        ProductId productId = new ProductId(1L);
        int quantity = 3;
        BigDecimal price = BigDecimal.valueOf(10.0);

        // when
        OrderItem item = OrderItem.createNew(productId, quantity, price);

        // then
        assertNotNull(item);
        assertEquals(productId, item.getProductId());
        assertEquals(quantity, item.getQuantity());
        assertEquals(price, item.getPrice());
        assertNull(item.getId());
    }

    @Test
    @DisplayName("createNew() - should throw OrderDomainException when quantity <= 0")
    void createNew_shouldThrowException_whenQuantityInvalid() {
        // given
        ProductId productId = new ProductId(2L);

        // when + then
        Order.OrderDomainException ex = assertThrows(
                Order.OrderDomainException.class,
                () -> OrderItem.createNew(productId, 0, BigDecimal.valueOf(10))
        );

        assertTrue(ex.getMessage().contains("Quantity must be greater than zero for product ID nhó:"));
    }
}

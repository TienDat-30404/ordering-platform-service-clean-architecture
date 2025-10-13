package com.example.demo.domain.entity;

import java.math.BigDecimal;

import com.example.demo.domain.valueobject.order.OrderItemId;
import com.example.demo.domain.valueobject.product.ProductId;

public class OrderItem {

    private OrderItemId id;
    private final ProductId productId;
    private int quantity;
    private BigDecimal price;
    private Order order;

    public OrderItem(OrderItemId id, ProductId productId, int quantity, BigDecimal price) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItem createNew(ProductId productId, int quantity, BigDecimal price) {
        return new OrderItem(null, productId, quantity, price); 
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public ProductId getProductId() {
        return productId;
    }


    public OrderItemId getId() {
        return id;
    }


    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    // Nếu cần update số lượng hoặc giá
    public void updateQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("than zero");
        }
        this.quantity = newQuantity;
    }


    public void updatePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero");
        }
        this.price = newPrice;
    }

    public String toString() {
        return "OrderItem{" +
                "productId=" + productId +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}

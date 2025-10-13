package com.example.demo.domain.entity;

import java.math.BigDecimal;

import com.example.demo.domain.valueobject.product.ProductId;

public class Product {
    private ProductId id;
    private String name;
    private BigDecimal price;

    public Product(ProductId id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public ProductId getProductId() {
        return id;
    }
    public String name() {
        return name;
    }
    public BigDecimal price() {
        return price;
    }

}

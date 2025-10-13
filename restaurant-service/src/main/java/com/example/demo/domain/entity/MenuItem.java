package com.example.demo.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.domain.valueobject.MenuItemId;
import com.example.demo.domain.valueobject.RestaurantId;

public class MenuItem {
    private MenuItemId id;
    private RestaurantId restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean available;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public MenuItem() {}

    public MenuItem(MenuItemId id, RestaurantId restaurantId, String name, String description, BigDecimal price, String category, Boolean available, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.available = available;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        // validatePrice(); 
    }

    public MenuItem(MenuItemId id, String name, BigDecimal price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    public MenuItemId getId() {
        return id;
    }

    public RestaurantId getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean getAvailable() {
        return available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void validatePrice() {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
    }
    
    public void makeUnavailable() {
        this.available = false;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void makeAvailable() {
        this.available = true;
        this.updatedAt = LocalDateTime.now();
    }

     @Override
    public String toString() {
        return "Restaurant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                '}';
    }
}
package com.example.demo.adapters.out.cache;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.domain.valueobject.MenuItemId;
import com.example.demo.domain.valueobject.RestaurantId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class MenuItemCaching {
    
    @JsonCreator
    public MenuItemCaching(
        @JsonProperty("id") MenuItemId id,
        @JsonProperty("restaurantId") RestaurantId restaurantId, 
        @JsonProperty("name") String name, 
        @JsonProperty("description") String description, 
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("category") String category, 
        @JsonProperty("available") Boolean available,
        @JsonProperty("createdAt") LocalDateTime createdAt,
        @JsonProperty("updatedAt") LocalDateTime updatedAt) {
    }
}
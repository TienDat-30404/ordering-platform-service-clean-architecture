package com.example.demo.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.domain.valueobject.RestaurantId;
import com.example.demo.domain.valueobject.RestaurantStatus;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Restaurant {
    private RestaurantId id;
    private String name;
    private String address;
    private String phone;
    private RestaurantStatus status;
    private BigDecimal rating;
    private Integer totalRatings;

    private List<MenuItem> menu;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonCreator
    public Restaurant(@JsonProperty("id") RestaurantId id,
            @JsonProperty("name") String name,
            @JsonProperty("address") String address,
            @JsonProperty("phone") String phone,
            @JsonProperty("status") RestaurantStatus status,
            @JsonProperty("rating") BigDecimal rating,
            @JsonProperty("totalRatings") Integer totalRatings,
            @JsonProperty("menu") List<MenuItem> menu,
            @JsonProperty("createdAt") LocalDateTime createdAt, // 👈 Bao gồm cả 2 trường này
            @JsonProperty("updatedAt") LocalDateTime updatedAt
            ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.status = status;
        this.rating = rating;
        this.totalRatings = totalRatings;
        this.menu = menu;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public RestaurantId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getPhone() {
        return phone;
    }

    public RestaurantStatus getStatus() {
        return status;
    }

    public BigDecimal getRating() {
        return rating;
    };

    public Integer getTotalRatings() {
        return totalRatings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void validateForOrderApproval() {
        if (status != RestaurantStatus.ACTIVE) {
            throw new IllegalStateException("Restaurant is not active");
        }
    }

    public List<MenuItem> getMenu() {
        return menu;
    }

    public void validateMenuItems(List<Long> menuItemIds) {
        for (Long itemId : menuItemIds) {
            MenuItem item = menu.stream()
                    .filter(mi -> mi.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Menu item not found: " + itemId));

            if (!item.getAvailable()) {
                throw new IllegalArgumentException(
                        "Menu item not available: " + item.getName());
            }
        }
    }

    public void activate() {
        this.status = RestaurantStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.status = RestaurantStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Restaurant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", menu=" + menu +
                '}';
    }

}

package com.example.demo.adapters.out.persistence.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.adapters.out.persistence.entity.MenuItemJpaEntity;
import com.example.demo.adapters.out.persistence.entity.RestaurantJpaEntity;
import com.example.demo.domain.entity.MenuItem;
import com.example.demo.domain.entity.Restaurant;
import com.example.demo.domain.valueobject.MenuItemId;
import com.example.demo.domain.valueobject.RestaurantId;
import com.example.demo.domain.valueobject.RestaurantStatus;

@Component
public class RestaurantPersistenceMapper {

    public RestaurantJpaEntity toRestaurantJpaEntity(Restaurant restaurant) {
        RestaurantJpaEntity jpaEntity = new RestaurantJpaEntity();
        if (restaurant.getId() != null) {
            jpaEntity.setId(restaurant.getId().value());
        }
        jpaEntity.setAddress(restaurant.getAddress());
        jpaEntity.setCreatedAt(restaurant.getCreatedAt());
        jpaEntity.setName(restaurant.getName());
        jpaEntity.setPhone(restaurant.getPhone());
        jpaEntity.setRating(restaurant.getRating());
        jpaEntity.setTotalRatings(restaurant.getTotalRatings());
        jpaEntity.setUpdatedAt(restaurant.getUpdatedAt());
        List<MenuItemJpaEntity> menuItems = restaurant.getMenu().stream()
                .map(this::toMenuItemJpaEntity)
                .collect(Collectors.toList());
        for (MenuItemJpaEntity item : menuItems) {
            item.setRestaurant(jpaEntity);
        }
        jpaEntity.setMenu(menuItems);
        return jpaEntity;
    }

    public MenuItemJpaEntity toMenuItemJpaEntity(MenuItem menuItem) {
        MenuItemJpaEntity menuItemJpaEntity = new MenuItemJpaEntity();
        if (menuItem.getId() != null) {
            menuItemJpaEntity.setId(menuItem.getId().value());
        }
        menuItemJpaEntity.setAvailable(menuItem.getAvailable());
        menuItemJpaEntity.setCategory(menuItem.getCategory());
        menuItemJpaEntity.setName(menuItem.getName());
        menuItemJpaEntity.setPrice(menuItem.getPrice());
        return menuItemJpaEntity;
    }

    public Restaurant toDomainRestaurant(RestaurantJpaEntity jpaEntity) {
        if (jpaEntity == null) return null;
        RestaurantStatus status = RestaurantStatus.valueOf(jpaEntity.getStatus());
        
        List<MenuItem> items = jpaEntity.getMenu().stream()
                .map(this::toDomainMenuItem)
                .collect(Collectors.toList());

        return new Restaurant(
            new RestaurantId(jpaEntity.getId()),
            jpaEntity.getName(),
            jpaEntity.getAddress(), 
            jpaEntity.getPhone(),
            status, 
            jpaEntity.getRating(), 
            jpaEntity.getTotalRatings(), 
            items);
     }

    public MenuItem toDomainMenuItem(MenuItemJpaEntity entity) {
        return new MenuItem(
            new MenuItemId(entity.getId()),
            new RestaurantId(entity.getRestaurant().getId()),
            entity.getName(),
            entity.getDescription(),
            entity.getPrice(),
            entity.getCategory(),
            entity.getAvailable(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}

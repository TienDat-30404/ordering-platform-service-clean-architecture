package com.example.demo.adapters.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.adapters.out.persistence.entity.MenuItemJpaEntity;
import com.example.demo.domain.entity.MenuItem;
import com.example.demo.domain.valueobject.MenuItemId;

@Component
public class MenuItemPersistenceMapper {
    
    public MenuItem toDomainEntity(MenuItemJpaEntity menuItemJpaEntity) {
        MenuItemId id = new MenuItemId(menuItemJpaEntity.getId());
        return new MenuItem(
            id,
            menuItemJpaEntity.getName(),
            menuItemJpaEntity.getPrice()
        );
    }
}

package com.example.demo.adapters.in.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.common_dtos.dto.MenuItemResponse;
import com.example.demo.application.mapper.MenuItemMapper;
import com.example.demo.domain.entity.MenuItem;

@Component
public class MenuItemMapperImpl implements MenuItemMapper {

    public MenuItemResponse toMenuItemDTO(MenuItem menuItem) {
        MenuItemResponse dto = new MenuItemResponse();
        dto.setId(menuItem.getId().value());
        dto.setName(menuItem.getName());
        dto.setPrice(menuItem.getPrice());
        return dto;
    }


    public List<MenuItemResponse> toListMenuItemDTO(List<MenuItem> menuItems) {
        if (menuItems == null) {
            return List.of();
        }
        return menuItems.stream().map(this::toMenuItemDTO)
            .collect(Collectors.toList());
    }
   
}

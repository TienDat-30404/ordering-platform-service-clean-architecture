package com.example.demo.application.mapper;

import java.util.List;

import com.example.common_dtos.dto.MenuItemResponse;
import com.example.demo.domain.entity.MenuItem;

public interface MenuItemMapper {
    List<MenuItemResponse> toListMenuItemDTO(List<MenuItem> menuItems);
}

package com.example.demo.application.ports.input;

import java.util.List;

import com.example.common_dtos.dto.MenuItemResponse;


public interface GetMenuItemsUseCase {
    List<MenuItemResponse> getAllMenuItemsByIds(List<Long> menuItemIds);
}

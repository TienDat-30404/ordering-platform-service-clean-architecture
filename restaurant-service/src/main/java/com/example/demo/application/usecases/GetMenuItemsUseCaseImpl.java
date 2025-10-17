package com.example.demo.application.usecases;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.common_dtos.dto.MenuItemResponse;
import com.example.demo.application.mapper.MenuItemMapper;
import com.example.demo.application.ports.input.GetMenuItemsUseCase;
import com.example.demo.application.ports.output.MenuItemRepositoryPort;
import com.example.demo.domain.entity.MenuItem;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMenuItemsUseCaseImpl implements GetMenuItemsUseCase {
    private final MenuItemRepositoryPort menuItemRepositoryPort;
    private final MenuItemMapper mapper;


  
    public List<MenuItemResponse> getAllMenuItemsByIds(List<Long> menuItemIds) {
        System.out.println("Toi la usecase get menuitem ");
        List<MenuItem> menuItems = menuItemRepositoryPort.findAllById(menuItemIds);
        System.out.println("menuItem" + menuItems);
        return mapper.toListMenuItemDTO(menuItems);
    }

}

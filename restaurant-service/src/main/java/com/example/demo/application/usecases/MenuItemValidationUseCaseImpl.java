package com.example.demo.application.usecases;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.ports.input.ValidateMenuItemUseCase;
import com.example.demo.application.ports.output.RestaurantRepositoryPort;
import com.example.demo.domain.entity.MenuItem;
import com.example.demo.domain.entity.Restaurant;
import com.example.demo.domain.valueobject.MenuItemId;
import com.example.demo.domain.valueobject.RestaurantId;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@RequiredArgsConstructor
@Slf4j 
public class MenuItemValidationUseCaseImpl implements ValidateMenuItemUseCase {
    
    private final RestaurantRepositoryPort restaurantRepository;

    @Override
    public List<ItemValidationResponse> validateItems(ItemValidationRequest request) {
        List<ItemValidationResponse> responses = new ArrayList<>();
        
        // 1. Load Restaurant Aggregate
        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(new RestaurantId(request.getRestaurantId()));
        if (restaurantOpt.isEmpty()) {
            log.warn("Restaurant ID {} not found during item validation.", request.getRestaurantId());
            for (Long id : request.getMenuItemIds()) {
                responses.add(new ItemValidationResponse(id, false, null, "Restaurant not found."));
            }
            return responses;
        }

        Restaurant restaurant = restaurantOpt.get();
        
        // 2. Tối ưu hóa: Chuyển List thành Map. (O(N) initial cost)
        // Việc này cho phép tra cứu (lookup) món ăn sau này chỉ mất O(1).
        Map<MenuItemId, MenuItem> menuMap = restaurant.getMenu().stream()
            .collect(Collectors.toMap(MenuItem::getId, Function.identity()));
        
        System.out.println("menuMapppp" + menuMap);
        // 3. Duyệt qua từng item và kiểm tra trạng thái
        for (Long itemId : request.getMenuItemIds()) {
            MenuItemId menuItemId = new MenuItemId(itemId); 
            MenuItem item = menuMap.get(menuItemId); // Tra cứu O(1)

            if (item == null) {
                responses.add(new ItemValidationResponse(itemId, false, null, "Menu item not found in restaurant menu."));
            } else {
            
                if (!item.getAvailable()) { 
                    responses.add(new ItemValidationResponse(itemId, false, null, "Menu item is currently not available."));
                } else {    
                    responses.add(new ItemValidationResponse(itemId, true, item.getPrice(), "OK"));
                }
            }
        }
        
        return responses;
    }
}




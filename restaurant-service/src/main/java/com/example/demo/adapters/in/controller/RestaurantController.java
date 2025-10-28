package com.example.demo.adapters.in.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.common_dtos.dto.MenuItemResponse;
import com.example.demo.adapters.out.persistence.entity.RestaurantJpaEntity;
import com.example.demo.adapters.out.persistence.repository.RestaurantJpaRepository;
import com.example.demo.application.ports.input.GetMenuItemsUseCase;
import com.example.demo.application.ports.input.ValidateMenuItemUseCase;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final ValidateMenuItemUseCase validationService;
    private final GetMenuItemsUseCase getMenuItemsUseCase;
    private final RestaurantJpaRepository restaurantJpaRepository;
    private final com.example.demo.application.ports.input.CheckStockUseCase checkStockUseCase;
    private final com.example.demo.application.ports.input.DeductStockUseCase deductStockUseCase;

    @PostMapping("/validate-menu-items")
    public ResponseEntity<List<ItemValidationResponse>> validateMenuItems(

            @RequestBody ItemValidationRequest request) {
        List<ItemValidationResponse> response = validationService.validateItems(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/menuItems")

    public ResponseEntity<List<MenuItemResponse>> getMenuItems(
            @RequestParam("ids") List<Long> ids) {
        List<MenuItemResponse> response = getMenuItemsUseCase.getAllMenuItemsByIds(ids);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test2")
    public String test() {
        return "Restaurant Service is up and running!";
    }

    private final RestaurantDTO convertToDto(RestaurantJpaEntity entity) {
        RestaurantDTO dto = new RestaurantDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }

    @Data
    public class RestaurantDTO {
        private Long id;
        private String name;
    }

    @GetMapping("/get-all-restaurants") // Tên RESTful chuẩn (danh từ số nhiều)
    // @Cacheable(value = "allRestaurants", key = "'all'")
    public List<RestaurantDTO> getAllRestaurants() {
        List<RestaurantDTO> dtos = restaurantJpaRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .toList();
        return dtos;
    }

    @Data
    public class MenuItemDTO {
        private Long id;
        private String name;
        private String description;
        private String price;
    }

    @GetMapping("/get-menuitems")
    public List<MenuItemDTO> getMenuItems() {
        List<MenuItemDTO> dtos = restaurantJpaRepository.findAll()
                .stream()
                .flatMap(restaurant -> restaurant.getMenu().stream())
                .map(menuItem -> {
                    MenuItemDTO dto = new MenuItemDTO();
                    dto.setId(menuItem.getId());
                    dto.setName(menuItem.getName());
                    dto.setDescription(menuItem.getDescription());
                    dto.setPrice(menuItem.getPrice().toString());
                    return dto;
                })
                .toList();
        return dtos;
    }

    @PostMapping("/stock/check")
    public ResponseEntity<List<com.example.demo.application.dto.stock.StockCheckResult>> checkStock(
            @RequestBody List<com.example.demo.application.dto.stock.StockCheckItem> items) {
        var result = checkStockUseCase.check(items);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/stock/deduct")
    public ResponseEntity<Void> deductStock(
            @RequestBody List<com.example.demo.application.dto.stock.StockCheckItem> items) {
        deductStockUseCase.deduct(items);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/test123")
    public String test3() {
        return "Hello World ";
    }

}
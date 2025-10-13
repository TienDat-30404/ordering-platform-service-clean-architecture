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
import com.example.demo.application.ports.input.GetMenuItemsUseCase;
import com.example.demo.application.ports.input.ValidateMenuItemUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final ValidateMenuItemUseCase validationService;
    private final GetMenuItemsUseCase getMenuItemsUseCase;

    @PostMapping("/validate-menu-items")
    public ResponseEntity<List<ItemValidationResponse>> validateMenuItems(
            @RequestBody ItemValidationRequest request) {
        
        List<ItemValidationResponse> response = validationService.validateItems(request);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/menuItems")
    public ResponseEntity<List<MenuItemResponse>> getMenuItems(
        @RequestParam("ids") List<Long> ids
    ) {
        List<MenuItemResponse> response = getMenuItemsUseCase.getAllMenuItemsByIds(ids);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public String test() {
        return "Restaurant Service is up and running!";
    }
}
package com.example.demo.adapters.out.external.api;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.common_dtos.dto.MenuItemResponse;
import com.example.demo.adapters.out.config.FeignClientConfig;



@FeignClient(
    name = "restaurant-service", 
    // url = "http://restaurant-service:8081", 
    configuration = FeignClientConfig.class
)
public interface RestaurantServiceApi {

 
    @PostMapping("/api/v1/restaurants/validate-menu-items")
    List<ItemValidationResponse> validateMenuItems(
        @RequestBody ItemValidationRequest request
    );

    @GetMapping("/api/v1/restaurants/menuItems")
    List<MenuItemResponse> getMenuItems(
        @RequestParam("ids") List<Long> ids
    );
}
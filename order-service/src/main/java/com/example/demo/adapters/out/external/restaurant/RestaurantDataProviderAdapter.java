package com.example.demo.adapters.out.external.restaurant;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.common_dtos.dto.MenuItemResponse;

import com.example.demo.adapters.out.external.api.RestaurantServiceApi;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.domain.entity.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantDataProviderAdapter implements RestaurantDataProviderPort {

    private final RestaurantServiceApi restaurantServiceApi;

    @Override
    public List<ItemValidationResponse> validateOrderCreation(Long restaurantId, List<Long> productIds) {
        ItemValidationRequest request = new ItemValidationRequest();
        request.setRestaurantId(restaurantId);
        request.setMenuItemIds(productIds);

        try {
            List<ItemValidationResponse> validationResponses = restaurantServiceApi.validateMenuItems(request);
            return validationResponses.stream()
                    .map(r -> {
                        if (!r.isValid()) {
                            throw new Order.OrderDomainException(
                                    "Product ID " + r.getMenuItemId() + " validation failed.");
                        }
                        // map nguyên trạng
                        return new ItemValidationResponse(
                                r.getMenuItemId(), r.isValid(), r.getPrice(), r.getReason()
                        );
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new Order.OrderDomainException("Failed to validate items with Restaurant Service: " + e.getMessage());
        }
    }

    // ✅ NEW: phục vụ quoteTotal()
    @Override
    public List<ProductDetailData> getProducts(Long restaurantId, List<Long> productIds) {
        try {
            // Nếu API của bạn có endpoint có restaurantId thì dùng; ở đây demo dùng theo chữ ký sẵn có
            List<MenuItemResponse> responses = restaurantServiceApi.getMenuItems(productIds);
            return responses.stream()
                    .map(res -> new ProductDetailData(
                            res.getId(),
                            res.getName(),
                            res.getPrice()      // ✅ lấy giá
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new Order.OrderDomainException(
                    "Failed to fetch product details (with prices) from Restaurant Service: " + e.getMessage());
        }
    }

    @Override
    public Map<Long, ProductDetailData> getProductDetailsByIds(List<Long> productIds) {
        try {
            List<MenuItemResponse> menuItemResponses = restaurantServiceApi.getMenuItems(productIds);
            return menuItemResponses.stream()
                    .collect(Collectors.toMap(
                            MenuItemResponse::getId,
                            res -> new ProductDetailData(
                                    res.getId(),
                                    res.getName(),
                                    res.getPrice() // ✅ đừng quên set price ở map cũ
                            )
                    ));
        } catch (Exception e) {
            throw new Order.OrderDomainException(
                    "Failed to fetch product details from Restaurant Service: " + e.getMessage());
        }
    }
}

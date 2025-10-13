package com.example.demo.application.ports.output.external; 

import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.dto.output.ProductDetailData;
import java.util.List;
import java.util.Map;

public interface RestaurantDataProviderPort {
    
    List<ItemValidationResponse> validateOrderCreation(Long restaurantId, List<Long> productIds);
    Map<Long, ProductDetailData> getProductDetailsByIds(List<Long> productIds);
}
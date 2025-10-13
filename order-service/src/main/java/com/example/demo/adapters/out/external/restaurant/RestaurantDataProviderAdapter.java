// package com.example.demo.adapters.out.external.restaurant;

// import java.math.BigDecimal;
// import java.util.List;
// import java.util.Map;
// import java.util.stream.Collectors;

// import org.springframework.stereotype.Service;
// import static java.util.function.Function.identity;
// import com.example.demo.application.dto.output.ProductDetailData;
// import com.example.demo.application.dto.output.VerifiedProductData;
// import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
// import com.example.demo.domain.entity.Order;
// import com.example.demo.domain.entity.OrderItem;
// import com.example.demo.domain.valueobject.product.ProductId;

// @Service
// public class RestaurantDataProviderAdapter implements RestaurantDataProviderPort {

//     private static final Map<Long, Map<String, Object>> MOCK_PRODUCT_DATA = Map.of(
//             1L, Map.of("name", "Product 1", "price", new BigDecimal("100000")),
//             2L, Map.of("name", "Product 2", "price", new BigDecimal("200000")));

//     private static final Long VALID_RESTAURANT_ID = 123L;

//     @Override
//     public List<VerifiedProductData> validateOrderCreation(Long restaurantId, List<Long> productIds) {

//         if (!restaurantId.equals(VALID_RESTAURANT_ID)) {
//             throw new Order.OrderDomainException("Invalid or closed Restaurant ID: " + restaurantId);
//         }

//         return productIds.stream().map(id -> {
//             Map<String, Object> productDetails = MOCK_PRODUCT_DATA.get(id);

//             if (productDetails == null) {
//                 throw new Order.OrderDomainException("Product ID " + id + " not found in menu.");
//             }
//             // 3. Lấy Price chính thức
//             BigDecimal price = (BigDecimal) productDetails.get("price");
//             // 4. TRẢ VỀ DTO HỢP LỆ (CHỈ CẦN ID và PRICE)
//             return new VerifiedProductData(id, price);

//         }).collect(Collectors.toList());
//     }

//     @Override
//     public Map<Long, ProductDetailData> getProductDetailsByIds(List<Long> productIds) {
//         return productIds.stream()
//                 .filter(MOCK_PRODUCT_DATA::containsKey) // Chỉ xử lý ID có trong Mock
//                 .map(id -> {
//                     Map<String, Object> productDetails = MOCK_PRODUCT_DATA.get(id);
//                     String name = (String) productDetails.get("name");
//                     return new ProductDetailData(id, name);
//                 })
//                 .collect(Collectors.toMap(
//                     // SỬA LỖI: Ép kiểu tường minh kết quả trả về thành Long
//                     data -> (Long) data.getId(), 
//                     identity() 
//             ));
//     }

// }

package com.example.demo.adapters.out.external.restaurant;

import java.math.BigDecimal;
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
@RequiredArgsConstructor // Thêm constructor injection cho Feign Client
public class RestaurantDataProviderAdapter implements RestaurantDataProviderPort {

    // 1. INJECT FEIGN CLIENT
    private final RestaurantServiceApi restaurantServiceApi;

    @Override
    public List<ItemValidationResponse> validateOrderCreation(Long restaurantId, List<Long> productIds) {

        ItemValidationRequest request = new ItemValidationRequest();
        request.setRestaurantId(restaurantId);
        request.setMenuItemIds(productIds);


        try {
            List<ItemValidationResponse> validationResponses = restaurantServiceApi.validateMenuItems(request);

            return validationResponses.stream()
                    .map(response -> {
                        if (!response.isValid()) {
                            // Nếu item không hợp lệ, ném 
                            throw new Order.OrderDomainException(
                                    "Product ID " + response.getMenuItemId() + " validation failed.");
                        }
                        return new ItemValidationResponse(response.getMenuItemId(), response.isValid(), response.getPrice(), response.getReason());
                    })
                    .collect(Collectors.toList());

        } catch (Exception e) {

            throw new Order.OrderDomainException("Failed to validate items with Restaurant Service: " + e.getMessage());
        }
    }

    @Override
    public Map<Long, ProductDetailData> getProductDetailsByIds(List<Long> productIds) {

        try {
            List<MenuItemResponse> menuItemResponses = restaurantServiceApi.getMenuItems(productIds);

            return menuItemResponses.stream()
                    .collect(Collectors.toMap(
                            MenuItemResponse::getId, 
                            response -> new ProductDetailData(response.getId(), response.getName()) 
                    ));

        } catch (Exception e) {
            throw new Order.OrderDomainException(
                    "Failed to fetch product details from Restaurant Service: " + e.getMessage());
        }
    }
}
package com.example.demo.application.usecases;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.adapters.out.external.restaurant.RestaurantDataProviderAdapter;
import com.example.demo.application.dto.output.ProductDetailData;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.dto.query.GetOrdersByCustomerQuery;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.ports.input.GetOrderHistoryUseCase;
import com.example.demo.application.ports.output.external.RestaurantDataProviderPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetOrderHistoryUseCaseImpl implements GetOrderHistoryUseCase {
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderMapper orderMapper;
    private final RestaurantDataProviderPort restaurantDataProviderPort;

    public List<TrackOrderResponse> getOrdersByCustomer(Long id) {
        UserId userId = new UserId(id);
        List<Order> orders = orderRepositoryPort.findByUserId(userId);
        List<Long> allProductIds = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .map(item -> item.getProductId().value())
                .distinct() // Chỉ lấy ID duy nhất
                .collect(Collectors.toList());

        Map<Long, ProductDetailData> productDetailsMap = restaurantDataProviderPort
                .getProductDetailsByIds(allProductIds);
        return orders.stream()
                // Truyền Map chi tiết sản phẩm vào Mapper để ánh xạ
                .map(order -> orderMapper.toOrderDTO(order))
                .collect(Collectors.toList());
    }
}

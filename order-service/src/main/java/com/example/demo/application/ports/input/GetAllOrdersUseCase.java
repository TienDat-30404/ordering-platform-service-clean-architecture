package com.example.demo.application.ports.input;

import java.util.List;

import com.example.demo.application.dto.output.TrackOrderResponse;

public interface GetAllOrdersUseCase {
    // CreateOrderResponse createOrder(CreateOrderCommand request);

    List<TrackOrderResponse> getAllOrders();
}

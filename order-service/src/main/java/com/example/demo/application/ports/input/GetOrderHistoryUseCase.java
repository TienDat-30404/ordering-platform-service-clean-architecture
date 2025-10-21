package com.example.demo.application.ports.input;

import java.util.List;

import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.dto.query.GetOrdersByCustomerQuery;

public interface GetOrderHistoryUseCase {
    List<TrackOrderResponse> getOrdersByCustomer(Long id);
}
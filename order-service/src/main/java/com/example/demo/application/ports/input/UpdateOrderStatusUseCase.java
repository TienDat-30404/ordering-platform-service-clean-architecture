package com.example.demo.application.ports.input;

import com.example.common_dtos.enums.OrderStatus;

public interface UpdateOrderStatusUseCase {
    void setStatus(String orderId, OrderStatus status);
}

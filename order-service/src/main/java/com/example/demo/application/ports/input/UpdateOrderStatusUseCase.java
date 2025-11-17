package com.example.demo.application.ports.input;

import com.example.demo.domain.valueobject.order.OrderStatus;

public interface UpdateOrderStatusUseCase {
    void setStatus(String orderId, OrderStatus status);
}

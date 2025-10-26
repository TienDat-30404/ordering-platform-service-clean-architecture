package com.example.demo.application.ports.input;

import com.example.demo.domain.entity.Order;

public interface CanceledOrderUseCase {
    void canceled(Order order);
}

package com.example.demo.application.ports.input;

import com.example.demo.application.dto.command.RateOrderCommand;

public interface RateOrderUseCase {
    void rateOrder(RateOrderCommand command);
}
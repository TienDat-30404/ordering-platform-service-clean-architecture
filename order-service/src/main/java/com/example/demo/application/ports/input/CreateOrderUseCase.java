package com.example.demo.application.ports.input;

import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;

public interface CreateOrderUseCase {
    TrackOrderResponse createOrder(CreateOrderCommand command);
}

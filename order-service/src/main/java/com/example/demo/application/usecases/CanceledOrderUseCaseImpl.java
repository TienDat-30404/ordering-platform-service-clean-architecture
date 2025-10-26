package com.example.demo.application.usecases;

import org.springframework.stereotype.Service;

import com.example.demo.application.ports.input.CanceledOrderUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CanceledOrderUseCaseImpl implements CanceledOrderUseCase {
     private final OrderRepositoryPort orderRepositoryPort;

    public void canceled(Order order) {
        order.canceled();
        orderRepositoryPort.save(order);
    }

}

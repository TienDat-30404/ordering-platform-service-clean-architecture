package com.example.demo.application.usecases;

import org.springframework.stereotype.Service;

import com.example.demo.application.ports.input.ConfirmOrderPaidUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ConfirmOrderPaidUseCaseImpl implements ConfirmOrderPaidUseCase {
    private final OrderRepositoryPort orderRepositoryPort;

    public void confirm(Order order) {
        order.confirmPaid();
        orderRepositoryPort.save(order);
    }
}

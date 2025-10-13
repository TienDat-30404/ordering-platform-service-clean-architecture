package com.example.demo.application.usecases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.ports.input.GetAllOrdersUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAllOrdersUseCaseImpl implements GetAllOrdersUseCase {
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderMapper orderMapper;

    public List<TrackOrderResponse> getAllOrders() {
        List<Order> orders = orderRepositoryPort.findAll();
        return orderMapper.toOrderDTOList(orders);
    }

    // public CreateOrderResponse createOrder(CreateOrderCommand request) {

    // }

}

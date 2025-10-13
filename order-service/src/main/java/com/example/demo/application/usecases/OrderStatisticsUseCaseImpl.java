package com.example.demo.application.usecases;

import org.springframework.stereotype.Service;

import com.example.demo.application.dto.output.OrderStatisticsResponse;
import com.example.demo.application.ports.input.OrderStatisticsUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.valueobject.order.OrderStatistics;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderStatisticsUseCaseImpl implements OrderStatisticsUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public OrderStatisticsResponse getStatistics() {
        // 1. Gọi Output Port để lấy Domain VO
        OrderStatistics stats = orderRepositoryPort.getStatistics(); 

        // 2. Ánh xạ từ Domain VO sang Application DTO
        return OrderStatisticsResponse.builder()
            .totalOrders(stats.getTotalOrders())
            .totalRevenue(stats.getTotalRevenue())
            .averageOrderValue(stats.getAverageOrderValue())
            .build();
    }
}
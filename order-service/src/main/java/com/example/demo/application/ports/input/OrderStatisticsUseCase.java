package com.example.demo.application.ports.input;

import com.example.demo.application.dto.output.OrderStatisticsResponse;

public interface OrderStatisticsUseCase {
    OrderStatisticsResponse getStatistics();
}
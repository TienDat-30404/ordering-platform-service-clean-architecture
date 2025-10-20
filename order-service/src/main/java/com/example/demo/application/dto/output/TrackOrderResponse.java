package com.example.demo.application.dto.output;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class TrackOrderResponse {
    private Long id;
    private Long userId;
    private Long restaurantId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
    List<OrderItemResponse> items;
}

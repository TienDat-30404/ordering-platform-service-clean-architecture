package com.example.demo.application.dto.output;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
}

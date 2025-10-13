package com.example.demo.application.dto.output;

import lombok.Data;

@Data
public class CreateOrderResponse {
    private Long id;
    private Long userId;
    private Double amount;

}

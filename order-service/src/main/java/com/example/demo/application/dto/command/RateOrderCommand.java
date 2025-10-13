package com.example.demo.application.dto.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateOrderCommand {
    private Long orderId;
    private Long customerId;
    private Integer score; // 1-5
    private String comment;
}
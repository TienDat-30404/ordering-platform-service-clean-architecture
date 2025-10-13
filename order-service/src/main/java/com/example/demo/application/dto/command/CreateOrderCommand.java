package com.example.demo.application.dto.command;

import java.util.List;

import lombok.Data;

@Data
public class CreateOrderCommand {
    private Long userId;
    private List<CreateOrderItemCommand> items;
    private Long restaurantId;
}

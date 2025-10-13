package com.example.demo.application.dto.command;

import java.util.List;


import lombok.Data;

@Data
public class AddItemsCommand {
    private Long orderId;
    private Long userId;
    private List<CreateOrderItemCommand> newItems;
}


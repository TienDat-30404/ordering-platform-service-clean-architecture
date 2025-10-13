package com.example.demo.application.dto.command;



import lombok.Data;

@Data
public class CreateOrderItemCommand {
    private Long productId;
    private int quantity;
}

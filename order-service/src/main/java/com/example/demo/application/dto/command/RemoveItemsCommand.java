package com.example.demo.application.dto.command;

import java.util.List;

import lombok.Data;

@Data
public class RemoveItemsCommand {
    private Long orderId; 
    private Long userId; 
    private List<Long> productIdsToRemove;
}

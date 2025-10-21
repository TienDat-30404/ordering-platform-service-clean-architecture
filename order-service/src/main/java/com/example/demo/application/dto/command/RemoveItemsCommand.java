package com.example.demo.application.dto.command;

import java.util.List;

import lombok.Data;

@Data
public class RemoveItemsCommand {
    private List<Long> productIdsToRemove;
}

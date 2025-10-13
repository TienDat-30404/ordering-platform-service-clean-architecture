package com.example.common_dtos.dto;


import java.util.List;

import lombok.Data;

@Data
public class ItemValidationRequest {
    private Long restaurantId;
    private List<Long> menuItemIds;

}   
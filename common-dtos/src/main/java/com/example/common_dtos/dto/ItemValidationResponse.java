package com.example.common_dtos.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemValidationResponse {
    private Long menuItemId;
    private boolean valid;
    private BigDecimal price;
    private String reason;

}
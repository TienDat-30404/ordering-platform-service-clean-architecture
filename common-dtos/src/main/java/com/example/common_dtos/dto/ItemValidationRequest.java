package com.example.common_dtos.dto;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor   // tạo constructor có tham số
@NoArgsConstructor    // giữ constructor mặc định
public class ItemValidationRequest {
    private Long restaurantId;
    private List<Long> menuItemIds;

}   
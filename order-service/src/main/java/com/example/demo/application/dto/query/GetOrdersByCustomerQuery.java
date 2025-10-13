package com.example.demo.application.dto.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetOrdersByCustomerQuery {
    private Long userId;
}

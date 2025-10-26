package com.example.demo.application.dto.saga;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;

@Data
@Builder
public class AuthorizePaymentCommandData implements Serializable {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    // BỎ restaurantId khỏi DTO này
}
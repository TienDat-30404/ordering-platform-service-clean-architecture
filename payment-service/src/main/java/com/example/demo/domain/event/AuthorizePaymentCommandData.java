package com.example.demo.domain.event;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AuthorizePaymentCommandData {
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
}
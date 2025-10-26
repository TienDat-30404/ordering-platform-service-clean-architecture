package com.example.demo.application.dto.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCommand implements Serializable {
    private String commandId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String commandType;
}
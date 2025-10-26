package com.example.demo.application.dto.command;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO để deserialize message từ Kafka topic "payment.authorize.command"
 * Nhận từ Order Service khi đơn hàng được tạo
 * 
 * Message từ Order Service:
 * {
 *   "orderId": 123,
 *   "userId": 456,
 *   "amount": 1000000.00
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorizePaymentCommandData {
    
    @JsonProperty("orderId")
    private Long orderId;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
}

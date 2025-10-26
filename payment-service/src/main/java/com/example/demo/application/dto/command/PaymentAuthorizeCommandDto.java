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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAuthorizeCommandDto {

    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("restaurantId")
    private String restaurantId;

    @JsonProperty("payload")
    private PaymentPayload payload;

    @JsonProperty("timestamp")
    private String timestamp;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentPayload {
        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("userId")
        private Long userId;
    }
}

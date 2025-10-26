package com.example.demo.domain.event;

public record PaymentResponseData(
    Long paymentId,
    Long orderId,
    String status, 
    String transactionId,
    String reason 
) {}
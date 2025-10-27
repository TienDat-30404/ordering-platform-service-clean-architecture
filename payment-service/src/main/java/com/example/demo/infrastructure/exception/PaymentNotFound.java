package com.example.demo.infrastructure.exception;

public class PaymentNotFound extends PaymentException {
   public PaymentNotFound(String paymentId) {
       super("Payment not found: " + paymentId);
   }
}
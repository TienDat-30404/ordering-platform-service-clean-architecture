package com.example.demo.infrastructure.exception;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

   @ExceptionHandler(PaymentNotFound.class)
   public ResponseEntity<Map<String, Object>> handlePaymentNotFound(PaymentNotFound ex) {
       log.error("Payment not found: {}", ex.getMessage());
       Map<String, Object> response = new HashMap<>();
       response.put("timestamp", LocalDateTime.now());
       response.put("status", HttpStatus.NOT_FOUND.value());
       response.put("error", "Payment Not Found");
       response.put("message", ex.getMessage());
       return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
   }

   @ExceptionHandler(PaymentException.class)
   public ResponseEntity<Map<String, Object>> handlePaymentException(PaymentException ex) {
       log.error("Payment exception: {}", ex.getMessage());
       Map<String, Object> response = new HashMap<>();
       response.put("timestamp", LocalDateTime.now());
       response.put("status", HttpStatus.BAD_REQUEST.value());
       response.put("error", "Payment Error");
       response.put("message", ex.getMessage());
       return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
   }

   @ExceptionHandler(Exception.class)
   public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
       log.error("Unexpected error: {}", ex.getMessage(), ex);
       Map<String, Object> response = new HashMap<>();
       response.put("timestamp", LocalDateTime.now());
       response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
       response.put("error", "Internal Server Error");
       response.put("message", "An unexpected error occurred");
       return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
   }
}

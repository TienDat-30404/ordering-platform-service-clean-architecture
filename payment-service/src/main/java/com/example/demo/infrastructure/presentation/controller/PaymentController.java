package com.example.demo.infrastructure.presentation.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.application.dto.command.AuthorizePaymentCommand;
import com.example.demo.application.dto.command.RefundPaymentCommand;
import com.example.demo.application.usecases.PaymentApplicationService;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentApplicationService paymentApplicationService;

    @PostMapping("/authorize")
    public ResponseEntity<String> authorizePayment(
            @RequestParam Long orderId,
            @RequestParam Long userId,
            @RequestParam BigDecimal amount,
            @RequestParam Long paymentId) {
        try {
            AuthorizePaymentCommand command = new AuthorizePaymentCommand(orderId, userId, amount, paymentId);
            paymentApplicationService.authorizePayment(command);
            return ResponseEntity.ok("Payment authorized successfully");
        } catch (Exception e) {
            log.error("Error authorizing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment authorization failed: " + e.getMessage());
        }
    }

    @PostMapping("/refund")
    public ResponseEntity<String> refundPayment(
            @RequestParam Long orderId,
            @RequestParam Long paymentId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {
        try {
            RefundPaymentCommand command = new RefundPaymentCommand(orderId, paymentId, amount, reason);
            paymentApplicationService.refundPayment(command);
            return ResponseEntity.ok("Payment refunded successfully");
        } catch (Exception e) {
            log.error("Error refunding payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Payment refund failed: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is running 11111");
    }
}

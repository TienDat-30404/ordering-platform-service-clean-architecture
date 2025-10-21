package com.example.demo.payment.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class PaymentService {

    public static final BigDecimal LIMIT = new BigDecimal("1000000"); // > 1,000,000 thì fail demo

    public Result authorize(String orderId, BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        boolean ok = amount.compareTo(LIMIT) < 0;
        if (ok) {
            return new Result(true, UUID.randomUUID().toString(), null);
        } else {
            return new Result(false, null, "amount_exceeds_limit");
        }
    }

    public record Result(boolean approved, String txId, String reason) {}
}
package com.example.demo.domain.valueobject.order;

import java.math.BigDecimal;
import java.util.Objects;

public class Voucher {
    private final String code;
    private final BigDecimal discountAmount; 
    private final boolean isPercentage; 

    public Voucher(String code, BigDecimal discountAmount, boolean isPercentage) {
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount amount must be positive.");
        }
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.discountAmount = discountAmount;
        this.isPercentage = isPercentage;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public boolean isPercentage() {
        return isPercentage;
    }

    @Override
    public String toString() {
        return "Voucher{" +
                "code='" + code + '\'' +
                ", discountAmount=" + discountAmount +
                ", isPercentage=" + isPercentage +
                '}';
    }

 
}
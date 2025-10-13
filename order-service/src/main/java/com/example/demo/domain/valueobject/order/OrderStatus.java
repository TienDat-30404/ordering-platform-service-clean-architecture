package com.example.demo.domain.valueobject.order;

public enum OrderStatus {
    
    // Các trạng thái mặc định của đơn hàng
    PENDING("PENDING"),      
    APPROVED("APPROVED");

    private final String displayValue;

    OrderStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getName() {
        return displayValue;
    }
}
package com.example.demo.domain.valueobject.order;

import java.math.BigDecimal;


public class OrderStatistics {
    private final Long totalOrders;
    private final BigDecimal totalRevenue;
    private final BigDecimal averageOrderValue;

    public OrderStatistics(Long totalOrders, BigDecimal totalRevenue, BigDecimal averageOrderValue) {
        this.totalOrders = totalOrders;
        this.totalRevenue = totalRevenue;
        this.averageOrderValue = averageOrderValue;
    }

    public Long getTotalOrders() {
        return totalOrders;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getAverageOrderValue() {
        return averageOrderValue;
    }
}

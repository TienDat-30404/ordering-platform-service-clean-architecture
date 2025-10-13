package com.example.demo.adapters.out.persistence.dto;

import java.math.BigDecimal;

// Projection Interface để hứng kết quả truy vấn COUNT/SUM/AVG
public interface OrderStatisticsJpaProjection {
    Long getTotalOrders();
    BigDecimal getTotalRevenue();
    BigDecimal getAverageOrderValue();
}
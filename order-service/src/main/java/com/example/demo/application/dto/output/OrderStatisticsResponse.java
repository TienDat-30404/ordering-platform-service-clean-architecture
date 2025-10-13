package com.example.demo.application.dto.output;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class OrderStatisticsResponse {
    private Long totalOrders; // Tổng số đơn hàng
    private BigDecimal totalRevenue; // Tổng doanh thu
    private BigDecimal averageOrderValue; // Giá trị đơn hàng trung bình
}
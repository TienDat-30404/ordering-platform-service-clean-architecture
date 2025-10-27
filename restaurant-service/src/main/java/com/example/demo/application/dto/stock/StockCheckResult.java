package com.example.demo.application.dto.stock;

public record StockCheckResult(Long productId, boolean sufficient, int available, String reason) {}
package com.example.demo.application.ports.input;

import java.util.List;
import com.example.demo.application.dto.stock.StockCheckItem;
import com.example.demo.application.dto.stock.StockCheckResult;

public interface CheckStockUseCase {
    List<StockCheckResult> check(List<StockCheckItem> items);
}
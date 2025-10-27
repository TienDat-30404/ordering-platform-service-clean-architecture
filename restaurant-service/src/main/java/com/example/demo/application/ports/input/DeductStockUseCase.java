package com.example.demo.application.ports.input;

import java.util.List;
import com.example.demo.application.dto.stock.StockCheckItem;

public interface DeductStockUseCase {
    void deduct(List<StockCheckItem> items);
}
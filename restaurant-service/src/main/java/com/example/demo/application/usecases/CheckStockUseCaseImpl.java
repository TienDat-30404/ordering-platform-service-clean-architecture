package com.example.demo.application.usecases;

import java.util.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.example.demo.adapters.out.persistence.repository.MenuItemJpaRepository;
import com.example.demo.application.dto.stock.StockCheckItem;
import com.example.demo.application.dto.stock.StockCheckResult;
import com.example.demo.application.ports.input.CheckStockUseCase;

@Service
@RequiredArgsConstructor
public class CheckStockUseCaseImpl implements CheckStockUseCase {

    private final MenuItemJpaRepository menuRepo;

    @Override
    public List<StockCheckResult> check(List<StockCheckItem> items) {
        if (items == null || items.isEmpty()) return List.of();

        List<Long> ids = items.stream().map(StockCheckItem::productId).toList();
        var dbItems = menuRepo.findByIdIn(ids);
        Map<Long, Integer> availableMap = new HashMap<>();
        dbItems.forEach(mi -> availableMap.put(mi.getId(), Optional.ofNullable(mi.getQuantity()).orElse(0)));

        List<StockCheckResult> results = new ArrayList<>();
        for (var it : items) {
            int available = availableMap.getOrDefault(it.productId(), 0);
            boolean ok = available >= it.requestedQty();
            results.add(new StockCheckResult(
                    it.productId(),
                    ok,
                    available,
                    ok ? "OK" : "NOT_ENOUGH_STOCK"
            ));
        }
        return results;
    }
}

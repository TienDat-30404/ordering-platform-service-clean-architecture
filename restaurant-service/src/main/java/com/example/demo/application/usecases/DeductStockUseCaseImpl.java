package com.example.demo.application.usecases;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

import com.example.demo.adapters.out.persistence.repository.MenuItemJpaRepository;
import com.example.demo.application.dto.stock.StockCheckItem;
import com.example.demo.application.ports.input.DeductStockUseCase;

@Service
@RequiredArgsConstructor
public class DeductStockUseCaseImpl implements DeductStockUseCase {

    private final MenuItemJpaRepository menuRepo;

    @Override
    @Transactional
    public void deduct(List<StockCheckItem> items) {
        if (items == null || items.isEmpty()) return;

        for (var it : items) {
            int updated = menuRepo.deductStock(it.productId(), it.requestedQty());
            if (updated == 0) {
                // Không đủ tồn hoặc id không tồn tại
                throw new IllegalStateException(
                        "Insufficient stock for productId=" + it.productId() + ", requested=" + it.requestedQty()
                );
            }
        }
    }
}

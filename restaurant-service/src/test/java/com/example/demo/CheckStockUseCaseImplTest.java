package com.example.demo;

import com.example.demo.adapters.out.persistence.entity.MenuItemJpaEntity;
import com.example.demo.adapters.out.persistence.repository.MenuItemJpaRepository;
import com.example.demo.application.dto.stock.StockCheckItem;
import com.example.demo.application.dto.stock.StockCheckResult;

import com.example.demo.application.usecases.CheckStockUseCaseImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CheckStockUseCaseImpl.
 *
 * Bao phủ toàn bộ nhánh logic:
 * - null/empty input
 * - đủ hàng
 * - thiếu hàng
 * - sản phẩm không có trong DB
 * - DB quantity = null
 */
@ExtendWith(MockitoExtension.class)
class CheckStockUseCaseImplTest {

    @Mock
    private MenuItemJpaRepository menuRepo;

    @InjectMocks
    private CheckStockUseCaseImpl useCase;

    private StockCheckItem item1;
    private StockCheckItem item2;

    @BeforeEach
    void setup() {
        item1 = new StockCheckItem(1L, 5);
        item2 = new StockCheckItem(2L, 10);
    }

    private MenuItemJpaEntity makeItem(Long id, Integer qty) {
        MenuItemJpaEntity e = new MenuItemJpaEntity();
        e.setId(id);
        e.setQuantity(qty);
        return e;
    }

    // ----------------------------- TEST 1 -----------------------------
    @Test
    void check_nullOrEmptyList_returnsEmpty() {
        assertTrue(useCase.check(null).isEmpty());
        assertTrue(useCase.check(Collections.emptyList()).isEmpty());
        verifyNoInteractions(menuRepo);
    }

    // ----------------------------- TEST 2 -----------------------------
    @Test
    void check_allItemsInStock_returnsOkResults() {
        // Arrange
        var dbItems = List.of(
                makeItem(1L, 10),
                makeItem(2L, 20)
        );
        when(menuRepo.findByIdIn(List.of(1L, 2L))).thenReturn(dbItems);

        // Act
        var results = useCase.check(List.of(item1, item2));

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(StockCheckResult::sufficient));
        assertEquals("OK", results.get(0).reason());
        verify(menuRepo).findByIdIn(List.of(1L, 2L));
    }

    // ----------------------------- TEST 3 -----------------------------
    @Test
    void check_someItemsOutOfStock_returnsNotEnough() {
        // Arrange
        var dbItems = List.of(
                makeItem(1L, 10),
                makeItem(2L, 5)
        );
        when(menuRepo.findByIdIn(List.of(1L, 2L))).thenReturn(dbItems);

        // Act
        var results = useCase.check(List.of(item1, item2));

        // Assert
        assertEquals(2, results.size());

        var r1 = results.get(0);
        var r2 = results.get(1);

        assertEquals(1L, r1.productId());
        assertTrue(r1.sufficient());
        assertEquals("OK", r1.reason());

        assertEquals(2L, r2.productId());
        assertFalse(r2.sufficient());
        assertEquals("NOT_ENOUGH_STOCK", r2.reason());
    }

    // ----------------------------- TEST 4 -----------------------------
    @Test
    void check_missingProductInDb_treatedAsZeroAvailable() {
        // Arrange
        var dbItems = List.of(
                makeItem(1L, 10)
        );
        when(menuRepo.findByIdIn(List.of(1L, 2L))).thenReturn(dbItems);

        // Act
        var results = useCase.check(List.of(item1, item2));

        // Assert
        assertEquals(2, results.size());
        var missing = results.stream()
                .filter(r -> r.productId() == 2L)
                .findFirst().orElseThrow();

        assertFalse(missing.sufficient());
        assertEquals(0, missing.available());
        assertEquals("NOT_ENOUGH_STOCK", missing.reason());
    }

    // ----------------------------- TEST 5 -----------------------------
    @Test
    void check_dbQuantityNull_treatedAsZero() {
        // Arrange
        var dbItems = List.of(
                makeItem(1L, null)
        );
        when(menuRepo.findByIdIn(List.of(1L))).thenReturn(dbItems);

        // Act
        var results = useCase.check(List.of(new StockCheckItem(1L, 2)));

        // Assert
        assertEquals(1, results.size());
        var r = results.get(0);
        assertFalse(r.sufficient());
        assertEquals(0, r.available());
        assertEquals("NOT_ENOUGH_STOCK", r.reason());
    }
}

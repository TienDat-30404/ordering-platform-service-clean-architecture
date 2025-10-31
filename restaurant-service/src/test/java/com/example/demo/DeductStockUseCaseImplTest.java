package com.example.demo;

import com.example.demo.adapters.out.persistence.repository.MenuItemJpaRepository;
import com.example.demo.application.dto.stock.StockCheckItem;
import com.example.demo.application.usecases.DeductStockUseCaseImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeductStockUseCaseImplTest {

    private MenuItemJpaRepository menuRepo;
    private DeductStockUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        menuRepo = mock(MenuItemJpaRepository.class);
        useCase = new DeductStockUseCaseImpl(menuRepo);
    }

    @Test
    void shouldDoNothingWhenItemsIsNull() {
        // Act
        useCase.deduct(null);

        // Assert
        verifyNoInteractions(menuRepo);
    }

    @Test
    void shouldDoNothingWhenItemsIsEmpty() {
        // Act
        useCase.deduct(List.of());

        // Assert
        verifyNoInteractions(menuRepo);
    }

    @Test
    void shouldDeductStockSuccessfully() {
        // Arrange
        var items = List.of(
                new StockCheckItem(1L, 2),
                new StockCheckItem(2L, 3)
        );

        when(menuRepo.deductStock(1L, 2)).thenReturn(1);
        when(menuRepo.deductStock(2L, 3)).thenReturn(1);

        // Act
        useCase.deduct(items);

        // Assert
        verify(menuRepo, times(1)).deductStock(1L, 2);
        verify(menuRepo, times(1)).deductStock(2L, 3);
        verifyNoMoreInteractions(menuRepo);
    }

    @Test
    void shouldThrowExceptionWhenStockNotEnough() {
        // Arrange
        var items = List.of(
                new StockCheckItem(10L, 5)
        );
        when(menuRepo.deductStock(10L, 5)).thenReturn(0); // không trừ được

        // Act + Assert
        var ex = assertThrows(IllegalStateException.class,
                () -> useCase.deduct(items));

        assertTrue(ex.getMessage().contains("Insufficient stock for productId=10"));
        verify(menuRepo, times(1)).deductStock(10L, 5);
    }

    @Test
    void shouldContinueTransactionUntilFailure() {
        // Arrange
        var items = List.of(
                new StockCheckItem(1L, 2),
                new StockCheckItem(2L, 3)
        );

        when(menuRepo.deductStock(1L, 2)).thenReturn(1); // thành công
        when(menuRepo.deductStock(2L, 3)).thenReturn(0); // thất bại

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> useCase.deduct(items));

        verify(menuRepo, times(1)).deductStock(1L, 2);
        verify(menuRepo, times(1)).deductStock(2L, 3);
    }
}

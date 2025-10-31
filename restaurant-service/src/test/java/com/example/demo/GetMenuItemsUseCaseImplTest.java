package com.example.demo;

import com.example.common_dtos.dto.MenuItemResponse;
import com.example.demo.application.mapper.MenuItemMapper;
import com.example.demo.application.ports.output.MenuItemRepositoryPort;
import com.example.demo.application.usecases.GetMenuItemsUseCaseImpl;
import com.example.demo.domain.entity.MenuItem;
import com.example.demo.domain.valueobject.MenuItemId;
import com.example.demo.domain.valueobject.RestaurantId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetMenuItemsUseCaseImplTest {

    private MenuItemRepositoryPort menuItemRepositoryPort;
    private MenuItemMapper mapper;
    private GetMenuItemsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        menuItemRepositoryPort = mock(MenuItemRepositoryPort.class);
        mapper = mock(MenuItemMapper.class);
        useCase = new GetMenuItemsUseCaseImpl(menuItemRepositoryPort, mapper);
    }

    @Test
    void shouldReturnMappedMenuItems() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);

        List<MenuItem> domainItems = List.of(
                new MenuItem(
                        new MenuItemId(1L),
                        new RestaurantId(10L),
                        "Pizza", "Cheese pizza",
                        BigDecimal.valueOf(10.5),
                        "Main",
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ),
                new MenuItem(
                        new MenuItemId(2L),
                        new RestaurantId(10L),
                        "Burger", "Beef burger",
                        BigDecimal.valueOf(8.0),
                        "Main",
                        true,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )
        );

        List<MenuItemResponse> dtoItems = List.of(
                new MenuItemResponse(1L, "Pizza", BigDecimal.valueOf(10.5)),
                new MenuItemResponse(2L, "Burger", BigDecimal.valueOf(8.0))
        );

        when(menuItemRepositoryPort.findAllById(ids)).thenReturn(domainItems);
        when(mapper.toListMenuItemDTO(domainItems)).thenReturn(dtoItems);

        // Act
        List<MenuItemResponse> result = useCase.getAllMenuItemsByIds(ids);

        // Assert
        assertEquals(2, result.size());
        assertEquals("Pizza", result.get(0).getName());
        assertEquals("Burger", result.get(1).getName());

        verify(menuItemRepositoryPort).findAllById(ids);
        verify(mapper).toListMenuItemDTO(domainItems);
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsEmpty() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        when(menuItemRepositoryPort.findAllById(ids)).thenReturn(List.of());
        when(mapper.toListMenuItemDTO(List.of())).thenReturn(List.of());

        // Act
        List<MenuItemResponse> result = useCase.getAllMenuItemsByIds(ids);

        // Assert
        assertTrue(result.isEmpty());
        verify(menuItemRepositoryPort).findAllById(ids);
        verify(mapper).toListMenuItemDTO(List.of());
    }

    @Test
    void shouldHandleNullRepositoryResultGracefully() {
        // Arrange
        List<Long> ids = List.of(99L);
        when(menuItemRepositoryPort.findAllById(ids)).thenReturn(null);
        when(mapper.toListMenuItemDTO(null)).thenReturn(List.of());

        // Act
        List<MenuItemResponse> result = useCase.getAllMenuItemsByIds(ids);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(menuItemRepositoryPort).findAllById(ids);
        verify(mapper).toListMenuItemDTO(null);
    }
}

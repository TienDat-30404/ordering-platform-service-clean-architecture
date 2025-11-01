package com.example.demo;

import com.example.common_dtos.dto.ItemValidationRequest;
import com.example.common_dtos.dto.ItemValidationResponse;
import com.example.demo.application.ports.output.RestaurantRepositoryPort;
import com.example.demo.application.usecases.ValidateMenuItemUseCaseImpl;
import com.example.demo.domain.entity.MenuItem;
import com.example.demo.domain.entity.Restaurant;
import com.example.demo.domain.valueobject.MenuItemId;
import com.example.demo.domain.valueobject.RestaurantId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValidateMenuItemUseCaseImplTest {

    private RestaurantRepositoryPort restaurantRepository;
    private ValidateMenuItemUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepositoryPort.class);
        useCase = new ValidateMenuItemUseCaseImpl(restaurantRepository);
    }

    @Test
    void shouldReturnErrorWhenRestaurantNotFound() {
        // Arrange
        Long restaurantId = 99L;
        List<Long> itemIds = List.of(1L, 2L);
        ItemValidationRequest request = new ItemValidationRequest(restaurantId, itemIds);

        when(restaurantRepository.findById(new RestaurantId(restaurantId))).thenReturn(Optional.empty());

        // Act
        List<ItemValidationResponse> responses = useCase.validateItems(request);

        // Assert
        assertEquals(2, responses.size());
        assertTrue(responses.stream().allMatch(r -> !r.isValid()));
        assertTrue(responses.get(0).getReason().contains("Restaurant not found"));

        verify(restaurantRepository).findById(new RestaurantId(restaurantId));
    }

    @Test
    void shouldReturnErrorWhenMenuItemNotInRestaurant() {
        // Arrange
        Long restaurantId = 1L;
        Restaurant restaurant = mock(Restaurant.class);

        // Restaurant chỉ có 1 món
        MenuItem existingItem = new MenuItem(
                new MenuItemId(10L),
                new RestaurantId(restaurantId),
                "Pizza",
                "Cheese pizza",
                BigDecimal.valueOf(10.5),
                "Main",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(restaurant.getMenu()).thenReturn(List.of(existingItem));
        when(restaurantRepository.findById(new RestaurantId(restaurantId))).thenReturn(Optional.of(restaurant));

        // Yêu cầu kiểm tra itemId = 10 (hợp lệ) và 99 (không tồn tại)
        ItemValidationRequest request = new ItemValidationRequest(restaurantId, List.of(10L, 99L));

        // Act
        List<ItemValidationResponse> responses = useCase.validateItems(request);

        // Assert
        assertEquals(2, responses.size());

        ItemValidationResponse valid = responses.stream()
                .filter(r -> r.getMenuItemId() == 10L).findFirst().orElseThrow();
        ItemValidationResponse invalid = responses.stream()
                .filter(r -> r.getMenuItemId() == 99L).findFirst().orElseThrow();

        assertTrue(valid.isValid());
        assertEquals("OK", valid.getReason());
        assertFalse(invalid.isValid());
        assertTrue(invalid.getReason().contains("not found"));

        verify(restaurantRepository).findById(new RestaurantId(restaurantId));
    }

    @Test
    void shouldReturnErrorWhenMenuItemUnavailable() {
        // Arrange
        Long restaurantId = 1L;
        Restaurant restaurant = mock(Restaurant.class);

        MenuItem unavailableItem = new MenuItem(
                new MenuItemId(5L),
                new RestaurantId(restaurantId),
                "Burger",
                "Out of stock burger",
                BigDecimal.valueOf(8.0),
                "Main",
                false, // unavailable
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(restaurant.getMenu()).thenReturn(List.of(unavailableItem));
        when(restaurantRepository.findById(new RestaurantId(restaurantId))).thenReturn(Optional.of(restaurant));

        ItemValidationRequest request = new ItemValidationRequest(restaurantId, List.of(5L));

        // Act
        List<ItemValidationResponse> responses = useCase.validateItems(request);

        // Assert
        assertEquals(1, responses.size());
        assertFalse(responses.get(0).isValid());
        assertTrue(responses.get(0).getReason().contains("not available"));

        verify(restaurantRepository).findById(new RestaurantId(restaurantId));
    }

    @Test
    void shouldReturnValidResponseWhenAllItemsOk() {
        // Arrange
        Long restaurantId = 1L;
        Restaurant restaurant = mock(Restaurant.class);

        MenuItem item1 = new MenuItem(
                new MenuItemId(1L),
                new RestaurantId(restaurantId),
                "Pizza",
                "Cheese pizza",
                BigDecimal.valueOf(12.0),
                "Main",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        MenuItem item2 = new MenuItem(
                new MenuItemId(2L),
                new RestaurantId(restaurantId),
                "Burger",
                "Beef burger",
                BigDecimal.valueOf(9.5),
                "Main",
                true,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(restaurant.getMenu()).thenReturn(List.of(item1, item2));
        when(restaurantRepository.findById(new RestaurantId(restaurantId))).thenReturn(Optional.of(restaurant));

        ItemValidationRequest request = new ItemValidationRequest(restaurantId, List.of(1L, 2L));

        // Act
        List<ItemValidationResponse> responses = useCase.validateItems(request);

        // Assert
        assertEquals(2, responses.size());
        assertTrue(responses.stream().allMatch(ItemValidationResponse::isValid));
        assertEquals("OK", responses.get(0).getReason());

        verify(restaurantRepository).findById(new RestaurantId(restaurantId));
    }
}

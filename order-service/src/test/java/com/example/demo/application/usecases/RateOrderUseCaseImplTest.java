package com.example.demo.application.usecases;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.example.demo.application.dto.command.RateOrderCommand;
import com.example.demo.application.ports.output.repository.OrderRatingRepositoryPort;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.entity.OrderRating;
import com.example.demo.domain.valueobject.user.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateOrderUseCaseImplTest {

  @Mock
  private OrderRepositoryPort orderRepositoryPort;

  @Mock
  private OrderRatingRepositoryPort ratingRepositoryPort;

  @Mock
  private RateOrderCommand command;

  @Mock
  private Order order;

  @InjectMocks
  private RateOrderUseCaseImpl useCase;

  @BeforeEach
  void setUp() {
    // InjectMocks will construct useCase using the mocked ports.
  }

  @Test
  void rateOrder_success_savesRatingAndUpdatesOrder() {
    // Arrange
    when(command.getOrderId()).thenReturn(1L);
    when(command.getScore()).thenReturn(5);
    when(command.getComment()).thenReturn("Good service");
    when(orderRepositoryPort.findById(any())).thenReturn(order);

    UserId userId = mock(UserId.class);

    // Act
    useCase.rateOrder(command, userId);

    // Assert
    verify(orderRepositoryPort).findById(any());
    verify(order).validateForRating();
    verify(ratingRepositoryPort).save(any(OrderRating.class));
    verify(order).setHasBeenRated(true);
    verify(orderRepositoryPort).save(order);
  }

  @Test
  void rateOrder_whenValidationFails_noSavesAndExceptionPropagated() {
    // Arrange
    when(command.getOrderId()).thenReturn(2L);
    when(orderRepositoryPort.findById(any())).thenReturn(order);
    doThrow(new IllegalStateException("invalid state")).when(order).validateForRating();

    UserId userId = mock(UserId.class);

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> useCase.rateOrder(command, userId));

    verify(order).validateForRating();
    verifyNoInteractions(ratingRepositoryPort);
    verify(orderRepositoryPort, never()).save(any());
  }
}
package com.example.demo.application.usecases;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.application.dto.command.RemoveItemsCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.application.mapper.OrderMapper;
import com.example.demo.application.ports.input.RemoveItemsUseCase;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.product.ProductId;
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RemoveItemsUseCaseImpl implements RemoveItemsUseCase {
    private final OrderRepositoryPort orderRepositoryPort;
    private final OrderMapper orderMapper;

    public TrackOrderResponse removeItems(RemoveItemsCommand command, UserId userId, OrderId orderId) {

        Order existingOrder = orderRepositoryPort.findByIdAndUserId(
            orderId,
            userId
        );
        if(existingOrder == null) {
            throw new Order.OrderDomainException("User does not have permission to modify this order.");
        }

        Order order = orderRepositoryPort.findById(orderId);

        List<ProductId> productIds = command.getProductIdsToRemove().stream()
                .map(ProductId::new)
                .toList();
        order.removeItems(productIds);
        Order savedOrder = orderRepositoryPort.save(order);
        return orderMapper.toOrderDTO(savedOrder);
    }

}

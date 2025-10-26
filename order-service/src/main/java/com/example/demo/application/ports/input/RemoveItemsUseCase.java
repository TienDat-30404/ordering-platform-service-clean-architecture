package com.example.demo.application.ports.input;


import com.example.demo.application.dto.command.RemoveItemsCommand;
import com.example.demo.application.dto.output.TrackOrderResponse;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.user.UserId;

public interface RemoveItemsUseCase {
    TrackOrderResponse removeItems(RemoveItemsCommand command,  UserId userId, OrderId orderId);
}

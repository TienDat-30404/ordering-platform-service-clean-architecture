package com.example.demo.application.ports.output.repository;

import java.util.List;

import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderStatistics;
import com.example.demo.domain.valueobject.user.UserId;


public interface OrderRepositoryPort {
    // public Order save(Order order);
    List<Order> findAll();
    Order save(Order order);
    Order findById(OrderId id);
    List<Order> findByUserId(UserId userId);
    OrderStatistics getStatistics();
}

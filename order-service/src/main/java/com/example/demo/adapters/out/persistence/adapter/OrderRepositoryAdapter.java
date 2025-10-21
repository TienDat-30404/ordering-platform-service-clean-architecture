package com.example.demo.adapters.out.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.example.demo.adapters.out.persistence.dto.OrderStatisticsJpaProjection;
import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;
import com.example.demo.adapters.out.persistence.mapper.OrderPersistenceMapper;
import com.example.demo.adapters.out.persistence.repository.OrderJpaRepository;
import com.example.demo.application.dto.command.CreateOrderCommand;
import com.example.demo.application.ports.output.repository.OrderRepositoryPort;
import com.example.demo.domain.entity.Order;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderStatistics;
import com.example.demo.domain.valueobject.user.UserId;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orderJpaRepository;
    private final OrderPersistenceMapper orderPersistenceMapper;

    @Override
    public List<Order> findAll() {
        return orderJpaRepository.findAll().stream()
                .map(orderPersistenceMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    public Order save(Order order) {
        OrderJpaEntity jpaEntity = orderPersistenceMapper.toJpaEntity(order);
        OrderJpaEntity savedJpaEntity = orderJpaRepository.save(jpaEntity);
        return orderPersistenceMapper.toDomainEntity(savedJpaEntity);

    }

    public Order findById(OrderId id) {
        Long orderId = id.value();
        return orderJpaRepository.findById(orderId)
                .map(orderPersistenceMapper::toDomainEntity)
                .orElseThrow(() -> new Order.OrderDomainException(
                        "Order with ID " + orderId + " not found."));
    }

    public List<Order> findByUserId(UserId userId) {
        Long rawUserId = userId.value();
        List<OrderJpaEntity> jpaEntities = orderJpaRepository.findByUserId(rawUserId);

        return jpaEntities.stream()
                .map(orderPersistenceMapper::toDomainEntity)
                .collect(Collectors.toList());
    }

    public OrderStatistics getStatistics() {
        OrderStatisticsJpaProjection projection = orderJpaRepository.getOrderStatistics();

        // Xử lý trường hợp không có đơn hàng nào
        if (projection == null || projection.getTotalOrders() == null) {
            return new OrderStatistics(
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO);

        }

        // Ánh xạ từ Persistence Projection sang Domain VO
        return new OrderStatistics(
                projection.getTotalOrders(),
                projection.getTotalRevenue(),
                projection.getAverageOrderValue());
    }

    public Order findByIdAndUserId(OrderId orderId, UserId userId) {
        OrderJpaEntity jpaEntity = orderJpaRepository.findByIdAndUserId(orderId.value(), userId.value());
        if (jpaEntity == null) {
            return null;
        }
        return orderPersistenceMapper.toDomainEntity(jpaEntity);
    }

}

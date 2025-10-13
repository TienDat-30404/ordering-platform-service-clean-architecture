package com.example.demo.adapters.out.persistence.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;
import com.example.demo.adapters.out.persistence.entity.OrderRatingJpaEntity;
import com.example.demo.domain.entity.OrderRating;
import com.example.demo.domain.valueobject.order.OrderId;
import com.example.demo.domain.valueobject.order.OrderRatingId;
import com.example.demo.domain.valueobject.order.RatingScore;
import com.example.demo.domain.valueobject.user.UserId;

@Component
public class OrderRatingPersistenceMapper {

    public OrderRatingJpaEntity toJpaEntity(OrderRating orderRating, OrderJpaEntity orderJpaEntity) {
        OrderRatingJpaEntity jpaEntity = new OrderRatingJpaEntity();

        if (orderRating.getId() != null) {
            jpaEntity.setId(orderRating.getId().value());
        }

        jpaEntity.setOrder(orderJpaEntity);
        jpaEntity.setCustomerId(orderRating.getCustomerId().value());
        jpaEntity.setScore(orderRating.getScore().getValue());
        jpaEntity.setComment(orderRating.getComment());
        jpaEntity.setCreatedAt(orderRating.getCreatedAt());

        return jpaEntity;
    }

    public OrderRating toDomainEntity(OrderRatingJpaEntity jpaEntity) {
        OrderRatingId id = new OrderRatingId(jpaEntity.getId());
        // OrderId orderId = new OrderId(jpaEntity.getOrderId());
        UserId userId = new UserId(jpaEntity.getCustomerId());
        return new OrderRating(
                id,
                // orderId,
                userId,
                new RatingScore(jpaEntity.getScore()),
                jpaEntity.getComment(),
                jpaEntity.getCreatedAt());
    }
}
package com.example.demo.adapters.out.persistence.adapter;

import org.springframework.stereotype.Repository;

import com.example.demo.adapters.out.persistence.entity.OrderJpaEntity;
import com.example.demo.adapters.out.persistence.entity.OrderRatingJpaEntity;
import com.example.demo.adapters.out.persistence.mapper.OrderRatingPersistenceMapper;
import com.example.demo.adapters.out.persistence.repository.OrderJpaRepository;
import com.example.demo.adapters.out.persistence.repository.OrderRatingJpaRepository;
import com.example.demo.application.ports.output.repository.OrderRatingRepositoryPort;
import com.example.demo.domain.entity.OrderRating;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class OrderRatingRepositoryAdapter implements OrderRatingRepositoryPort {
    private final OrderRatingPersistenceMapper mapper;
    private final OrderRatingJpaRepository orderRatingJpaRepository;
    private final OrderJpaRepository orderJpaRepository;

    public OrderRating save(OrderRating orderRating) {
        OrderJpaEntity orderJpaEntity = orderJpaRepository.getReferenceById(orderRating.getOrderId().value());
        OrderRatingJpaEntity orderRatingJpaEntity = mapper.toJpaEntity(orderRating, orderJpaEntity);
        OrderRatingJpaEntity savedOrderRating = orderRatingJpaRepository.save(orderRatingJpaEntity);
        return mapper.toDomainEntity(savedOrderRating);
    }

}


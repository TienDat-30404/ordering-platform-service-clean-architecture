package com.example.demo.application.ports.output.repository;

import com.example.demo.domain.entity.OrderRating;

public interface OrderRatingRepositoryPort {
    OrderRating save(OrderRating orderRating);
}
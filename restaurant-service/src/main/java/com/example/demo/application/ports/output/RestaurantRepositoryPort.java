package com.example.demo.application.ports.output;

import java.util.Optional;

import com.example.demo.domain.entity.Restaurant;
import com.example.demo.domain.valueobject.RestaurantId;

public interface RestaurantRepositoryPort {
    Optional<Restaurant> findById(RestaurantId id);
}

package com.example.demo.adapters.out.persistence.adapter;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.adapters.out.persistence.mapper.RestaurantPersistenceMapper;
import com.example.demo.adapters.out.persistence.repository.RestaurantJpaRepository;
import com.example.demo.application.ports.output.RestaurantRepositoryPort;
import com.example.demo.domain.entity.Restaurant;
import com.example.demo.domain.valueobject.RestaurantId;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantRepositoryAdapter implements RestaurantRepositoryPort {

    private final RestaurantJpaRepository repo;
    private final RestaurantPersistenceMapper mapper;

    @Override
    @Cacheable(value = "restaurant", key = "#id.value()")
    public Optional<Restaurant> findById(RestaurantId id) {
        System.out.println("------------------------------------- Cache hit ----------------------------------------");
        Long pk = id.value();                  // record -> value()
        return repo.findByIdWithMenu(pk)       // ✅ BẮT BUỘC dùng met hod này
                .map(mapper::toDomainRestaurant);
    }
}

package com.example.demo.adapters.out.persistence.adapter;

import java.util.List;
import java.util.stream.Collectors;

// import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import com.example.demo.adapters.out.persistence.mapper.MenuItemPersistenceMapper;
import com.example.demo.adapters.out.persistence.repository.MenuItemJpaRepository;
import com.example.demo.application.ports.output.MenuItemRepositoryPort;
import com.example.demo.domain.entity.MenuItem;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MenuItemRepositoryAdapter implements MenuItemRepositoryPort {
    private final MenuItemJpaRepository menuItemJpaRepository;
    private final MenuItemPersistenceMapper mapper;

    // @Cacheable(value = "courses")
    public List<MenuItem> findAllById(List<Long> menuItemIds) {
        return menuItemJpaRepository.findAllById(menuItemIds)
                .stream()
                .map(mapper::toDomainEntity)
                .collect(Collectors.toList());
    }
}
